package com.happyfox.btbotcontrol.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Set;


public class RobotSelect extends ActionBarActivity {

    public final static String BLUETOOTH_DEVICE = "com.happyfox.btbotcontrol.DEVICE";

    static final int ENABLE_BLUETOOTH = 0;


    private BluetoothAdapter btAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_select);
    }

    @Override
    protected void onStart(){
        super.onStart();

        BluetoothManager btManager;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
            btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
            btAdapter = btManager.getAdapter();
        }
        else{
            btAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), ENABLE_BLUETOOTH);
        btAdapter.cancelDiscovery();
    }

    protected void onPause(){
       super.onPause();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.robot_select, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == ENABLE_BLUETOOTH){
            if(resultCode == RESULT_OK){
                populate_list();
            }
            else{
                // TODO: Blow up and quit.
            }
        }
    }

    private void populate_list(){
        Set<BluetoothDevice> bluetoothDeviceSet = btAdapter.getBondedDevices();

        class BtDevFormatter {

            public BluetoothDevice btDevice;

            public BtDevFormatter(BluetoothDevice device){
                btDevice = device;
            }

            @Override
            public String toString(){
                return btDevice.getName();
            }

        }

        ArrayList<BtDevFormatter> deviceNames = new ArrayList<BtDevFormatter>();

        for(BluetoothDevice device : bluetoothDeviceSet){
            deviceNames.add(new BtDevFormatter(device));
        }

        ListView devList = (ListView)findViewById(R.id.deviceList);

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,
                                                deviceNames.toArray());
        devList.setAdapter(adapter);

        devList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                BtDevFormatter devFormatter = (BtDevFormatter)adapterView.getItemAtPosition(position);
                devFormatter.btDevice.getName();

                Intent intent = new Intent(RobotSelect.this, CockpitActivity.class);
                intent.putExtra(BLUETOOTH_DEVICE, devFormatter.btDevice);
                startActivity(intent);
            }
        });
    }

}
