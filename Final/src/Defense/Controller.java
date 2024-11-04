package Defense;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class Controller {

	@FXML private Label label;
    @FXML private MenuItem about;
    @FXML private TableView<Device> deviceTable;
    @FXML private DeviceTableController deviceTableController;
    @FXML private TableColumn<Device, String> deviceIdColumn;
    @FXML private TableColumn<Device, String> deviceNameColumn;
    @FXML private TableColumn<Device, String> deviceStatusColumn;
    @FXML private TableColumn<Device, String> deviceParametersColumn;
    @FXML private TableColumn<Device, String> currentParametersColumn;

    @FXML private TableView<Threat> priorityQueueTable;
    
    @FXML private LineChart<?,?> realTimeChart;
    
    @FXML void aboutPage(ActionEvent event) {
    	String javaVersion = System.getProperty("java.version");
    	String javafxVersion = System.getProperty("javafx.version");
    	label.setText("RealTime System Defense " + javafxVersion + "\nRunning on Java " + javaVersion + ".");
    }
    public void initialize() {
    	deviceIdColumn.setCellValueFactory(new PropertyValueFactory<>("deviceId"));
    	deviceNameColumn.setCellValueFactory(new PropertyValueFactory<>("deviceName"));
        deviceStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        deviceParametersColumn.setCellValueFactory(new PropertyValueFactory<>("normalParameters"));
        deviceParametersColumn.setCellValueFactory(new PropertyValueFactory<>("currentParameters"));
    	deviceTableController = new DeviceTableController(deviceTable);
        deviceTableController.setupDeviceTable();
        setupRealTimeChart();
        setupPriorityQueueTable();
    }	
    
    private void setupRealTimeChart() {
    	
    }
    
    private void setupPriorityQueueTable() {
    	
    }
    
    @FXML
    private void handleStartMonitoring() {
        // Logic to start monitoring
    }

    @FXML
    private void handleStopMonitoring() {
        // Logic to stop monitoring
    }

    @FXML
    private void handleResetSystem() {
        // Logic to reset the system and clear data
    }
}