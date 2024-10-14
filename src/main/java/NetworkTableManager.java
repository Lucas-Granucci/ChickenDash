import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTableValue;

public final class NetworkTableManager {

    private static final Map<String, Object> masterTable = new ConcurrentHashMap<>();
    private static final Set<String> topics = new CopyOnWriteArraySet<>();

    private static final NetworkTableInstance ntInst = NetworkTableInstance.getDefault();

    private NetworkTableManager() {}

    static {

        ntInst.startClient4("Chicken Dash Client");
        ntInst.setServer("localhost", NetworkTableInstance.kDefaultPort4);

        ntInst.addListener(
            new String[] {""},
            EnumSet.of(NetworkTableEvent.Kind.kTopic),
            event -> {
                String topicName = event.topicInfo.name;
                topics.add(topicName);
            }
        );
    }

    public static void populateMasterTable() {
        for (String topic : topics) {
            NetworkTableEntry entry = ntInst.getEntry(topic);
            if (entry.exists()){
                NetworkTableValue value = entry.getValue();
                Object decodedValue = decodeNTValue(value);
                masterTable.put(topic, decodedValue);
            }
        }
    }

    public static Object getValue(String topic) {

        Object valueObject = masterTable.get(topic);

        if (valueObject == null) {
            return null;
        }

        if (valueObject instanceof String || valueObject instanceof Number || 
            valueObject instanceof Boolean || valueObject instanceof byte[]) {
            return valueObject;
        } else if (valueObject instanceof String[]) {
            return Arrays.toString((String[]) valueObject);
        } else if (valueObject instanceof double[]) {
            return Arrays.toString((double[]) valueObject);
        } else if (valueObject instanceof boolean[]) {
            return Arrays.toString((boolean[]) valueObject);
        } else if (valueObject instanceof List) {
            return valueObject.toString();
        } else if (valueObject instanceof Map) {
            return valueObject.toString();
        } else {
            System.err.println("Unhandled type: " + valueObject.getClass().getName());
            return valueObject.toString();
        }
    }

    public static Map<String, Object> getMasterTable() {
        return masterTable;
    }

    public static boolean isConnected() {
        return ntInst.isConnected();
    }

    public static Object decodeNTValue(NetworkTableValue ntValue){

        if (ntValue == null) {
            return "null";
        }

        switch (ntValue.getType()) {
            case kBoolean: return ntValue.getBoolean();
            case kDouble: return ntValue.getDouble();
            case kString: return ntValue.getString();
            case kBooleanArray: return ntValue.getBooleanArray();
            case kDoubleArray: return ntValue.getDoubleArray();
            case kStringArray: return ntValue.getStringArray();
            case kRaw: return ntValue.getRaw();
            case kUnassigned: return "Unassigned value";
            default: return "unknown type";
        }
    }
}
