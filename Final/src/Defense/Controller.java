package Defense;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.LineChart;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.List;

public class Controller {

	@FXML private Label label;
	@FXML private Button captureParameterButton;
    @FXML private MenuItem about;
    @FXML private TableView<Device> deviceTable;
    @FXML private TableView<Threat> priorityQueueTable;
    @FXML private DeviceTableController deviceTableController;
    @FXML private Controller Controller;
    @FXML private TableColumn<Device, String> deviceIdColumn;
    @FXML private TableColumn<Device, String> deviceNameColumn;
    @FXML private TableColumn<Device, String> deviceStatusColumn;
    @FXML private TableColumn<Device, String> deviceParametersColumn;
    @FXML private TableColumn<Device, String> currentParametersColumn;
    @FXML private TableColumn<Device, String> deviceIpAddressColumn;    
    @FXML private LineChart<String,Number> realTimeChart;
    
    private ThreatManager threatManager;
    private List<Device> devices;
    private ChartController chartController;
    
    @FXML void aboutPage(ActionEvent event) {
    	String javaVersion = System.getProperty("java.version");
    	String javafxVersion = System.getProperty("javafx.version");
    	label.setText("RealTime System Defense " + javafxVersion + "\nRunning on Java " + javaVersion + ".");
    }
    
    public void initialize() {
    	
    	deviceIdColumn.setCellValueFactory(new PropertyValueFactory<>("deviceId"));
    	deviceNameColumn.setCellValueFactory(new PropertyValueFactory<>("deviceName"));
        deviceStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        deviceIpAddressColumn.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        deviceParametersColumn.setCellValueFactory(new PropertyValueFactory<>("normalParameters"));
        currentParametersColumn.setCellValueFactory(new PropertyValueFactory<>("currentParameters"));
        
        setupRealTimeChart();
        deviceTableController = new DeviceTableController(deviceTable);
        deviceTableController.setupDeviceTable();
        chartController = new ChartController(realTimeChart);
        
        setupPriorityQueueTable();        
        
        deviceTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                chartController.updateRealTimeChart(newValue);
            }
        });
        
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
        	Device selectedDevice = deviceTable.getSelectionModel().getSelectedItem();
        	if (selectedDevice != null) {
        		chartController.updateRealTimeChart(selectedDevice);
        	}
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }	
    
    private void setupRealTimeChart() {
    	CategoryAxis xAxis = new CategoryAxis();
    	NumberAxis yAxis = new NumberAxis(0, 100, 10);
    	realTimeChart.setTitle("Device Parameters Over Time");
    	xAxis.setLabel("Time");
    	yAxis.setLabel("Parameter (%)");
    	realTimeChart.getXAxis().setAutoRanging(true);
    	realTimeChart.getYAxis().setAutoRanging(false);
    	realTimeChart.getYAxis().setTickLength(10);
    }
    
    private void setupPriorityQueueTable() {
    	
    }
    
    @FXML
    private void handleStartMonitoring() {
    	new Thread(() -> {
    		createDevices();
    		startMonitoring();
    	}).start();
    }
    
    private void createDevices() {
    	devices = deviceTableController.getDevices();
    }
    
    private void startMonitoring() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        devices.forEach(device -> {
            executorService.submit(() -> {
                device.captureNormalParameters();
                System.out.println("Captured normal parameters for " + device.getDeviceName());
            });
        });
        executorService.shutdown();
    }
    
    private void stopMonitoring() {
    	// TODO devices.forEach(device -> threatManager.removeDevice(device));
    }
    
    @FXML
    private void handleStopMonitoring() {
        
    }
    
    private void clearData() {
    	deviceTable.getItems().clear();
    	priorityQueueTable.getItems().clear();
    }

    @FXML
    private void handleResetSystem() {
    	resetSystem();
    }
    
    private void resetSystem() {
    	stopMonitoring();
    	clearData();
    	devices.clear();
    	threatManager = new ThreatManager();
    }
}