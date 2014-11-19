package eugeny.n7.lampapp;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class DataReceiver extends Thread {
    private static final String TAG = "DataReceiver";
    private BluetoothSocket m_socket;
    private InputStream m_inStream;
    private OutputStream m_outStream;

    private boolean isWork = true;
    private byte[] buffer = new byte[64];

    private List<DataReceivedListener> m_dataReceivedListeners;

    public void addDateReceivedListener(DataReceivedListener listener) {
        m_dataReceivedListeners.add(listener);
    }

    public DataReceiver(BluetoothSocket socket) throws Exception {
        if  (socket == null){
            throw new IllegalArgumentException("socket");
        }
        m_socket = socket;
        m_dataReceivedListeners = new ArrayList<DataReceivedListener>();
    }

    private void connect() {
        if(!m_socket.isConnected()) {
            try {
                m_socket.connect();
                m_inStream = m_socket.getInputStream();
                m_outStream = m_socket.getOutputStream();
            } catch (Exception ex) {
                Log.e(TAG + " connect()", "Error: " + ex.getMessage());
            }
        }
    }

    private void disconnect() {
        if (m_inStream != null) {
            try {m_inStream.close();} catch (Exception e) {
                Log.e(TAG + "disconnect()", "Error while closing input stream: " + e.getMessage());
            }
            m_inStream = null;
        }

        if (m_outStream != null) {
            try {m_outStream.close();} catch (Exception e) {
                Log.e(TAG + "disconnect()", "Error while closing output stream: " + e.getMessage());
            }
            m_outStream = null;
        }

        try {m_socket.close();} catch (Exception e) {
            Log.e(TAG + "disconnect()", "Error while closing socket: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        int nBytes;
        connect();
        while(isWork) {
            try {
                Log.d(TAG + "-run", "Trying to read data");
                nBytes = m_inStream.read(buffer);
                Log.d(TAG + "-run", nBytes + " bytes received");
                byte b[] = Arrays.copyOfRange(buffer, 0, nBytes);
                onDataReceived(b);
                String resString = new String(b, "UTF-8");
                Log.d(TAG + "-run", "Received: " + resString);
            } catch (Exception ex) {
                Log.d(TAG + "-run", "Error: " + ex.getMessage());
                try { sleep(2000); } catch (Exception ex2) {
                    Log.e(TAG + "-run", "Error while sleeping =)");
                }
            }
        }
        Log.d(TAG + "-run", "Thread finished");
    }

    public void stopThread(){
        isWork = false;
        disconnect();
    }

    private void onDataReceived(byte[] bytes) {
        for(DataReceivedListener listener : m_dataReceivedListeners) {
            listener.dataReceived(bytes);
        }
    }
}