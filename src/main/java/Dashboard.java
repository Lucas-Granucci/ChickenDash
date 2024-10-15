import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Dashboard extends Application{

    private ScheduledExecutorService scheduler;
    
    @Override
    public void start(Stage primaryStage) {
        // Top bar: Status indicators
        HBox topBar = UIController.createTopBar();

        // Field view with Contol panel
        VBox leftPanel = new VBox(10);
        leftPanel.getChildren().addAll(UIController.createFieldView(), UIController.createControlPanel());
        leftPanel.setPadding(new Insets(10));

        // Network Table viewer
        TreeView<String> networkTableViewer = UIController.createNetworkTableViewer();

        // Swerve module visualization
        GridPane swerveModuleView = UIController.createSwerveModules();

        // Main layout (splitPane)
        SplitPane mainSplitPane = new SplitPane();
        mainSplitPane.getItems().addAll(leftPanel, networkTableViewer);
        mainSplitPane.setDividerPositions(0.7);

        // BorderPane
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(mainSplitPane);
        root.setBottom(swerveModuleView);

        // Scene setup
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("Chicken Dash");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start the scheduled task to update the dashboard every 100ms
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {

            NetworkTableManager.populateMasterTable();

            // // Update dashboard on the JavaFX Application Thread
            Platform.runLater(() -> updateNTTree(networkTableViewer.getRoot()));

            Platform.runLater(() -> updateRobotPos());
            Platform.runLater(() -> updateStatusBar());

            Platform.runLater(() -> updateAutoSelector());

        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void updateNTTree(TreeItem<String> rootItem) {
        UIController.organizeNTTreeData(rootItem, NetworkTableManager.getMasterTable());
    }

    private void updateRobotPos() {
        Object positionObject = NetworkTableManager.getValue("/Shuffleboard/Positions/Swerve Position");
        Object rotationObject = NetworkTableManager.getValue("/Shuffleboard/SmartDashboard/Custom Field/fieldSwerveMod0/fieldSwerveMod0/angle");

        if ((positionObject != "Unassigned value" && positionObject != null) && (rotationObject != "Unassigned value" && rotationObject != null)) {

            String position = (String) positionObject;
            Double rotation = (Double) rotationObject;

            String[] coordinates = position.replaceAll("[()]", "").split(",\\s*");

            double x = Double.parseDouble(coordinates[0]);
            double y = Double.parseDouble(coordinates[1]);

            rotation = -1.0 * (rotation - 90.0);

            UIController.drawRobot(x, y, rotation);
        }
    }

    private void updateStatusBar() {
        boolean isConnected = NetworkTableManager.isConnected();
        UIController.updateStatusBar(isConnected);
    }

    private void updateAutoSelector() {
        Object autoSelectionOptions = NetworkTableManager.getValue("/Shuffleboard/SmartDashboard/Auto Selector/options");

        if (autoSelectionOptions != "Unassigned value" && autoSelectionOptions != null) {
            String[] stringAutoSelectionOptions = (String[]) autoSelectionOptions;
            String selectedItem = UIController.updateAutoSelector(Arrays.asList(stringAutoSelectionOptions));
            NetworkTableManager.publishValue("string", "/Shuffleboard/SmartDashboard/Auto Selector/selected", selectedItem);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() {
        scheduler.shutdown();
    }
}
