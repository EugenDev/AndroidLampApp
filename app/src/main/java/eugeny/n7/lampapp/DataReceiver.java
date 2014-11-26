package eugeny.n7.lampapp;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public final class DataReceiver extends Thread {
    private static final String TAG = "DataReceiver";
    private BluetoothSocket m_socket;
    private InputStream m_inStream;
    //TODO: Обернуть потоки в потоки
    private DataInputStream m_dataInputStream;
    private OutputStream m_outStream;

    private boolean isWork = true;
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
                m_dataInputStream = new DataInputStream(m_inStream);
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

        if (m_dataInputStream != null) {
            try {m_dataInputStream.close();} catch (Exception e) {
                Log.e(TAG + "disconnect()", "Error while closing data output stream: " + e.getMessage());
            }
            m_dataInputStream = null;
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

    private static byte START_BYTE = 'B';
    private static byte STOP_BYTE = 'E';
    //TODO: Сделать гибче
    private static byte COMMAND_LENGTH = 4;
    private static byte MESSAGE_LENGTH = 7;

    private byte checksum = 0;
    private int command_pointer = 0;
    private int[] command_buffer = new int[COMMAND_LENGTH];

    @Override
    public void run() {
        connect();
        while(isWork) {
            try {
                byte received_byte = m_dataInputStream.readByte();

                if (command_pointer == 0) {
                    if(received_byte == START_BYTE) {
                        command_pointer++;
                    }
                }
                else if (0 < command_pointer && command_pointer < MESSAGE_LENGTH - 2) {
                    if(received_byte < 0) {
                        command_buffer[command_pointer - 1] = received_byte + 256;
                    } else {
                        command_buffer[command_pointer - 1] = received_byte;
                    }
                    command_pointer++;
                }
                else if (command_pointer == MESSAGE_LENGTH - 2) {
                    checksum = received_byte;
                    command_pointer++;
                }
                else if (command_pointer == MESSAGE_LENGTH - 1) {
                    if(received_byte == STOP_BYTE) {
                        if(checksumIsValid()) {
                            onDataReceived(command_buffer);
                        }
                        command_pointer = 0;
                    }
                } else {
                    command_pointer = 0;
                }
            } catch (Exception ex) {
                command_pointer = 0;
                Log.d(TAG + "-run", "Error: " + ex.getMessage());
                try { sleep(2000); } catch (Exception ex2) {
                    Log.e(TAG + "-run", "Error while sleeping =)");
                }
            }
        }
        Log.d(TAG + "-run", "Thread finished");
    }

    private boolean checksumIsValid() {
        return calcChecksum(command_buffer, COMMAND_LENGTH) == checksum;
    }

    private int calcChecksum(int[] data, int n){
        int result = 0;
        for(int i = 0; i < n; i++)
        {
            result += data[i];
        }
        return result % 256;
    }

    public void stopThread(){
        m_dataReceivedListeners.clear();
        isWork = false;
        disconnect();
    }

    private void onDataReceived(int[] bytes) {
        for (DataReceivedListener listener : m_dataReceivedListeners) {
            listener.dataReceived(bytes);
        }
    }

    public void sendData(int[] data) {
        if(data.length != 4){
            return;
        }

        int[] message = new int[MESSAGE_LENGTH];
        message[0] = START_BYTE;
        for(int i = 1; i <= COMMAND_LENGTH; i++) {
            message[i] = data[i - 1];
        }
        message[MESSAGE_LENGTH - 2] = calcChecksum(data, COMMAND_LENGTH);
        message[MESSAGE_LENGTH - 1] = STOP_BYTE;

        //TODO: Implement bytes sending
        //Log.d(TAG, "" + data[0] +data[1] + data[2] + data[3]);
    }
}