package com.happyfox.btbotcontrol.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.bda.controller.Controller;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public class CockpitActivity extends ActionBarActivity {

    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        CONNECTION_ERROR
    }

    private BluetoothDevice btDevice;

    private BtUpdateRunnable btUpdateRunnable = null;
    private Thread btThread = null;

    private Button connectButton;

    Controller mController = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cockpit);

        Intent intent = getIntent();
        btDevice = intent.getParcelableExtra(RobotSelect.BLUETOOTH_DEVICE);

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        connectButton = (Button)findViewById(R.id.connectButton);

        mController = Controller.getInstance(this);
        mController.init();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.cockpit, menu);
        return false;
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
    public void onStart(){
        super.onStart();
        connect();
    }

    @Override
    public void onDestroy(){
        if(mController != null) {
            mController.exit();
        }

        if(btUpdateRunnable != null) {
            btUpdateRunnable.running = false;

            try {
                btThread.join();
            }
            catch(final InterruptedException e){
                e.printStackTrace();
            }
        }
        super.onDestroy();

    }

    @Override
    protected void onPause()
    {
        super.onPause(); if(mController != null) {
        mController.onPause(); }
    }
    @Override
    protected void onResume() {
        super.onResume(); if(mController != null) {
            mController.onResume(); }
    }

    public void onUiUpdate(boolean connected){
        // TODO: add update code.
    }

    public void updateConnection(ConnectionState state){
        if( state == ConnectionState.CONNECTION_ERROR){
            onConnectionError();
        }
        else if( state == ConnectionState.CONNECTED){
            onConnected();
        }
        else if( state == ConnectionState.CONNECTING){
            onConnecting();
        }
        else if( state == ConnectionState.DISCONNECTED){
            onDisconnect();
        }
    }

    public void onConnectionError(){
        connectButton.setText(R.string.cockpit_connection_error);
        connectButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));

        Context context = getApplicationContext();
        CharSequence text;

        text = getString(R.string.cockpit_connection_error);
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    public void onConnected(){
        connectButton.setText(R.string.cockpit_connected);
        connectButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
    }

    public  void onConnecting(){
        connectButton.setText(R.string.cockpit_connecting);
        connectButton.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
    }

    public void onDisconnect(){
        connectButton.setText(R.string.cockpit_not_connected);
        connectButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
    }

    public void connect(){
        if( btUpdateRunnable == null) {
            onConnecting();
            btUpdateRunnable = new BtUpdateRunnable(this, btDevice, mController);
            btThread = new Thread(btUpdateRunnable);
            btThread.start();
        }
        else if(!btUpdateRunnable.running) {
            onConnecting();
            btUpdateRunnable = new BtUpdateRunnable(this, btDevice, mController);
            btThread = new Thread(btUpdateRunnable);
            btThread.start();
        }
    }

    public void clickConnect(View v){
        connect();
    }
}


class BtUpdateRunnable implements Runnable {

    public volatile boolean running = true;

    private BluetoothDevice btDevice;
    private BluetoothAdapter btAdapter;
    private CockpitActivity cockpitActivity;

    private Controller controller;

    private int lMotor, rMotor;

    public BtUpdateRunnable(CockpitActivity cockpitActivity, BluetoothDevice btDevice,
                            Controller controller){
        this.btDevice = btDevice;
        this.cockpitActivity = cockpitActivity;
        this.controller = controller;

        BluetoothManager btManager;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
            btManager = (BluetoothManager)cockpitActivity.getSystemService(
                    Context.BLUETOOTH_SERVICE);
            btAdapter = btManager.getAdapter();
        }
        else{
            btAdapter = BluetoothAdapter.getDefaultAdapter();
        }
    }


    @Override
    public void run() {
        running = true;

        OutputStream btOutStream;
        BluetoothSocket btSocket;
        UUID sppID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        btAdapter.cancelDiscovery();

        try {
            btSocket = btDevice.createRfcommSocketToServiceRecord(sppID);
        }
        catch (IOException e){
            updateConnectionState(CockpitActivity.ConnectionState.CONNECTION_ERROR);
            running = false;
            e.printStackTrace();
            return;
        }


        try {
            btSocket.connect();
            btOutStream = btSocket.getOutputStream();
        }
        catch (IOException e){
            try {
                btSocket.close();
            }
            catch (IOException e2){
                running = false;
                updateConnectionState(CockpitActivity.ConnectionState.CONNECTION_ERROR);
                e.printStackTrace();
                return;
            }

            running = false;
            updateConnectionState(CockpitActivity.ConnectionState.CONNECTION_ERROR);
            e.printStackTrace();
            return;
        }
        updateConnectionState(CockpitActivity.ConnectionState.CONNECTED);

        while(running){

            // read left analog stick
            final float mX = controller.getAxisValue(Controller.AXIS_X);
            final float mY = controller.getAxisValue(Controller.AXIS_Y);

            int x = (int) (mX * -255);
            int y = (int) (mY * 255);

            calculateMotorLevels(x, y);

            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.order(ByteOrder.BIG_ENDIAN);
            bb.putShort((short)rMotor);
            bb.putShort((short)lMotor);

            byte[] data = bb.array();

            try {
                btOutStream.write(data);
                btOutStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        try {
            btSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateConnectionState(CockpitActivity.ConnectionState.DISCONNECTED);
    }

    public void calculateMotorLevels(int x, int y){
        int V = (255 - java.lang.Math.abs(x)) * (y/255) + y;
        int W = (255 - java.lang.Math.abs(y)) * (x/255) + x;

        rMotor = (V+W)/2;
        lMotor = (V-W)/2;
    }

    public void updateConnectionState(final CockpitActivity.ConnectionState state){
        cockpitActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cockpitActivity.updateConnection(state);
            }
        });
    }
}
