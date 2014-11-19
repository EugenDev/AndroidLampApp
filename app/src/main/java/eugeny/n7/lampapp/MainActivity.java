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


public class MainActivity extends Activity implements DataReceivedListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_ENABLE_BT = 1;

    private Context appContext = null;

    private SeekBar redSeekBar;
    private SeekBar greenSeekBar;
    private SeekBar blueSeekBar;

    private SurfaceView surfaceView;
    private TextView statusTextView;

    private BluetoothAdapter bluetoothAdapter = null;
    private static final String DEVICE_NAME = "MoodLamp";
    private DataReceiver dataReceiver = null;

    private void ProcessColorChanging() {
        int newColor = Color.rgb(redSeekBar.getProgress(), greenSeekBar.getProgress(), blueSeekBar.getProgress());
        surfaceView.setBackgroundColor(newColor);
    }

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            ProcessColorChanging();
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

        redSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        greenSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        blueSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);

        statusTextView = (TextView)findViewById(R.id.statusTextView);
        surfaceView = (SurfaceView)findViewById(R.id.surfaceView);

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
            dataReceiver.addDateReceivedListener(this);
            dataReceiver.start();
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

    //TODO: Listen to event and do something useful
    @Override
    public void dataReceived(byte[] data) {
        String s = new String(data);
        Log.i(TAG + "dataReceived", s);
        this.runOnUiThread(new ChangeUiTextThread(s));
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
}
