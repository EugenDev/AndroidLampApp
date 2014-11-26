package eugeny.n7.lampapp;

import java.util.HashMap;
import java.util.Map;

public enum LampMessageType {
    NONE(0),
    PARAMETERS_RESPONSE(1),
    COLOR(2),
    PARAMETERS_REQUEST(3);

    private final int value;

    /**
     * Get Integer value from enum
     * @return Value
     */
    public int getValue(){
        return value;
    }

    private LampMessageType(int val) {
        this.value = val;
    }

    private static final Map<Integer, LampMessageType> _map = new HashMap<Integer, LampMessageType>();
    static {
        for (LampMessageType msgType : LampMessageType.values()) {
            _map.put(msgType.getValue(), msgType);
        }
    }

    /**
     * Get LampMessageType from Integer value
     * @param value Value
     * @return Lamp message type
     */
    public static LampMessageType fromInteger(int value) {
        if(_map.containsKey(value)) {
            return _map.get(value);
        } else {
            return LampMessageType.NONE;
        }
    }
}
