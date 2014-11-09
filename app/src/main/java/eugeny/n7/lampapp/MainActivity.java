package eugeny.n7.lampapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;


public class MainActivity extends Activity implements IMessageReceivedListener
{

    private static final int REQUEST_ENABLE_BT = 1;

    private SeekBar redSeekBar;
    private SeekBar greenSeekBar;
    private SeekBar blueSeekBar;

    private SurfaceView surfaceView;

    private TextView statusTextView;

    private LampThread _lampThread;

    private void ProcessColorChanging() {
        int newColor = Color.rgb(redSeekBar.getProgress(), greenSeekBar.getProgress(), blueSeekBar.getProgress());
        surfaceView.setBackgroundColor(newColor);
    }

    private SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
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
        Log.e("onCreate", "Activity - onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        statusTextView = (TextView)findViewById(R.id.statusTextView);


        redSeekBar = (SeekBar)findViewById(R.id.redSeekBar);
        greenSeekBar = (SeekBar)findViewById(R.id.greenSeekBar);
        blueSeekBar = (SeekBar)findViewById(R.id.blueSeekBar);

        redSeekBar.setOnSeekBarChangeListener(listener);
        greenSeekBar.setOnSeekBarChangeListener(listener);
        blueSeekBar.setOnSeekBarChangeListener(listener);

        BluetoothAdapter _btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(_btAdapter == null)
        {
            Log.e("onCreate", "Bluetooth not supported.");

            AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
            dlgAlert.setMessage("Bluetooth doesn't available on this device");
            dlgAlert.setTitle("Error");
            dlgAlert.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) { }
                }
            );
            dlgAlert.setCancelable(true);
            dlgAlert.create().show();
        }

        if (!_btAdapter.isEnabled())
        {
            Log.d("onCreate", "Bluetooth disabled. Requesting to enable.");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else
        {
            Log.d("onCreate", "Bluetooth already enabled.");
            connectToDevice();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != RESULT_OK)
        {
            Log.d("onCreate", "Request for enable bluetooth was declined by user.");
            return;
        }
        Log.d("onCreate", "Bluetooth enabled by user.");
        connectToDevice();
    }

    private void connectToDevice() {
        try {
            Log.d("connectToDevice", "Starting lamp thread.");
            _lampThread = new LampThread(BluetoothAdapter.getDefaultAdapter(), "EUGENY-LAPTOP");
            _lampThread.start();
            _lampThread.addMessageReceivedListener(this);
            Log.d("connectToDevice", "Lamp thread started.");
        }
        catch (Exception ex){
            Log.d("connectToDevice exception", ex.getMessage());
        }
    }

    @Override
    public void MessageReceived(String s) {
        Log.e("MessageReceived", s);
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

    @Override
    public void onStop() {
        Log.e("onStop", "Activity - onStop");
        if(_lampThread != null) {
            _lampThread.stopThread();
        }
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}


class LampThread extends Thread {
    private List<IMessageReceivedListener> messageReceivedListeners = new ArrayList<IMessageReceivedListener>();
    public void addMessageReceivedListener(IMessageReceivedListener listener) {
        messageReceivedListeners.add(listener);
    }

    private BluetoothDevice m_device;
    private BluetoothAdapter m_adapter;
    private String m_deviceName;

    public LampThread(BluetoothAdapter adapter, String deviceName) throws Exception {
        m_adapter = adapter;
        m_deviceName = deviceName;
        boolean deviceFounded = false;
        for (BluetoothDevice device : m_adapter.getBondedDevices())
        {
            if(device.getName().equals(m_deviceName))
            {
                m_device = device;
                Log.d("LampThread constructor", "Lamp device founded");
                deviceFounded = true;
                m_socket = m_device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                Log.d("LampThread constructor", "Socket created");
            }
        }
        if (!deviceFounded) {
            throw new Exception("Device not founded");
        }
    }

    private boolean isWork = true;
    private BluetoothSocket m_socket;
    private byte[] buffer = new byte[1024];
    private int nBytes;

    @Override
    public void run() {
        while(isWork) {
            try {
                if (!m_socket.isConnected()) {
                    Log.d("LampThread-run", "Trying to connect to socket");
                    m_socket.connect();
                }
                Log.d("LampThread-run", "Socket connected. Trying to read data");
                InputStream iStream = m_socket.getInputStream();
                nBytes = iStream.read(buffer);
                Log.d("LampThread-run", nBytes + " bytes received");
                byte b[] = Arrays.copyOfRange(buffer, 0, nBytes);
                String resString = new String(b, "UTF-8");
                onMessageReceived(resString);
                Log.d("LampThread-run", nBytes + " bytes received");

            } catch (Exception ex) {
                Log.d("LampThread-run exception", "Error: " + ex.getMessage());
                try {
                    sleep(500);
                } catch (Exception ex2) {
                }
            }
        }
        Log.d("LampThread-run", "Thread finished");
    }

    public void stopThread(){
        isWork = false;
    }

    private void onMessageReceived(String msg) {
        for(IMessageReceivedListener listener : messageReceivedListeners) {
            listener.MessageReceived(msg);
        }
    }
}

interface IMessageReceivedListener {
    public void MessageReceived(String s);
}
