package org.teva.piroid;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
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
    final byte delimiter = 33;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice = null;
    int readBufferPosition = 0;
    // https://stackoverflow.com/questions/42678488/streaming-live-video-from-raspberry-pi-to-my-android-app-but-getting-security-ex
    String vidAddress = "http://192.168.0.14:8081/";
    private WebView webView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Handler handler = new Handler();

        webView = (WebView) findViewById(R.id.webView);
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        webView.getSettings().setJavaScriptEnabled(true);


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


        final EditText lcd1 = (EditText) findViewById(R.id.lcd1);
        lcd1.addTextChangedListener(new LCDTextWatcher(0));
        lcd1.setOnFocusChangeListener(new LCDFocusChangeListener(0));
        final EditText lcd2 = (EditText) findViewById(R.id.lcd2);
        lcd2.addTextChangedListener(new LCDTextWatcher(1));
        lcd2.setOnFocusChangeListener(new LCDFocusChangeListener(1));


        //actions

        Button meteo = addAction("Meteo", "action:meteo");

        //default focus
        meteo.setFocusable(true);
        meteo.setFocusableInTouchMode(true);///add this line
        meteo.requestFocus();

        Button lightOff = addAction("Lumière", "lcd:off");

        Button coucou = addAction("Coucou", "action:coucou");

        Button R2_screaming = addSound("Cri de R2D2", "sound:R2_screaming.wav");
        Button Chewbacca_Sound_10 = addSound("Chewbacca", "sound:Chewbacca_Sound_10.wav");

/*
        Button meteo = addAction("Meteo", "action:meteo");

        final Button meteo = (Button) findViewById(R.id.meteo);
        meteo.setOnClickListener(new SendMessageClickHandler("action:meteo"));

        final Button coucou = (Button) findViewById(R.id.coucou);
        coucou.setOnClickListener(new SendMessageClickHandler("action:coucou"));

        final Button lightOff = (Button) findViewById(R.id.lightOff);
        lightOff.setOnClickListener(new SendMessageClickHandler("lcd:off"));
*/

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
                    Log.i(TAG, "Device " + device.getName());
                    if (device.getName().equalsIgnoreCase("piroman") || device.getName().equalsIgnoreCase("raspberrypi")) //Note, you will need to change this to match the name of your device
                    {
                        Log.i(TAG, "Found bluetoooth device " + device.getName());
                        mmDevice = device;
                        break;
                    }
                }
            }
        }

        if (mmDevice == null) {

        } else {
            //https://stackoverflow.com/questions/11430182/android-see-images-of-ip-camera-with-webview-on-android-2-2

            loadVideoUrl();
            webView.setOnTouchListener(new View.OnTouchListener(){

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    loadVideoUrl();
                    return true;
                }
            });
            //webView.loadUrl(vidAddress);

        }
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
    private Button addAction(String text, String msg) {
        final LinearLayout row = (LinearLayout) findViewById(R.id.actionsView);
        return addButton(text, msg, row);
    }

    @NonNull
    private Button addSound(String text, String msg) {
        final LinearLayout row = (LinearLayout) findViewById(R.id.soundsView);
        return addButton(text, msg, row);
    }

    @NonNull
    private Button addButton(String text, String msg, LinearLayout row) {
        Button btnTag = new Button(this);
        btnTag.setBackgroundResource(R.drawable.button2);
        btnTag.setTextColor(getResources().getColor(R.color.colorWhite));
        btnTag.setPadding(6, 4, 6, 4);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(6, 2, 6, 2);
        btnTag.setLayoutParams(params);
        btnTag.setText(text);
        btnTag.setOnClickListener(new SendMessageClickHandler(msg));
        row.addView(btnTag);
        return btnTag;
    }

    public void sendBluetoothMessage(String msg2send) {
        // same as in Pyroman/scripts/piroman-server.py >> uuid
        UUID uuid = UUID.fromString("7be1fcb3-5776-42fb-91fd-2ee7b5bbb86d"); //Standard SerialPortService ID
        try {
            System.out.println("send " + msg2send);
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
            synchronized (mmDevice) {

                sendBluetoothMessage(btMsg);
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
                    Log.i(TAG,"Send :"+btMsg+" \tReceive "+readMessage);
                    if (callback != null)
                        callback.onReceiveValue(readMessage);


                    mmSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
            (new Thread(new WorkerThread(msg))).start();

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
}