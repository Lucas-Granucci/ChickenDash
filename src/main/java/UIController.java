import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class UIController {

    private final static double fieldLength = 708.0;

    private static GraphicsContext fieldGC;

    private static Map<String, TreeItem<NTDataModel>> treeItemMap = new HashMap<>();

    // Declare UI elements as instance variables
    private static Label robotStatus;
    private static Button connectButton;
    private static ComboBox<String> autoSelector;
    private static GridPane chartGrid;

    private static TreeTableView<NTDataModel> treeTableView;

    // Declare values to track for graphing
    private static final int MAX_DATA_POINTS = 10;

    private static Map<String, XYChart.Series<String, Number>> trackedValues = new HashMap<>();
    private static Map<String, LineChart<String, Number>> activeCharts = new HashMap<>();

    final static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ss.SSS");

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

        treeTableView = new TreeTableView<>();
        treeTableView.setPrefWidth(350);

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

        // Setup selection context menu
        ContextMenu selectionContextMenu = new ContextMenu();
        
        MenuItem startTrackingItem = new MenuItem("Start Tracking Value");

        MenuItem stopTrackingItem = new MenuItem("Stop Tracking Value");

        selectionContextMenu.getItems().addAll(startTrackingItem, stopTrackingItem);

        // Setup click events
        treeTableView.setRowFactory(tv -> {
            TreeTableRow<NTDataModel> row = new TreeTableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                    NTDataModel selectedItem = row.getItem();
                    String selectedValue = selectedItem.valueProperty().getValue();

                    TreeItem<NTDataModel> treeItem = row.getTreeItem();
                    if (treeItem != null) {
                        String path = getPath(treeItem);

                        if (!selectedValue.equals("...") && canBeDouble(selectedValue)) {

                        startTrackingItem.setOnAction( contextEvent -> {
                            trackedValues.put(path, new XYChart.Series<String, Number>());
                        });

                        stopTrackingItem.setOnAction( contextEvent -> {
                            trackedValues.remove(path);
                        });

                        selectionContextMenu.show(row, event.getScreenX(), event.getScreenY());
                    }
                    }
                }
            });
            return row;
        });
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
    public static GridPane createValueTrackingCharts() {
        chartGrid = new GridPane();
        chartGrid.setHgap(10);
        chartGrid.setVgap(10);
        chartGrid.setPadding(new Insets(10));

        return chartGrid;
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
        }
    }

    // Track all values
    public static void updateTrackedValues() {

        final int COLUMNS = 6;

        int row = 0;
        int col = 0;

        for (Map.Entry<String, XYChart.Series<String, Number>> entry : trackedValues.entrySet()) {

            String topicName = entry.getKey();
            XYChart.Series<String, Number> currentValuesSeries = entry.getValue();

            LineChart<String, Number> lineChart = activeCharts.get(topicName);
            if (lineChart == null) {
                // Create chart
                CategoryAxis xAxis = new CategoryAxis();
                NumberAxis yAxis = new NumberAxis();

                xAxis.setAnimated(false);
                yAxis.setAnimated(false);

                lineChart = new LineChart<>(xAxis, yAxis);
                lineChart.getData().add(currentValuesSeries);

                lineChart.setTitle(topicName);
                lineChart.setStyle("-fx-font-size: " + 6 + "px;");
                lineChart.setCreateSymbols(false);
                lineChart.setAnimated(false);
                lineChart.setMaxHeight(50);

                currentValuesSeries.getNode().setStyle("-fx-stroke: blue; -fx-stroke-width: 2px;");

                // Calculate grid position
                col = activeCharts.size() % COLUMNS;
                row = activeCharts.size() / COLUMNS;

                // Add to display and activeCharts
                chartGrid.add(lineChart, col, row);
                activeCharts.put(topicName, lineChart);
            }

            // Get new NT value and add to series
            Object currentValue = NetworkTableManager.getValue(topicName);

            Date now = new Date();
            currentValuesSeries.getData().add(new XYChart.Data<>(simpleDateFormat.format(now), (double) currentValue));

            // Trim series if it's too long
            if (currentValuesSeries.getData().size() > MAX_DATA_POINTS) {
                currentValuesSeries.getData().remove(0);
            }
            trackedValues.put(topicName, currentValuesSeries);
        }

        activeCharts.keySet().removeIf(topicName -> {
            if (!trackedValues.containsKey(topicName)) {
                LineChart<String, Number> chartToRemove = activeCharts.get(topicName);
                chartGrid.getChildren().remove(chartToRemove);

                // Reposition remaining charts to fill gaps
                int index = 0;
                for (LineChart<String, Number> chart : activeCharts.values()) {
                    if (chart != chartToRemove) {
                        int newCol = index % COLUMNS;
                        int newRow = index / COLUMNS;
                        GridPane.setColumnIndex(chart, newCol);
                        GridPane.setRowIndex(chart, newRow);
                        index++;
                    }
                }
                return true;
            }
            return false;
        });
    }

    // **************************** UTILS **************************** //

    private static String getPath(TreeItem<NTDataModel> item) {
        StringBuilder pathBuilder = new StringBuilder();
        TreeItem<NTDataModel> current = item;

        while (current != null) {
            pathBuilder.insert(0, "/" + current.getValue().keyProperty().getValue());
            current = current.getParent();
        }
        return pathBuilder.length() > 0 ? pathBuilder.substring(1).replace("NetworkTable", "") : "";
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

    // Helper method to determine if value can be a double
    public static boolean canBeDouble(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
