package eugeny.n7.lampapp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class LampMessage {
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

    public LampMessageType getMessageType() {
        switch(messageData[1]) {
            case 1:
                return LampMessageType.PARAMETERS_RESPONSE;
            case 2:
                return LampMessageType.COLOR;
            case 3:
                return LampMessageType.PARAMETERS_REQUEST;
            default:
            case 0:
                return LampMessageType.NONE;
        }
    }
    public void setMessageType(LampMessageType messageType) {
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
        setMessageType(LampMessageType.NONE);
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

