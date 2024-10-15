import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import edu.wpi.first.networktables.GenericPublisher;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTableValue;
import edu.wpi.first.networktables.Topic;

public final class NetworkTableManager {

    private static final Map<String, Object> masterTable = new ConcurrentHashMap<>();

    private static final Set<String> topics = new CopyOnWriteArraySet<>();
    private static final Map<String, GenericPublisher> publisherCache = new ConcurrentHashMap<>();

    private static final NetworkTableInstance ntInst = NetworkTableInstance.getDefault();

    private NetworkTableManager() {}

    public static void connectToNetworkTables(String teamNumIP) {

        ntInst.disconnect();

        ntInst.startClient4("Chicken Dash Client");
        ntInst.setServer(teamNumIP, NetworkTableInstance.kDefaultPort4);

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
        return valueObject;
    }

    public static Map<String, Object> getMasterTable() {
        return masterTable;
    }

    public static void publishValue(String topicType, String topicName, Object newValue) {

        GenericPublisher publisher = publisherCache.computeIfAbsent(topicName, key -> {
            Topic topic = ntInst.getTopic(topicName);
            System.out.println(topic.getName());
            return topic.genericPublish(topicType);
        });

        NetworkTableValue ntValue = creatNetworkTableValue(topicType, newValue);

        if (ntValue != null) {
            if (!ntValue.equals(masterTable.get(topicName))) {
                publisher.set(ntValue);
            }
        }

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

    private static NetworkTableValue creatNetworkTableValue(String topicType, Object newValue) {
        NetworkTableValue ntValue = switch (topicType) {
            case "string" -> NetworkTableValue.makeString((String) newValue);
            case "int"    -> NetworkTableValue.makeInteger((Integer) newValue);
            case "double" -> NetworkTableValue.makeDouble((Double) newValue);
            case "boolean" -> NetworkTableValue.makeBoolean((Boolean) newValue);
            default -> {
                System.out.println("Unsupported topic type: " + topicType);
                yield null;
            }
        };
        return ntValue;
    }
}
