package org.teva.piroid;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import java.io.*;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import piroid.teva.org.piroid.R;

public class MainActivity extends Activity {

    public static final String TAG = "Piroman";
    public static final String DEVICE_PIROMAN = "piroman";
    public static final String DEVICE_RASPBERRYPI = "raspberrypi";
    final byte delimiter = 33;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice = null;
    Object monitor = new Object();
    int readBufferPosition = 0;
    // https://stackoverflow.com/questions/42678488/streaming-live-video-from-raspberry-pi-to-my-android-app-but-getting-security-ex
    String vidAddress = "http://192.168.0.14:8081/";
    private WebView webView;
    private EditText lcd1;
    private EditText lcd2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Handler handler = new Handler();

        webView = (WebView) findViewById(R.id.webView);
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        webView.getSettings().setJavaScriptEnabled(true);


        // start temp button handler
        int delta = 20;
        final Button leftUp = (Button) findViewById(R.id.leftUp);
        ValueCallback<String> callback = null;
        leftUp.setOnClickListener(new SendMessageClickHandler("left:up:" + delta, callback));
        final Button leftDown = (Button) findViewById(R.id.leftDown);
        leftDown.setOnClickListener(new SendMessageClickHandler("left:down:" + delta, callback));

        final Button rightUp = (Button) findViewById(R.id.rightUp);
        rightUp.setOnClickListener(new SendMessageClickHandler("right:up:" + delta, callback));
        final Button rightDown = (Button) findViewById(R.id.rightDown);
        rightDown.setOnClickListener(new SendMessageClickHandler("right:down:" + delta, callback));

        final Button headRight = (Button) findViewById(R.id.headRight);
        headRight.setOnClickListener(new SendMessageClickHandler("head:right:" + delta, callback));
        final Button headLeft = (Button) findViewById(R.id.headLeft);
        headLeft.setOnClickListener(new SendMessageClickHandler("head:left:" + delta, callback));


        lcd1 = (EditText) findViewById(R.id.lcd1);
        lcd1.addTextChangedListener(new LCDTextWatcher(0));
        //lcd1.setOnFocusChangeListener(new LCDFocusChangeListener(0));
        lcd2 = (EditText) findViewById(R.id.lcd2);
        lcd2.addTextChangedListener(new LCDTextWatcher(1));
        //lcd2.setOnFocusChangeListener(new LCDFocusChangeListener(1));


        //actions

        Button meteo = addAction("Meteo", "action:meteo", new ReloadDataValueCallback());

        //default focus
        meteo.setFocusable(true);
        meteo.setFocusableInTouchMode(true);///add this line
        meteo.requestFocus();

        Button lightOff = addAction("Lumière", "lcd:off", null);

        Button coucou = addAction("Coucou", "action:coucou", new ReloadDataValueCallback());

        Button R2_screaming = addSound("Cri de R2D2", "sound:R2_screaming.wav", null);
        Button Chewbacca_Sound_10 = addSound("Chewbacca", "sound:Chewbacca_Sound_10.wav", null);


