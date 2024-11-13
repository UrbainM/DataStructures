package Defense;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Controller {

	private static final Logger logger = LoggerFactory.getLogger(Controller.class);
	
	@FXML private Label lineChartTitle;
	@FXML private Button captureParameterButton;
    @FXML private TableView<Device> deviceTable;
    @FXML private TableView<Threat> priorityQueueTable;
    @FXML private DeviceTableController deviceTableController;
    @FXML private TableColumn<Device, String> deviceIdColumn;
    @FXML private TableColumn<Device, String> deviceNameColumn;
    @FXML private TableColumn<Device, String> deviceStatusColumn;
    @FXML private TableColumn<Device, String> deviceParametersColumn;
    @FXML private TableColumn<Device, String> currentParametersColumn;
    @FXML private TableColumn<Device, String> deviceIpAddressColumn;    
    @FXML private LineChart<String,Number> realTimeChart;
    
    private List<Device> devices;
    private ChartController chartController;
    private ThreatManager threatManager;
    private ExecutorService executorService;
    
    public void initialize() {
    	executorService = Executors.newFixedThreadPool(10);
    	
    	deviceIdColumn.setCellValueFactory(new PropertyValueFactory<>("deviceId"));
    	deviceNameColumn.setCellValueFactory(new PropertyValueFactory<>("deviceName"));
        deviceStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        deviceIpAddressColumn.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        deviceParametersColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNormalParameters().getParameters().toString()));
        currentParametersColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCurrentParameters().getParameters().toString().replace("{", "").replace("}", "")));
        
        setupRealTimeChart();
        deviceTableController = new DeviceTableController(deviceTable);
        deviceTableController.setupDeviceTable();
        chartController = new ChartController(realTimeChart);
        threatManager = new ThreatManager();
        
        setupPriorityQueueTable();        
        
        deviceTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                chartController.updateRealTimeChart(newValue);
                logger.info("Selected device changed to: {}", newValue.getDeviceName());
            }
        });
        
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(.2), event -> {
        	Device selectedDevice = deviceTableController.getSelectedDevice();
        	if (selectedDevice != null) {
        		lineChartTitle.setText(selectedDevice.getDeviceName());
        		chartController.updateRealTimeChart(selectedDevice);
        		logger.debug("Real-time chart updated for device: {}", selectedDevice.getDeviceName());
        	}
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }	
    
    private void setupRealTimeChart() {  // https://docs.oracle.com/javafx/2/charts/line-chart.htm
    	CategoryAxis xAxis = new CategoryAxis();
    	NumberAxis yAxis = new NumberAxis(0, 100, 10);
    	realTimeChart = new LineChart<>(xAxis, yAxis);
    	realTimeChart.setTitle("Device Parameters Over Time");
    	realTimeChart.setAnimated(true);
    	xAxis.setLabel("Time");
    	yAxis.setLabel("Parameter (%)");
    	realTimeChart.getXAxis().setAutoRanging(true);
    	realTimeChart.getYAxis().setAutoRanging(false);
    	realTimeChart.getYAxis().setTickLength(10);
    	logger.info("Real-time chart setup completed.");
    }
    
    @FXML
    private void setupPriorityQueueTable() {
        TableColumn<Threat, String> threatDeviceNameColumn = new TableColumn<>("Device");
        threatDeviceNameColumn.setCellValueFactory(new PropertyValueFactory<>("deviceName"));
        TableColumn<Threat, Integer> threatLevelColumn = new TableColumn<>("Threat Level");
        threatLevelColumn.setCellValueFactory(new PropertyValueFactory<>("threatLevel"));

        priorityQueueTable.getColumns().addAll(threatDeviceNameColumn, threatLevelColumn);
        
        threatManager.getThreatHistory().forEach(threat -> priorityQueueTable.getItems().add(threat));
        logger.info("Priority queue table setup with threat data.");
    }
    
    @FXML
    private void handleStartMonitoring() {
    	new Thread(() -> {
    		createDevices();
    		startMonitoring();
    	}).start();
    	logger.info("Monitoring started.");
    }
    
    private void createDevices() {
    	devices = deviceTableController.getDevices();
    	logger.info("Devices created: {}", devices.size());
    }
    
    private void startMonitoring() {
        devices.forEach(device -> { 
        	executorService.submit(() -> {
        	    try {
        	        device.captureNormalParameters();
        	        logger.info("Captured normal parameters for {}", device.getDeviceName());
        	    } catch (Exception e) {
        	        logger.error("Error capturing parameters for {}: {}", device.getDeviceName(), e.getMessage());
        	    }
        	    return null;
        });});
        executorService.shutdown();
    }
    
    private void stopMonitoring() {
    	devices.forEach(device -> threatManager.removeDevice(device));
    	executorService.shutdown();
    	logger.info("Monitoring stopped.");
    }
    
    @FXML
    private void handleStopMonitoring() {
        stopMonitoring();
    }
    
    private void clearData() {
    	deviceTable.getItems().clear();
    	priorityQueueTable.getItems().clear();
    	logger.info("Data cleared.");
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
    	logger.info("System reset.");
    }
}