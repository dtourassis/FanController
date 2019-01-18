package com.lethergo.fancontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FanController";

    private BluetoothAdapter bluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private boolean BT_READY = false;
    private boolean CONNECTED = false;
    private BluetoothSocket btSocket;
    private OutputStream outStream;

    TextView textView1, textView2, textView3, textView4, textView5, textView6;
    ToggleButton btnTgl;
    FloatingActionButton btnSync;

    // UUID hint from Android dev docs
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC address of BT device
    private static String address = "00:14:04:12:02:05";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        textView1 = findViewById(R.id.textView1);
        textView2 = findViewById(R.id.textView2);
        textView3 = findViewById(R.id.textView3);
        textView4 = findViewById(R.id.textView4);
        textView5 = findViewById(R.id.textView5);
        textView6 = findViewById(R.id.textView6);
        btnTgl = findViewById(R.id.toggleButton);
        btnSync = findViewById(R.id.floatingActionButton);
        btnTgl.setEnabled(false);

        btnTgl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(CONNECTED) {
                    if (isChecked) {
                        sendData("1");
                        Toast msg = Toast.makeText(getBaseContext(),
                                "You have turned the fans ON!", Toast.LENGTH_SHORT);
                        msg.show();
                    } else {
                        sendData("0");
                        Toast msg = Toast.makeText(getBaseContext(),
                                "You have turned the fans OFF!", Toast.LENGTH_SHORT);
                        msg.show();
                    }
                }
            }
        });

        btnSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToDevice();
            }
        });

        // Check if device has bluetooth and if its enabled
        bluetoothCheck(bluetoothAdapter);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if(BT_READY) {

            // Get device name from the paired devices list
            findFromPaired();

            // Try to connect to selected device
            connectToDevice();

        }

    }

    @Override
    public void onPause() {
        super.onPause();

        if(BT_READY) {
            if (outStream != null) {
                try {
                    outStream.flush();
                } catch (IOException e) {
                    Log.e("TAG", "Failed to flush output stream: " + e.getMessage() + ".");
                }
            }

            try     {
                btSocket.close();
            } catch (IOException e2) {
                Log.e(TAG, "Unable to close socket during connection failure:" + e2.getMessage() + ".");
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == RESULT_OK) {
                BT_READY = true;
                Toast.makeText(this, "Bluetooth has been enabled!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Bluetooth needs to be enabled for this app to work!", Toast.LENGTH_LONG).show();
                enableBluetooth();
            }
        }
    }

    // Sends a string to the output stream
    protected void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        Log.d(TAG, "Sending data: " + message + "...");

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            Log.e(TAG,"An exception occurred during write: " + e.getMessage());
        }
    }

    // Looks into the paired devices to find the one with the requested MAC address.
    public void findFromPaired() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        for(BluetoothDevice device : pairedDevices) {
            if(device.getAddress().equals(address)) {
                textView2.setText(device.getName());
                Log.d(TAG, "Found device with name: " + device.getName());
                textView4.setText(device.getAddress());
            }
        }
    }

    // Checks if device has bluetooth and if it's enabled
    public void bluetoothCheck(BluetoothAdapter btAdapter) {
        if (btAdapter == null) {
            // Device doesn't support Bluetooth, exit the app!
            Toast msg = Toast.makeText(getBaseContext(),
                    "This device doesn't support Bluetooth!", Toast.LENGTH_SHORT);
            msg.show();
            this.finishAffinity();
        } else {
            // Device supports bluetooth, check if it's enabled and if not enable it.
            if(!btAdapter.isEnabled()) {
                enableBluetooth();
            } else {
                BT_READY = true;
            }
        }
    }

    // Launches a prompt to enable BT.
    public void enableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    // Connects to the device and creates the socket
    public void connectToDevice() {
        if(BT_READY) {

            // Get device name from the paired devices list
            findFromPaired();

            // Start comms
            BluetoothDevice btDevice = bluetoothAdapter.getRemoteDevice(address);

            try {
                btSocket = btDevice.createRfcommSocketToServiceRecord(MY_UUID);

            } catch (IOException e) {
                Log.e(TAG, "Socket create failed: " + e.getMessage() + ".");
                Toast msg = Toast.makeText(getBaseContext(),
                        "Could not find device nearby!", Toast.LENGTH_SHORT);
                msg.show();
            }

            bluetoothAdapter.cancelDiscovery();

            Log.d(TAG, "Connecting to Remote...");
            try {
                btSocket.connect();
                CONNECTED = true;
                btnTgl.setEnabled(true);
                textView6.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                textView6.setText("Connected");
                Log.d(TAG, "Connection established and data link opened...");
            } catch (IOException e) {
                try {
                    btSocket.close();
                    Log.d(TAG, "Connection could not be established, closing socket..");
                } catch (IOException e2) {
                    Log.e(TAG, "Unable to close socket during connection failure:" + e2.getMessage() + ".");
                }
            }

            if(CONNECTED) {
                Log.d(TAG, "Creating output stream...");
                try {
                    outStream = btSocket.getOutputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Could not create output stream:" + e.getMessage() + ".");
                }
            }
        }
    }

}
