package eugeny.n7.lampapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;


public class MainActivity extends Activity implements LampMessageListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_ENABLE_BT = 1;

    private Context appContext = null;

    private SeekBar redSeekBar;
    private SeekBar greenSeekBar;
    private SeekBar blueSeekBar;

    private SeekBar brightnessSeekBar;
    private SeekBar speedSeekBar;
    private SeekBar holdSeekBar;

    private SurfaceView surfaceView;
    private TextView statusTextView;

    private BluetoothAdapter bluetoothAdapter = null;
    private static final String DEVICE_NAME = "MoodLamp";
    private DataReceiver dataReceiver = null;
    private LampMessageClient lampMessageClient = null;

    private SeekBar.OnSeekBarChangeListener colorChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            int red = redSeekBar.getProgress();
            int green = greenSeekBar.getProgress();
            int blue = blueSeekBar.getProgress();
            lampMessageClient.sendColor(red, green, blue);
            //int newColor = Color.rgb(red, green, blue);
            //surfaceView.setBackgroundColor(newColor);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

    private SeekBar.OnSeekBarChangeListener parametersChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            if(!stateReceiving) {
                int bright = brightnessSeekBar.getProgress();
                int speed = speedSeekBar.getProgress();
                int hold = holdSeekBar.getProgress();
                String str = "" + bright + " " + speed + " " + hold;
                ChangeUiTextThread changeUiTextThread = new ChangeUiTextThread(str);
                runOnUiThread(changeUiTextThread);
                lampMessageClient.sendState(bright, speed, hold);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appContext = getApplicationContext();

        redSeekBar = (SeekBar)findViewById(R.id.redSeekBar);
        greenSeekBar = (SeekBar)findViewById(R.id.greenSeekBar);
        blueSeekBar = (SeekBar)findViewById(R.id.blueSeekBar);

        redSeekBar.setOnSeekBarChangeListener(colorChangeListener);
        greenSeekBar.setOnSeekBarChangeListener(colorChangeListener);
        blueSeekBar.setOnSeekBarChangeListener(colorChangeListener);

        brightnessSeekBar = (SeekBar)findViewById(R.id.brightnessSeekBar);
        speedSeekBar = (SeekBar)findViewById(R.id.speedSeekBar);
        holdSeekBar = (SeekBar)findViewById(R.id.holdSeekBar);

        brightnessSeekBar.setOnSeekBarChangeListener(parametersChangeListener);
        speedSeekBar.setOnSeekBarChangeListener(parametersChangeListener);
        holdSeekBar.setOnSeekBarChangeListener(parametersChangeListener);

        statusTextView = (TextView)findViewById(R.id.statusTextView);
        surfaceView = (SurfaceView)findViewById(R.id.surfaceView);

        initBluetooth();
    }

    private void initBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null)
        {
            Log.i("onCreate", "Bluetooth not supported.");
            toastMessage("Bluetooth is not supported on this device");
            finish();
        }

        if (!bluetoothAdapter.isEnabled())
        {
            Log.i(TAG, "Bluetooth disabled. Requesting to enable.");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else
        {
            Log.i(TAG, "Bluetooth already enabled.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != RESULT_OK)
        {
            Log.i(TAG, "Request for enable bluetooth was declined by user.");
            finish();
            return;
        }
        Log.i(TAG, "Bluetooth enabled by user.");
        startWorking();
    }

    //Assume that bluetooth adapter enabled
    private void startWorking() {
        BluetoothDevice myDevice = getNeededDevice(bluetoothAdapter, DEVICE_NAME);
        if(myDevice == null) {

            Log.i(TAG, "Device not founded. Please pair with device");
            toastMessage("Device not founded. Please pair with the proper device");
            return;
        }
        try {
            BluetoothSocket socket = myDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            dataReceiver = new DataReceiver(socket);
            lampMessageClient = new LampMessageClient(dataReceiver);
            lampMessageClient.addLampMessageListener(this);
            dataReceiver.start();
            requestStateThread.start();
        } catch (Exception ex) {
            Log.e(TAG, "Error while creating bluetooth socket " + ex.getMessage());
        }
    }

    private BluetoothDevice getNeededDevice(BluetoothAdapter adapter, String deviceName) {
        BluetoothDevice result = null;
        for (BluetoothDevice device : adapter.getBondedDevices()) {
            if(device.getName().equals(deviceName)) {
                result = device;
                break;
            }
        }
        return result;
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        startWorking();
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        lampMessageClient = null;
        if(dataReceiver != null && dataReceiver.isAlive()) {
            dataReceiver.stopThread();
            dataReceiver = null;
        }
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onRestart() {
        Log.i(TAG, "onRestart");
        super.onRestart();
    }

    class ChangeUiTextThread implements Runnable {
        private String m_string;
        public ChangeUiTextThread(String s)
        {
            m_string = s;
        }

        @Override
        public void run() {
            statusTextView.setText(m_string);
        }
    }

    private void toastMessage(String msgText) {
        Toast toast = Toast.makeText(appContext, msgText, Toast.LENGTH_SHORT);
        toast.show();
    }

    //********************** Для отображения цвета лампы ***********************
    class ChangeColorThread implements Runnable {
        private int new_color = 0;

        public void setColor(int red, int green, int blue){
            new_color = Color.rgb(red, green, blue);
        }

        @Override
        public void run() {
            surfaceView.setBackgroundColor(new_color);
        }
    }

    private ChangeColorThread changeColorThread = new ChangeColorThread();

    @Override
    public void colorReceived(int red, int green, int blue) {
        changeColorThread.setColor(red, green, blue);
        runOnUiThread(changeColorThread);
    }

    //******************* Для отображения состояния лампы **********************
    class ChangeStateThread implements Runnable {
        private int m_speed = 0;
        private int m_hold = 0;
        private int m_brightness = 0;

        public void setValues(int speed, int hold, int brightness) {
            m_speed = speed;
            m_hold = hold;
            m_brightness = brightness;
        }

        @Override
        public void run() {
            stateReceiving = true;
//            brightnessSeekBar.setProgress(m_brightness);
//            speedSeekBar.setProgress(m_speed);
//            holdSeekBar.setProgress(m_hold);
            stateReceiving = false;
        }
    }

    private boolean stateReceiving = false;

    private ChangeStateThread changeStateThread = new ChangeStateThread();

    @Override
    public void stateReceived(int brightness, int speed, int hold) {
        firstTimeStateReceived = true;
        changeStateThread.setValues(speed, hold, brightness);
        runOnUiThread(changeStateThread);
    }

    //******************* Для запроса состояния лампы **************************
    private boolean firstTimeStateReceived = false;

    class RequestStateThread extends Thread {
        @Override
        public void run() {
            while(!firstTimeStateReceived){
                try {
                    if (lampMessageClient != null) {
                        lampMessageClient.sendParametersRequest();
                    }
                    Thread.sleep(1000);
                } catch (Exception ex) {
                  Log.d(TAG, "Unexpected error in RequestStateThread: " + ex.getMessage());
                }
            }
        }
    }

    private RequestStateThread requestStateThread = new RequestStateThread();
}
