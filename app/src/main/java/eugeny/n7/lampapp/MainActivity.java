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
import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
            String serverName = "MoodLamp";
            //String serverName = "EUGENY-LAPTOP";
            _lampThread = new LampThread(BluetoothAdapter.getDefaultAdapter(), serverName);
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
        while (!deviceFounded) {
            for (BluetoothDevice device : m_adapter.getBondedDevices()) {
                if (device.getName().equals(m_deviceName)) {
                    m_device = device;
                    Log.d("LampThread constructor", "Lamp device founded");
                    deviceFounded = true;
                    m_socket = m_device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                    Log.d("LampThread constructor", "Socket created");
                }
            }
            try {
                sleep(1000);
            } catch (Exception ex) {
            }
        }
        if (!deviceFounded) {
            throw new Exception("Device not founded");
        }
    }

    private boolean isWork = true;
    private BluetoothSocket m_socket;

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int nBytes;
        while(isWork) {
            try {
                if (!m_socket.isConnected()) {
                    Log.d("LampThread-run", "Trying to connect to socket");
                    m_socket.connect();
                }
                Log.d("LampThread-run", "Socket connected. Trying to read data");
                InputStream iStream = m_socket.getInputStream();
                nBytes = iStream.read(buffer);
                Log.d("LampThread-run", nBytes + " bytes received. Processing");
                processMessage(buffer, nBytes);
                Log.d("LampThread-run", "Data processed");

            } catch (Exception ex) {
                Log.d("LampThread-run exception", "Error: " + ex.getMessage());
                try {
                    sleep(500);
                } catch (Exception ex2) {
                }
            }
        }
        Log.d("LampThread-run", "Closing streams");
        try {
            InputStream inputStream = m_socket.getInputStream();
            inputStream.close();
            OutputStream outputStream = m_socket.getOutputStream();
            outputStream.close();
        }catch(Exception ex){
            Log.d("LampThread-run","Error while closing streams");
        }

        Log.d("LampThread-run", "Thread finished");
    }

    public void stopThread(){
        isWork = false;
    }

    private void processMessage(byte[] buffer, int nBytes)
    {
        if(nBytes < 7) {
            return;
        }
    }

    private void onMessageReceived(String msg) {
        for(IMessageReceivedListener listener : messageReceivedListeners) {
            listener.MessageReceived(msg);
        }
    }
}

class LampMessage {
    private int MESSAGE_LENGTH = 7;
    private byte START_BYTE = 0x42;
    private byte STOP_BYTE = 0x45;

    public boolean isValid() {
        return (messageData[MESSAGE_LENGTH - 2] == GetCheckSum())
                && (messageData[0] == START_BYTE && (messageData[MESSAGE_LENGTH - 1] == STOP_BYTE));
    }

    private byte GetCheckSum() {
        byte tmp = 0;
        for (int i = 1; i < MESSAGE_LENGTH - 2; i++)
        {
            tmp += messageData[i];
        }
        return tmp;
    }

    private void RecalculateCheckSum() {
        messageData[MESSAGE_LENGTH - 2] = GetCheckSum();
    }

    private byte[] messageData;

    public MessageType getMessageType() {
        switch(messageData[1]) {
            case 1:
                return MessageType.PARAMETERS_RESPONSE;
            case 2:
                return MessageType.COLOR;
            case 3:
                return MessageType.PARAMETERS_REQUEST;
            default:
            case 0:
                return MessageType.NONE;
        }
    }
    public void setMessageType(MessageType messageType) {
        switch (messageType){
            case NONE:
                messageData[1] = 0;
            case PARAMETERS_RESPONSE:
                messageData[1] = 1;
            case COLOR:
                messageData[1] = 2;
            case PARAMETERS_REQUEST:
                messageData[1] = 3;
        }
        RecalculateCheckSum();
    }

    public byte getRed() {
        return messageData[2];
    }
    public void setRed(byte value) {
        messageData[2] = value;
        RecalculateCheckSum();
    }

    public byte getGreen() {
        return messageData[3];
    }
    public void setGreen(byte value) {
        messageData[3] = value;
        RecalculateCheckSum();
    }

    public byte getBlue() {
        return messageData[4];
    }
    public void setBlue(byte value) {
        messageData[4] = value;
        RecalculateCheckSum();
    }

    public byte getBrightness() {
        return messageData[2];
    }
    public void setBrightness(byte value) {
        messageData[2] = value;
        RecalculateCheckSum();
    }

    public byte getSpeed() {
        return messageData[3];
    }
    public void setSpeed(byte value) {
        messageData[3] = value;
        RecalculateCheckSum();
    }

    public byte getHold() {
        return messageData[4];
    }
    public void setHold(byte value) {
        messageData[4] = value;
        RecalculateCheckSum();
    }

    public LampMessage() {
        messageData = new byte[MESSAGE_LENGTH];
        messageData[0] = START_BYTE;
        messageData[MESSAGE_LENGTH - 1] = STOP_BYTE;
        setMessageType(MessageType.NONE);
    }

    public static void WriteMessage(OutputStream stream, LampMessage message) throws IOException {
        stream.write(message.messageData);
    }

    public static LampMessage ReadMessage(InputStream stream) throws IOException {
        LampMessage m = new LampMessage();
        stream.read(m.messageData);
        return m;
    }
}

enum MessageType {

    NONE,
    PARAMETERS_RESPONSE,
    COLOR,
    PARAMETERS_REQUEST
}

interface IMessageReceivedListener {
    public void MessageReceived(String s);
}
