package com.example.cezary.mashtherm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.util.ArrayList;

import static android.view.View.VISIBLE;

public class DefineMashActivity extends Activity {

    Button addStage;
    Button submitMash;
    Button deleteStage;
    TableLayout table;
    TableLayout subtable;
    ArrayList<Integer> temps = new ArrayList<>();
    ArrayList<Integer> times = new ArrayList<>();
    EditText tempLimit;
    String temperatureLimit;
    boolean allgood;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_definemash);

        addStage = (Button)findViewById(R.id.button_addStage);
        submitMash = (Button)findViewById(R.id.button_submitMash);
        deleteStage = (Button)findViewById(R.id.button_deleteStage);
        subtable = (TableLayout)findViewById(R.id.subtable);
        table = (TableLayout)findViewById(R.id.table);
        tempLimit = (EditText)findViewById(R.id.tempLimit);

        submitMash.setEnabled(false);
        allgood = false;

        addStage.setOnClickListener(new View.OnClickListener() {
            public void onClick (View v){

                submitMash.setEnabled(true);
                table.setVisibility(VISIBLE);

                LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View tr = inflater.inflate(R.layout.definemash_row, null, false);
                subtable.addView(tr);
            }
        });

        deleteStage.setOnClickListener(new View.OnClickListener() {
            public void onClick (View v){
                int childCount = subtable.getChildCount();
                if (childCount > 1) {
                    subtable.removeViews(childCount - 1, 1);
                }
            }
        });

        submitMash.setOnClickListener(new View.OnClickListener(){
            public void onClick (View v){
                for(int i = 0, j = subtable.getChildCount(); i < j; i++) {
                    TableLayout layout = (TableLayout) findViewById(R.id.subtable);
                    View child = layout.getChildAt(i);
                    TableRow t = (TableRow) child;
                    EditText firstTextView = (EditText) t.getChildAt(0);
                    EditText secondTextView = (EditText) t.getChildAt(1);
                    String firstText = firstTextView.getText().toString();
                    String secondText = secondTextView.getText().toString();
                    temperatureLimit = tempLimit.getText().toString();
                    if (firstText.equals("") || secondText.equals("") || temperatureLimit.equals("")){
                        final AlertDialog.Builder builder = new AlertDialog.Builder(DefineMashActivity.this, android.R.style.Theme_Material_Dialog_Alert);
                        builder.setTitle("SOME INFORMATION IS MISSING");
                        builder.setMessage("Click OK and add missing information.");
                        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        builder.show();
                    }
                    else {
                        Integer firstTextInt = Integer.parseInt(firstText);
                        Integer secondTextInt = Integer.parseInt(secondText);
                        temps.add(firstTextInt);
                        times.add(secondTextInt);
                        allgood = true;
                    }
                }
                if (allgood){
                    Intent intent = new Intent(v.getContext(), DeviceScanActivity.class);
                    intent.putExtra("temps_list", temps);
                    intent.putExtra("times_list", times);
                    intent.putExtra("tempLimit", temperatureLimit);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        allgood = false;
        temperatureLimit = "";
        temps.clear();
        times.clear();
    }
}