        initDevice();


    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mmDevice == null) {

        } else {
            //https://stackoverflow.com/questions/11430182/android-see-images-of-ip-camera-with-webview-on-android-2-2

            loadVideoUrl();
            webView.setOnTouchListener(new VideoOnTouchListener());
            loadLcd();
            loadSounds();
        }
    }

    private void initDevice() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
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
                    Log.i(TAG, "Device " + device.getName());
                    if (device.getName().equalsIgnoreCase(DEVICE_PIROMAN) || device.getName().equalsIgnoreCase(DEVICE_RASPBERRYPI)) //Note, you will need to change this to match the name of your device
                    {
                        Log.i(TAG, "Found bluetoooth device " + device.getName());
                        mmDevice = device;
                        break;
                    }
                }
            }
        }
    }

    private void loadLcd() {
        (new Thread(new WorkerThread("lcd:get", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                final String[] vals = value.split("\n");
                final String lcd1Msg = vals.length > 0 ? vals[0] : "";
                final String lcd2Msg = vals.length > 1 ? vals[1] : "";

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        lcd1.setText(lcd1Msg);
                        lcd2.setText(lcd2Msg);
                    }
                });


            }
        }))).start();
    }
    private void loadSounds() {
        (new Thread(new WorkerThread("sound:get", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                final String[] sounds = value.split("\n");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final LinearLayout row = (LinearLayout) findViewById(R.id.soundsView);
                        row.removeAllViews();
                        for (String sound:sounds) {
                            String msg=sound.substring(0,sound.indexOf("."));
                            Button bt= addSound(msg, "sound:"+sound, null);
                        }
                    }
                });


            }
        }))).start();
    }
    private void loadVideoUrl() {
        (new Thread(new WorkerThread("getcameraurl", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                vidAddress = value;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String html = "<!DOCTYPE html>\n" +
                                "<html>\n" +
                                "<head>\n" +
                                "\t<title></title>\n" +
                                "</head>\n" +
                                "<body>\n" +
                                "<center><img width='100%' src=\"" + vidAddress + "\"></center>\n" +
                                "</body>\n" +
                                "</html>";
                        String mime = "text/html";
                        String encoding = "utf-8";
                        webView.loadDataWithBaseURL(null, html, mime, encoding, null);
                    }
                });


            }
        }))).start();
    }

    @NonNull
    private Button addAction(String text, String msg, ValueCallback<String> callback) {
        final LinearLayout row = (LinearLayout) findViewById(R.id.actionsView);
        return addButton(text, msg, row, callback);
    }

    @NonNull
    private Button addSound(String text, String msg, ValueCallback<String> callback) {
        final LinearLayout row = (LinearLayout) findViewById(R.id.soundsView);
        return addButton(text, msg, row, callback);
    }

    @NonNull
    private Button addButton(String text, String msg, LinearLayout row, ValueCallback<String> callback) {
        Button btnTag = new Button(this);
        btnTag.setBackgroundResource(R.drawable.button2);
        btnTag.setTextColor(getResources().getColor(R.color.colorWhite));
        btnTag.setPadding(6, 4, 6, 4);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(6, 2, 6, 2);
        btnTag.setLayoutParams(params);
        btnTag.setText(text);
        btnTag.setOnClickListener(new SendMessageClickHandler(msg, callback));
        row.addView(btnTag);
        return btnTag;
    }

    public void sendBluetoothMessage(String msg2send) {
        // same as in Pyroman/scripts/piroman-server.py >> uuid
        UUID uuid = UUID.fromString("7be1fcb3-5776-42fb-91fd-2ee7b5bbb86d"); //Standard SerialPortService ID
        try {
            initDevice();
            System.out.println("send " + msg2send + " " + mmDevice);
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            if (!mmSocket.isConnected()) {
                mmSocket.connect();
            }

            OutputStream mmOutputStream = mmSocket.getOutputStream();
            mmOutputStream.write(msg2send.getBytes());

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    final class WorkerThread implements Runnable {

        ValueCallback<String> callback;
        private String btMsg;

        public WorkerThread(String msg) {
            btMsg = msg;
        }

        public WorkerThread(String msg, ValueCallback<String> callback) {
            btMsg = msg;
            this.callback = callback;
        }

        public void run() {
            synchronized (monitor) {
                long deb = System.currentTimeMillis();
                sendBluetoothMessage(btMsg);
                System.out.println("timse send " + (System.currentTimeMillis() - deb));
                //while (!Thread.currentThread().isInterrupted()) {
                int bytesAvailable;
                boolean workDone = false;

                try {


                    final InputStream mmInputStream;
                    mmInputStream = mmSocket.getInputStream();
                    bytesAvailable = mmInputStream.available();
                    byte[] buffer = new byte[1024];
                    int bytes = mmInputStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    Log.i(TAG, "Send :" + btMsg + " \tReceive " + readMessage);
                    if (readMessage.startsWith("msg:"))
                        readMessage = readMessage.substring(4);
                    if (callback != null)
                        callback.onReceiveValue(readMessage);


                    mmSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("timse total " + (System.currentTimeMillis() - deb));

            }
        }
    }

    ;

    private class SendMessageClickHandler implements View.OnClickListener {
        private final String msg;
        ValueCallback<String> callback;
        public SendMessageClickHandler(String msg, ValueCallback<String> callback) {
            this.msg = msg;
            this.callback=callback;
        }

        public void onClick(View v) {
            (new Thread(new WorkerThread(msg,callback))).start();

        }
    }

    private class LCDTextWatcher implements TextWatcher {
        private final int line;

        public LCDTextWatcher(int line) {
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

    private class LCDFocusChangeListener implements View.OnFocusChangeListener {
        private final int line;

        public LCDFocusChangeListener(int line) {
            this.line = line;
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            (new Thread(new WorkerThread("line:" + line + ":" + ((EditText) v).getText()))).start();
        }
    }

    private class VideoOnTouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            loadVideoUrl();
            return true;
        }
    }

    private class ReloadDataValueCallback implements ValueCallback<String> {
        @Override
        public void onReceiveValue(String value) {
            loadLcd();
        }
    }
}