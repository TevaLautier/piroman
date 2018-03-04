package org.teva.piroid;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.*;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import piroid.teva.org.piroid.R;

public class MovementActivity extends Activity {

    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice = null;

    final byte delimiter = 33;
    int readBufferPosition = 0;


    public void sendBtMsg(String msg2send) {
        //UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        UUID uuid = UUID.fromString("7be1fcb3-5776-42fb-91fd-2ee7b5bbb86d"); //Standard SerialPortService ID
        try {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            if (!mmSocket.isConnected()) {
                mmSocket.connect();
            }

            String msg = msg2send;
            //msg += "\n";
            OutputStream mmOutputStream = mmSocket.getOutputStream();
            mmOutputStream.write(msg.getBytes());

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Handler handler = new Handler();


        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        // start temp button handler
        int delta = 20;
        final Button leftUp = (Button) findViewById(R.id.leftUp);
        leftUp.setOnClickListener(new SendMessageClickHandler("left:up:" + delta));
        final Button leftDown = (Button) findViewById(R.id.leftDown);
        leftDown.setOnClickListener(new SendMessageClickHandler("left:down:" + delta));

        final Button rightUp = (Button) findViewById(R.id.rightUp);
        rightUp.setOnClickListener(new SendMessageClickHandler("right:up:" + delta));
        final Button rightDown = (Button) findViewById(R.id.rightDown);
        rightDown.setOnClickListener(new SendMessageClickHandler("right:down:" + delta));

        final Button headRight = (Button) findViewById(R.id.headRight);
        headRight.setOnClickListener(new SendMessageClickHandler("head:right:" + delta));
        final Button headLeft = (Button) findViewById(R.id.headLeft);
        headLeft.setOnClickListener(new SendMessageClickHandler("head:left:" + delta));

        final Button meteo = (Button) findViewById(R.id.meteo);
        meteo.setOnClickListener(new SendMessageClickHandler("action:meteo"));

        final Button coucou = (Button) findViewById(R.id.coucou);
        coucou.setOnClickListener(new SendMessageClickHandler("action:coucou"));

        final Button lightOff = (Button) findViewById(R.id.lightOff);
        lightOff.setOnClickListener(new SendMessageClickHandler("lcd:off"));

        final EditText lcd1 = (EditText) findViewById(R.id.lcd1);
        lcd1.addTextChangedListener(new MyTextWatcher(0));
        lcd1.setOnFocusChangeListener(new MyOnFocusChangeListener(0));
        final EditText lcd2 = (EditText) findViewById(R.id.lcd2);
        lcd2.addTextChangedListener(new MyTextWatcher(1));
        lcd2.setOnFocusChangeListener(new MyOnFocusChangeListener(1));


        // end light off button handler
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled())

            {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);
            }

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0)

            {
                for (BluetoothDevice device : pairedDevices) {
                    Log.e("Piroid","Device "+device.getName());
                    if (device.getName().equalsIgnoreCase("piroman") || device.getName().equalsIgnoreCase("raspberrypi")) //Note, you will need to change this to match the name of your device
                    {
                        Log.e("PiRoMan", "Found bluetoooth device "+device.getName());
                        mmDevice = device;
                        break;
                    }
                }
            }
        }


    }

    final class WorkerThread implements Runnable {

        private String btMsg;

        public WorkerThread(String msg) {
            btMsg = msg;
        }

        public void run() {
            synchronized (mmDevice) {

                sendBtMsg(btMsg);
                //while (!Thread.currentThread().isInterrupted()) {
                    int bytesAvailable;
                    boolean workDone = false;

                    try {


                        final InputStream mmInputStream;
                        mmInputStream = mmSocket.getInputStream();
                        bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0) {

                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);

                        }


                        mmSocket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }


               // }
            }
        }
    }

    ;

    private class SendMessageClickHandler implements View.OnClickListener {
        private final String msg;

        public SendMessageClickHandler(String msg) {
            this.msg = msg;
        }

        public void onClick(View v) {
            // Perform action on temp button click

            (new Thread(new WorkerThread(msg))).start();

        }
    }

    private class MyTextWatcher implements TextWatcher {
        private final int line;

        public MyTextWatcher(int line) {
            this.line = line;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            (new Thread(new WorkerThread("line:" + line + ":" + ((SpannableStringBuilder) s).toString()))).start();
        }
    }

    private class MyOnFocusChangeListener implements View.OnFocusChangeListener {
        private final int line;

        public MyOnFocusChangeListener(int line) {
            this.line = line;
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            (new Thread(new WorkerThread("line:" + line + ":" + ((EditText) v).getText()))).start();
        }
    }
}