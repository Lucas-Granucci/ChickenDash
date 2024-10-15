import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class UIController {

    private final static double fieldLength = 708.0;

    private static GraphicsContext fieldGC;

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
    public static TreeView<String> createNetworkTableViewer() {
        TreeItem<String> rootItem = new TreeItem<>("NetworkTables");
        rootItem.setExpanded(true);

        TreeItem<String> exampleItem = new TreeItem<>("ExampleKey: value");
        rootItem.getChildren().add(exampleItem);

        TreeView<String> treeView = new TreeView<>(rootItem);
        treeView.setPrefWidth(300);
        return treeView;
    }

    // Create control panel (model selector, auto selector, run button)
    public static HBox createControlPanel() {
        
        HBox controlPanel = new HBox(20);
        controlPanel.setPadding(new Insets(10));

        // Mode selector
        ComboBox<String> modeSelector = new ComboBox<>();
        modeSelector.getItems().addAll("Disconnected", "Disabled", "Autonomous", "Teleoperated", "Test");
        modeSelector.setValue("Teleop");

        // Auto selector
        autoSelector = new ComboBox<>();
        autoSelector.getItems().addAll("Auto 1", "Auto 2", "Auto 3");
        autoSelector.setValue("Auto 1");

        // Run button
        Button startButton = new Button("Start");
        
        controlPanel.getChildren().addAll(new Label("Mode: "), modeSelector, new Label("Auto Routine: "), autoSelector, startButton);

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

    public static void organizeNTTreeData(TreeItem<String> rootItem, Map<String, Object> masterTable) {
        // Preserve the expanded states using a recursive method
        Map<String, Boolean> expandedStates = new HashMap<>();
        storeExpandedStates(rootItem, expandedStates, "");

        // Clear the current children
        rootItem.getChildren().clear(); 

        // Create primary groups
        TreeItem<String> robotGroup = new TreeItem<>("Robot");
        TreeItem<String> fmsInfoGroup = new TreeItem<>("FMS Info");
        TreeItem<String> pidControllersGroup = new TreeItem<>("PID Controllers");
        TreeItem<String> shooterGroup = new TreeItem<>("Shooter");
        TreeItem<String> climberGroup = new TreeItem<>("Climber");
        TreeItem<String> fieldGroup = new TreeItem<>("Field");
        TreeItem<String> liveWindowGroup = new TreeItem<>("Live Window");
        TreeItem<String> metadataGroup = new TreeItem<>("Shuffleboard Metadata and Recording");
        TreeItem<String> schemasGroup = new TreeItem<>("Schemas");

        // Create subgroups for Robot (Swerve Info)
        TreeItem<String> swerveGroup = new TreeItem<>("Swerve");
        TreeItem<String> swervePositions = new TreeItem<>("Positions");
        TreeItem<String> swerveModules = new TreeItem<>("Modules");
        TreeItem<String> swerveMovementVector = new TreeItem<>("Movement Vector");

        // Create subgroup for Field (Custom Fields)
        TreeItem<String> customFieldGroup = new TreeItem<>("Custom Field");

        // Iterate over masterTable and sort entries into their respective groups
        for (Map.Entry<String, Object> entry : masterTable.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String valueString = decodeValue(value);

            TreeItem<String> itemNode = new TreeItem<>(key + " : " + valueString);

            // Sort entries into respective groups
            if (key.contains("Swerve")) {
                if (key.contains("Position")) {
                    swervePositions.getChildren().add(itemNode);
                } else if (key.contains("Mod")) {
                    swerveModules.getChildren().add(itemNode);
                } else if (key.contains("Movement Vector")) {
                    swerveMovementVector.getChildren().add(itemNode);
                } else {
                    swerveGroup.getChildren().add(itemNode);
                }
            } else if (key.startsWith("/FMSInfo")) {
                fmsInfoGroup.getChildren().add(itemNode);
            } else if (key.contains("PID")) {
                pidControllersGroup.getChildren().add(itemNode);
            } else if (key.contains("Shooter")) {
                shooterGroup.getChildren().add(itemNode);
            } else if (key.contains("Climber")) {
                climberGroup.getChildren().add(itemNode);
            } else if (key.contains("LiveWindow")) {
                liveWindowGroup.getChildren().add(itemNode);
            } else if (key.contains(".metadata") || key.contains(".recording")) {
                metadataGroup.getChildren().add(itemNode);
            } else if (key.contains(".schema")) {
                schemasGroup.getChildren().add(itemNode);
            } else {
                rootItem.getChildren().add(itemNode);
            }
        }

        // Add subgroups to their respective parents
        swerveGroup.getChildren().addAll(Arrays.asList(
            swervePositions, swerveModules, swerveMovementVector
        ));
        fieldGroup.getChildren().add(customFieldGroup);

        // Add primary groups to the root
        rootItem.getChildren().addAll(Arrays.asList(
            robotGroup, fmsInfoGroup, pidControllersGroup,
            shooterGroup, climberGroup, fieldGroup,
            liveWindowGroup, metadataGroup, schemasGroup
        ));

        // Add Swerve to Robot
        robotGroup.getChildren().add(swerveGroup);

        // Restore expanded state for each group after the tree is rebuilt
        restoreExpandedStates(rootItem, expandedStates, "");
    }

    // Recursive method to store the expanded states of all nodes
    private static void storeExpandedStates(TreeItem<String> item, Map<String, Boolean> expandedStates, String path) {
        String currentPath = path.isEmpty() ? item.getValue() : path + "/" + item.getValue();
        expandedStates.put(currentPath, item.isExpanded()); 
        for (TreeItem<String> child : item.getChildren()) {
            storeExpandedStates(child, expandedStates, currentPath); 
        }
    }

    // Recursive method to restore the expanded states of all nodes
    private static void restoreExpandedStates(TreeItem<String> item, Map<String, Boolean> expandedStates, String path) {
        String currentPath = path.isEmpty() ? item.getValue() : path + "/" + item.getValue();
        Boolean expandedState = expandedStates.get(currentPath);
        if (expandedState != null) {
            item.setExpanded(expandedState);
        }
        for (TreeItem<String> child : item.getChildren()) {
            restoreExpandedStates(child, expandedStates, currentPath);
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
            valueString = value.toString();  // Lists have good toString()
        } else if (value != null) {
            valueString = value.toString();  // Handle other types
        } else {
            valueString = "null";
        }
        return valueString;
    }

}
