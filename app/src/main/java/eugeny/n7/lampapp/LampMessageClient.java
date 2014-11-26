package eugeny.n7.lampapp;

import java.util.ArrayList;
import java.util.List;

public class LampMessageClient implements DataReceivedListener {
    private DataReceiver m_dataReceiver;

    private List<LampMessageListener> m_lampMessageListeners;
    public void addLampMessageListener(LampMessageListener listener){
        m_lampMessageListeners.add(listener);
    }

    public LampMessageClient(DataReceiver dataReceiver) {
        m_dataReceiver = dataReceiver;
        m_lampMessageListeners = new ArrayList<LampMessageListener>();
        m_dataReceiver.addDateReceivedListener(this);
    }

    private void onColorReceived(int red, int green, int blue){
        for(LampMessageListener listener : m_lampMessageListeners){
            listener.colorReceived(red, green, blue);
        }
    }

    private void onStateReceived(int brightness,int speed, int hold) {
        for(LampMessageListener listener : m_lampMessageListeners){
            listener.stateReceived(brightness, speed, hold);
        }
    }

    @Override
    public void dataReceived(int[] data) {
        LampMessageType messageType = LampMessageType.fromInteger(data[0]);

        switch (messageType){
            case COLOR:
                onColorReceived(data[1], data[2], data[3]);
                break;

            case PARAMETERS_RESPONSE:
                onStateReceived(data[1], data[2], data[3]);
                break;

            default:
                break;
        }
    }

    public void sendState(int brightness, int speed, int hold) {
        int[] data = new int[4];
        data[0] = LampMessageType.PARAMETERS_RESPONSE.getValue();
        data[1] = brightness;
        data[2] = speed;
        data[3] = hold;
        this.m_dataReceiver.sendData(data);
    }

    public void sendColor(int red, int green, int blue) {
        int[] data = new int[4];
        data[0] = LampMessageType.COLOR.getValue();
        data[1] = red;
        data[2] = green;
        data[3] = blue;
        this.m_dataReceiver.sendData(data);
    }

    public void sendParametersRequest() {
        int[] data = new int[4];
        data[0] = LampMessageType.PARAMETERS_REQUEST.getValue();
        this.m_dataReceiver.sendData(data);
    }
}
