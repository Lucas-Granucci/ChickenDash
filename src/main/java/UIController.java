import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class UIController {

    private final static double fieldLength = 708.0;

    private static GraphicsContext fieldGC;

    private static Map<String, TreeItem<NTDataModel>> treeItemMap = new HashMap<>();

    // Declare UI elements as instance variables
    private static Label robotStatus;
    private static Button connectButton;
    private static ComboBox<String> autoSelector;

    // **************************** DISPLAY ELEMENTS **************************** //

    // Create top bar (Status indicators)
    public static HBox createTopBar() {
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10));
        topBar.setStyle("-fx-background-color: #2c3e50;");

        robotStatus = new Label("Robot: Connected");

        TextField teamNumIPField = new TextField("localhost");

        connectButton = new Button("Connect");
        EventHandler<ActionEvent> connectEvent = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                System.out.println("Connecting with id: " + teamNumIPField.getText());
                NetworkTableManager.connectToNetworkTables(teamNumIPField.getText());
            }
        };
        connectButton.setOnAction(connectEvent);

        Label battery = new Label("Battery: 99V");
        Label timer = new Label("Timer: 0:00");

        robotStatus.setStyle("-fx-text-fill: white;");
        battery.setStyle("-fx-text-fill: white;");
        timer.setStyle("-fx-text-fill: white;");

        topBar.getChildren().addAll(robotStatus, teamNumIPField, connectButton, battery, timer);
        return topBar;
    }

    // Create field view
    public static StackPane createFieldView() {
        StackPane fieldView = new StackPane();

        final Canvas fieldCanvas = new Canvas();
        ImageView fieldImage = new ImageView(UIController.class.getResource("/assets/crescendo-field.jpg").toExternalForm());
        fieldImage.setRotate(180);
        fieldImage.setPreserveRatio(true);

        fieldImage.fitWidthProperty().bind(fieldView.widthProperty());
        fieldImage.fitHeightProperty().bind(fieldView.heightProperty());

        fieldCanvas.widthProperty().bind(fieldView.widthProperty());
        fieldCanvas.heightProperty().bind(fieldView.heightProperty());

        fieldView.setMinSize(100, 100);

        fieldGC = fieldCanvas.getGraphicsContext2D();

        fieldView.getChildren().addAll(fieldImage, fieldCanvas);
        drawRobot(0, 0, 0);

        return fieldView;
    }

    // Create network table viewer
    public static TreeTableView<NTDataModel> createNetworkTableViewer() {

        TreeTableView<NTDataModel> treeTableView = new TreeTableView<>();
        TreeItem<NTDataModel> rootItem = new TreeItem<>(new NTDataModel("NetworkTable", "..."));
        treeTableView.setRoot(rootItem);
        rootItem.setExpanded(true);

        // Define columns
        TreeTableColumn<NTDataModel, String> keyColumn = new TreeTableColumn<>("NT Key");
        TreeTableColumn<NTDataModel, String> valueColumn = new TreeTableColumn<>("NT Value");

        keyColumn.prefWidthProperty().bind(treeTableView.widthProperty().multiply(0.7));
        valueColumn.prefWidthProperty().bind(treeTableView.widthProperty().multiply(0.3));

        keyColumn.setCellValueFactory(param -> param.getValue().getValue().keyProperty());
        valueColumn.setCellValueFactory(param -> param.getValue().getValue().valueProperty());

        treeTableView.getColumns().add(keyColumn);
        treeTableView.getColumns().add(valueColumn);

        treeTableView.setPrefWidth(350);

        return treeTableView;
    }

    // Create control panel (model selector, auto selector, run button)
    public static HBox createControlPanel() {
        
        HBox controlPanel = new HBox(20);
        controlPanel.setPadding(new Insets(10));

        // Auto selector
        autoSelector = new ComboBox<>();
        autoSelector.getItems().addAll("Auto 1", "Auto 2", "Auto 3");
        autoSelector.setValue("Auto 1");
        
        controlPanel.getChildren().addAll(new Label("Auto Routine: "), autoSelector);

        return controlPanel;
    }

    // Create swerve module visualiztaion
    public static GridPane createSwerveModules() {
        GridPane swerveGrid = new GridPane();
        swerveGrid.setHgap(10);
        swerveGrid.setVgap(10);
        swerveGrid.setPadding(new Insets(10));

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++){
                Rectangle module = new Rectangle(50, 50, Color.DARKGRAY);
                swerveGrid.add(module, i, j);
            }
        }

        return swerveGrid;
    }

    // **************************** UPDATE ELEMENTS **************************** //

    // Draw robot
    public static void drawRobot(double x, double y, double rotation) {
        fieldGC.save();
        fieldGC.clearRect(0, 0, fieldGC.getCanvas().getWidth(), fieldGC.getCanvas().getHeight());

        double resizeScaler = fieldGC.getCanvas().getWidth() / fieldLength;

        x = x * resizeScaler;
        y = y * resizeScaler;

        double robotWidth = 30.0 * resizeScaler;
        double robotHeight = 30.0 * resizeScaler;

        double halfRobotWidth = robotWidth / 2.0;
        double halfRobotHeight = robotHeight / 2.0;

        try {
            fieldGC.translate(-y + (fieldGC.getCanvas().getWidth() / 2), -x + (fieldGC.getCanvas().getHeight() / 2)); // Switch X and Y because of field orientation
            fieldGC.rotate(rotation);

            fieldGC.setFill(Color.RED);
            fieldGC.setStroke(Color.RED);

            fieldGC.fillPolygon(new double[]{-halfRobotWidth, halfRobotWidth, 0}, new double[]{-halfRobotHeight, -halfRobotHeight, halfRobotHeight}, 3);
            fieldGC.strokePolygon(new double[]{-halfRobotWidth, halfRobotWidth, halfRobotWidth, -halfRobotWidth}, new double[]{-halfRobotHeight, -halfRobotHeight, halfRobotHeight, halfRobotHeight}, 4);
        } finally {
            fieldGC.restore();
        }
    }

    public static void updateStatusBar(boolean isConnected) {
        if (robotStatus != null) {
            robotStatus.setText("Robot: " + (isConnected ? "Connected" : "Disconnected"));
        }
    }

    public static String updateAutoSelector(List<String> autoSelectionOptions) {

        if (!autoSelector.getItems().equals(autoSelectionOptions)) {
            autoSelector.getItems().clear();
            autoSelector.getItems().addAll(autoSelectionOptions);
            autoSelector.getSelectionModel().selectFirst();
        }
        return autoSelector.getValue();
    }

    public static void organizeNTTreeData(TreeTableView<NTDataModel> treeTableView, Map<String, Object> masterTable) {

        TreeItem<NTDataModel> rootItem = treeTableView.getRoot();

        for (Map.Entry<String, Object> entry : masterTable.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String valueString = decodeValue(value);

            if (treeItemMap.containsKey(key)) {
                TreeItem<NTDataModel> item = treeItemMap.get(key);
                item.getValue().valueProperty().set(valueString);
            } else {
                createNestedTreeItems(rootItem, key, valueString);
            }
        }

        // treeItemMap.keySet().removeIf(key -> {
        //     if (!masterTable.containsKey(key)) {
        //         TreeItem<NTDataModel> itemToRemove = treeItemMap.get(key);
        //         rootItem.getChildren().remove(itemToRemove);
        //         return true;
        //     }
        //     return false;
        // });
    }

    // Tree Item class
    public static class NTDataModel {
        private final StringProperty key;
        private final StringProperty value;

        public NTDataModel(String key, String value) {
            this.key = new SimpleStringProperty(key);
            this.value = new SimpleStringProperty(value);
        }

        public StringProperty keyProperty() {
            return key;
        }

        public StringProperty valueProperty() {
            return value;
        }
    }

    private static void createNestedTreeItems(TreeItem<NTDataModel> rootItem, String key, String value) {

        String trimmedKey = key.replaceFirst("^/", "");
        String[] keyArray = trimmedKey.split("/", 2);

        String[] filteredKeyArray = Arrays.stream(keyArray)
                                                .filter(s -> !s.isEmpty())
                                                .toArray(String[]::new);
        
        if (filteredKeyArray.length == 2) {
            String newEntryName = filteredKeyArray[0];
            String newKey = filteredKeyArray[1];
            TreeItem<NTDataModel> item;
            
            int indexOfNewKey = key.indexOf(newKey);
            String currentPath = indexOfNewKey > 0 ? key.substring(0, indexOfNewKey - 1) : key;

            // Check if this path segment already exists
            if (treeItemMap.containsKey(currentPath)) {
                item = treeItemMap.get(currentPath);
            } else {
                item = new TreeItem<>(new NTDataModel(newEntryName, "..."));
                treeItemMap.put(currentPath, item);
                rootItem.getChildren().add(item);
            }
            createNestedTreeItems(item, newKey, value);
            


            // if (treeItemMap.containsKey(newEntryName)) {
            //     item = treeItemMap.get(newEntryName);
            // } else {
            //     item = new TreeItem<>(new NTDataModel(newEntryName, "..."));
            //     treeItemMap.put(newEntryName, item);

            //      if (!rootItem.getChildren().contains(item)) {
            //         rootItem.getChildren().add(item);
            //      }
            // }

            // createNestedTreeItems(item, newKey, value);

        } else if (filteredKeyArray.length == 1) {
            String entryName = filteredKeyArray[0];

            // For leaf nodes, use full key for map
            if (treeItemMap.containsKey(key)) {
                TreeItem<NTDataModel> existingItem = treeItemMap.get(key);
                existingItem.getValue().valueProperty().set(value);
            } else {
                TreeItem<NTDataModel> newItem = new TreeItem<>(new NTDataModel(entryName, value));
                treeItemMap.put(key, newItem);
                rootItem.getChildren().add(newItem);
            }
            
            // if (treeItemMap.containsKey(entryName)) {
            //     TreeItem<NTDataModel> existingItem = treeItemMap.get(key);
            //     existingItem.getValue().valueProperty().set(value);
            // } else {
            //     TreeItem<NTDataModel> newItem = new TreeItem<>(new NTDataModel(entryName, value));
            //     treeItemMap.put(key, newItem);
            //     rootItem.getChildren().add(newItem);
            // }
        }
    }

    // Helper method to decode entries
    private static String decodeValue(Object value) {
        String valueString;
        if (value instanceof String[]) {
            valueString = Arrays.toString((String[]) value);
        } else if (value instanceof double[]) {
            valueString = Arrays.toString((double[]) value);
        } else if (value instanceof boolean[]) {
            valueString = Arrays.toString((boolean[]) value);
        } else if (value instanceof List) {
            valueString = value.toString();
        } else if (value != null) {
            valueString = value.toString();
        } else {
            valueString = "null";
        }
        return valueString;
    }

    // Create graph visualization
    public static void storeValues() {}

}
