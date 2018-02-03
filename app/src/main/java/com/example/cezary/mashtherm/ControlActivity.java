package com.example.cezary.mashtherm;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ControlActivity extends Activity {

    private final static String TAG = ControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceName;
    private String mDeviceAddress;

    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothLeService mBluetoothLeService;

    private ExpandableListView mGattServicesList;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    ArrayList<Integer> temps_list = new ArrayList<>();
    ArrayList<Integer> times_list = new ArrayList<>();

    TextView crrTemp;
    TextView crrStage;
    TextView timeAim;
    Button startMash;
    TextView timeLeft;
    TextView tempAim;
    Integer nrStage = 0;
    String tempLimit;
    int tempAim_int;
    int tempLimit_int;
    boolean tempReached;
    boolean showHigh;
    boolean showLow;
    CountDownTimer resultTimer;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    public void startMashing(ArrayList <Integer> times, final Integer nrStage) {
        startMash.setEnabled(false);
        Integer timesSize = times.size();
        if (nrStage == 0){
            changeStage(nrStage, temps_list, times_list);
        }
        if (nrStage < timesSize && nrStage != 0){
            timeLeft.setText("");
            tempReached = false;
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
            final AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
            builder.setTitle("STAGE FINISHED");
            builder.setMessage("Click OK to start next stage.");
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            changeStage(nrStage, temps_list, times_list);
            builder.show();
        }
        if (nrStage == timesSize){
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
            final AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
            builder.setTitle("MASHING COMPLETED");
            builder.setMessage("Click OK to start new mashing.");
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    Intent intent = new Intent(ControlActivity.this, DefineMashActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            });
            builder.show();
        }
    }

    public void notifing() {
        if (mGattCharacteristics != null) {
            final BluetoothGattCharacteristic characteristic =
                    mGattCharacteristics.get(0).get(0);
            final int charaProp = characteristic.getProperties();
            mBluetoothLeService.setCharacteristicNotification(
                    characteristic, false);
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                mNotifyCharacteristic = characteristic;
                mBluetoothLeService.setCharacteristicNotification(
                        characteristic, true);
            }
            startMashing(times_list, nrStage);
        }
    }

    public void changeStage(Integer nrStage, ArrayList <Integer> temps, ArrayList <Integer> times){
        crrStage.setText(Integer.toString(nrStage+1) + "/" + Integer.toString(times.size()));
        timeAim.setText(Integer.toString(times.get(nrStage)*60));
        tempAim.setText(Integer.toString(temps.get(nrStage)));
        String tempAim_s = tempAim.getText().toString();
        tempAim_int = Integer.valueOf(tempAim_s);
    }

    public void timer(final Integer time){
         CountDownTimer resultTimer = new CountDownTimer(time*1000*60, 1000) {
            public void onTick(long millisUntilFinished) {
                timeLeft.setText(Integer.toString((int)(millisUntilFinished / 1000)));
            }
            public void onFinish() {
                try {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                nrStage++;
                startMashing(times_list, nrStage);
            }
        }.start();

    }


    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                //updateConnectionState("GATT_CONNECTED");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                //updateConnectionState("GATT_DISCONNECTED");
                //clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA), times_list);
            }
        }
    };

    /*private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
    }*/

    private void updateConnectionState(final String st) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                crrTemp.setText(st);
            }
        });
    }

    private void displayData(String data, ArrayList <Integer> times) {
        if (data != null) {
            crrTemp.setText(data);
            data = data.replaceAll("\\D+", "");
            int data_dec = Integer.valueOf(data);
            int temp1 = tempAim_int + tempLimit_int;
            int temp2 = tempAim_int - tempLimit_int;
            if (!tempReached) {
                if (data_dec == tempAim_int) {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();
                    timer(times.get(nrStage));
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
                    builder.setTitle("TEMPERATURE AIM REACHED");
                    builder.setMessage("Now control temperature carefully.");
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    builder.show();
                    tempReached = true;
                }
            }
            if (tempReached) {
                if (showHigh) {
                    if (data_dec >= temp1) {
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                        r.play();
                        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
                        builder.setTitle("TEMPERATURE TOO HIGH");
                        builder.setMessage("Chill mash");
                        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        builder.show();
                        showHigh = false;
                    }
                }
                if (data_dec < temp1 && data_dec >= tempAim_int) {
                    showHigh = true;
                }
                if (data_dec > temp2 && data_dec <= tempAim_int) {
                    showLow = true;
                }
                if (showLow) {
                    if (data_dec <= temp2) {
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                        r.play();
                        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
                        builder.setTitle("TEMPERATURE TOO LOW");
                        builder.setMessage("Heat up mash");
                        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        builder.show();
                        showLow = false;
                    }
                }
            }
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {

        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = "Unknown Service";
        String unknownCharaString = "Unknown Characteristic";
        ArrayList<HashMap<String, String>> gattServiceData =
                new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            if (uuid.equals("00002012-0000-1000-8000-00805f9b34fb")) {
                currentServiceData.put(
                        LIST_NAME, lookup(uuid, unknownServiceString));
                currentServiceData.put(LIST_UUID, uuid);
                gattServiceData.add(currentServiceData);

                ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                        new ArrayList<HashMap<String, String>>();
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                ArrayList<BluetoothGattCharacteristic> charas =
                        new ArrayList<BluetoothGattCharacteristic>();

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    charas.add(gattCharacteristic);
                    HashMap<String, String> currentCharaData = new HashMap<String, String>();
                    uuid = gattCharacteristic.getUuid().toString();
                    currentCharaData.put(
                            LIST_NAME, lookup(uuid, unknownCharaString));
                    currentCharaData.put(LIST_UUID, uuid);
                    gattCharacteristicGroupData.add(currentCharaData);
                }
                mGattCharacteristics.add(charas);
                gattCharacteristicData.add(gattCharacteristicGroupData);
            }
        }

        /*SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);*/
    }

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    /*private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(0).get(0);
                        final int charaProp = characteristic.getProperties();
                        mBluetoothLeService.setCharacteristicNotification(
                                characteristic, false);
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
        };*/

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        //TextView textViewDeviceName = (TextView)findViewById(R.id.textDeviceName);
        //TextView textViewDeviceAddr = (TextView)findViewById(R.id.textDeviceAddress);

        //textViewDeviceName.setText(mDeviceName);
        //textViewDeviceAddr.setText(mDeviceAddress);

        //mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        //mGattServicesList.setOnChildClickListener(servicesListClickListner);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        temps_list = (ArrayList<Integer>) getIntent().getSerializableExtra("temps_list");
        times_list = (ArrayList<Integer>) getIntent().getSerializableExtra("times_list");
        tempLimit = (String) getIntent().getSerializableExtra("tempLimit");
        tempLimit_int = Integer.valueOf(tempLimit);

        crrTemp = (TextView)findViewById(R.id.textCurrentTemp);
        crrStage = (TextView)findViewById(R.id.textCurrentStage);
        timeAim = (TextView)findViewById(R.id.textTimeAim);
        startMash = (Button)findViewById(R.id.startMashing);
        timeLeft = (TextView)findViewById(R.id.textTimeLeft);
        tempAim = (TextView)findViewById(R.id.textTempAim);

        tempReached = false;
        showHigh = true;
        showLow = true;

        startMash.setOnClickListener(new View.OnClickListener(){
            public void onClick (View v) {
                notifing();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public void onBackPressed() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("DO YOU WANT TO CANCEL MASHING?");
        builder.setMessage("Click OK to cancel mashing, click CANCEL to continue mashing.");
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (resultTimer != null){
                    resultTimer.cancel();
                }
                finish();
            }
        });
        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                }
        );
        builder.show();
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private static HashMap<String, String> attributes = new HashMap();

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}