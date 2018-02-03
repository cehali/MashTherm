package com.example.cezary.mashtherm;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class DeviceScanActivity extends Activity {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;

    private boolean mScanning;

    private static final int RQS_ENABLE_BLUETOOTH = 1;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    Button btnScan;
    ListView listViewLE;

    //List<HashMap<String, String>> listBluetoothDevice;
    List<String> listBluetoothDevice;
    //HashMap<String, String> map;
    String name;
    String address;
    String deviceName;
    String deviceAddress;
    String deviceTEMP;


    ListAdapter adapterLeScanResult;

    ArrayList<Integer> temps_list = new ArrayList<>();
    ArrayList<Integer> times_list = new ArrayList<>();
    String tempLimit;

    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        // Check if BLE is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this,
                    "BLUETOOTH_LE not supported in this device!",
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        getBluetoothAdapterAndLeScanner();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this,
                    "bluetoothManager.getAdapter()==null",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnScan = (Button)findViewById(R.id.scan);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(true);
            }
        });
        listViewLE = (ListView)findViewById(R.id.lelist);

        temps_list = (ArrayList<Integer>) getIntent().getSerializableExtra("temps_list");
        times_list = (ArrayList<Integer>) getIntent().getSerializableExtra("times_list");
        tempLimit = (String) getIntent().getSerializableExtra("tempLimit");
        /*map = new HashMap<String, String>();
        listBluetoothDevice = new ArrayList<HashMap<String, String>>();
        adapterLeScanResult = new ArrayAdapter<HashMap<String, String>>(
                this, android.R.layout.simple_list_item_1, listBluetoothDevice.toString().replace("{",""));*/

        listBluetoothDevice = new ArrayList<String>();
        adapterLeScanResult = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, listBluetoothDevice);

        listViewLE.setAdapter(adapterLeScanResult);
        listViewLE.setOnItemClickListener(scanResultOnItemClickListener);

        mHandler = new Handler();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @RequiresApi(api = Build.VERSION_CODES.M)
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Location access has not been granted.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }

    AdapterView.OnItemClickListener scanResultOnItemClickListener =
            new AdapterView.OnItemClickListener(){

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final String device = (String) parent.getItemAtPosition(position);
                    //final BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);

                    /*String msg = device.getName() + "\n"
                            + device.getBluetoothClass().toString() + "\n"
                            + getBTDevieType(device);*/

                    /*if (device.containsKey(name)){
                        deviceName = name;
                    }

                    if (device.containsValue(address)){
                        deviceAddress = address;
                    }*/

                    final String[] parts = device.split(": ");

                    new AlertDialog.Builder(DeviceScanActivity.this)
                            .setTitle(parts[0])
                            //.setMessage(msg)
                            /*.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })*/
                            .setNeutralButton("CONNECT", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final Intent intent = new Intent(DeviceScanActivity.this,
                                            ControlActivity.class);
                                    intent.putExtra(ControlActivity.EXTRAS_DEVICE_NAME,
                                            parts[0]);
                                    intent.putExtra(ControlActivity.EXTRAS_DEVICE_ADDRESS,
                                            parts[1]);
                                    intent.putExtra("temps_list", temps_list);
                                    intent.putExtra("times_list", times_list);
                                    intent.putExtra("tempLimit", tempLimit);

                                    if (mScanning) {
                                        mBluetoothLeScanner.stopScan(scanCallback);
                                        mScanning = false;
                                        btnScan.setEnabled(true);
                                    }
                                    startActivity(intent);
                                }
                            })
                            .show();
                }
            };

    private String getBTDevieType(BluetoothDevice d){
        String type = "";

        switch (d.getType()){
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                type = "DEVICE_TYPE_CLASSIC";
                break;
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                type = "DEVICE_TYPE_DUAL";
                break;
            case BluetoothDevice.DEVICE_TYPE_LE:
                type = "DEVICE_TYPE_LE";
                break;
            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                type = "DEVICE_TYPE_UNKNOWN";
                break;
            default:
                type = "unknown...";
        }

        return type;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, RQS_ENABLE_BLUETOOTH);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == RQS_ENABLE_BLUETOOTH && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        getBluetoothAdapterAndLeScanner();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this,
                    "bluetoothManager.getAdapter()==null",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void getBluetoothAdapterAndLeScanner(){
        // Get BluetoothAdapter and BluetoothLeScanner.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mScanning = false;
    }

    /*
    to call startScan (ScanCallback callback),
    Requires BLUETOOTH_ADMIN permission.
    Must hold ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission to get results.
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            //devices.clear();
            listViewLE.invalidateViews();

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeScanner.stopScan(scanCallback);
                    listViewLE.invalidateViews();

                    /*Toast.makeText(DeviceScanActivity.this,
                            "Scan timeout",
                            Toast.LENGTH_LONG).show();*/

                    mScanning = false;
                    btnScan.setEnabled(true);
                }
            }, SCAN_PERIOD);

            //scan specified devices only with ScanFilter
            ScanFilter scanFilter =
                    new ScanFilter.Builder()
                            .setServiceUuid(BluetoothLeService.ParcelUuid_ThermoNRF52832_Service)
                            .build();
            List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
            scanFilters.add(scanFilter);

            ScanSettings scanSettings =
                    new ScanSettings.Builder().build();

            mBluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback);

            mBluetoothLeScanner.startScan(scanCallback);
            mScanning = true;
            btnScan.setEnabled(false);
        } else {
            mBluetoothLeScanner.stopScan(scanCallback);
            mScanning = false;
            btnScan.setEnabled(true);
        }
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            addBluetoothDevice(result.getDevice());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for(ScanResult result : results){
                addBluetoothDevice(result.getDevice());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            /*Toast.makeText(DeviceScanActivity.this,
                    "onScanFailed: " + String.valueOf(errorCode),
                    Toast.LENGTH_LONG).show();*/
        }

        private void addBluetoothDevice(BluetoothDevice device){
            name = device.getName();
            address = device.getAddress();
            deviceTEMP = name + ": " + address;
            if(!listBluetoothDevice.contains(deviceTEMP)){
                //map.put( name);
                //map.put(name, address);
                listBluetoothDevice.add(deviceTEMP);
                listViewLE.invalidateViews();
            }
            /*if(!devices.contains(deviceTEMP)){
                devices.add(deviceTEMP);
                listViewLE.invalidateViews();
            }*/
        }
    };
}