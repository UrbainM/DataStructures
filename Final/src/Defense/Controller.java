package Defense;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
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
    @FXML private ChartController chartController;
    @FXML private TableColumn<Device, String> deviceIdColumn;
    @FXML private TableColumn<Device, String> deviceNameColumn;
    @FXML private TableColumn<Device, String> deviceStatusColumn;
    @FXML private TableColumn<Device, String> deviceParametersColumn;
    @FXML private TableColumn<Device, String> currentParametersColumn;
    @FXML private TableColumn<Device, String> deviceIpAddressColumn;    
    @FXML private LineChart<String,Number> realTimeChart;
    @FXML private CategoryAxis timeAxis;
    @FXML private NumberAxis parameterAxis;
    
    private List<Device> devices;
    private ThreatManager threatManager;
    private ExecutorService executorService;
    
    @FXML
    public void initialize() {
    	executorService = Executors.newFixedThreadPool(10);
    	
    	deviceIdColumn.setCellValueFactory(new PropertyValueFactory<>("deviceId"));
    	deviceNameColumn.setCellValueFactory(new PropertyValueFactory<>("deviceName"));
        deviceStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        deviceIpAddressColumn.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        deviceParametersColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNormalParameters().getParameters().toString()));
        currentParametersColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCurrentParameters().getParameters().toString().replace("{", "").replace("}", "")));
        
        /* XYChart.Series<String, Number> series = new XYChart.Series<>(); // necessary?
        realTimeChart.getData().add(series); // ?
        */
        deviceTableController = new DeviceTableController(deviceTable);
        deviceTableController.setupDeviceTable();
        setupRealTimeChart();
        chartController = new ChartController(realTimeChart);
        threatManager = new ThreatManager();
        
        setupPriorityQueueTable();        
        
        deviceTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                chartController.updateRealTimeChart(newValue);
                logger.info("Selected device changed to: {}", newValue.getDeviceName());
                startRealTimeChartUpdater();
            }	
        });              
    }	
    
    private void startRealTimeChartUpdater() {
    	logger.info("Starting real-time chart updater...");
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(.5), event -> {
        	logger.info("Timeline triggered...");
        	Device selectedDevice = deviceTableController.getSelectedDevice();
            if (selectedDevice != null) {
                lineChartTitle.setText(selectedDevice.getDeviceName());
                selectedDevice.captureCurrentParameters();
                chartController.updateRealTimeChart(selectedDevice);
                
                /* double currentValue = selectedDevice.getMetrics().values().stream()
                        .reduce((first, second) -> second)
                        .orElse(0.0); */
                /*
                Number currentValue = selectedDevice != null 
                	    ? selectedDevice.getCurrentParameters().getParameters().values().stream()
                	          .reduce((first, second) -> second).orElse(0.0)
                	    : 0.0;
                logger.debug("Current Value {}", currentValue);
                String timestamp = selectedDevice != null ? selectedDevice.getCurrentTime() : "N/A";
                logger.debug("Current Time {}", timestamp);
                // Add data to the chart
                XYChart.Series<String, Number> series = realTimeChart.getData().get(0); 
                series.getData().add(new XYChart.Data<>(timestamp, currentValue)); 

                if (series.getData().size() > 60) {
                    series.getData().remove(0);
                } */

                logger.debug("Real-time chart updated for device: {}", selectedDevice.getDeviceName());
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
    
    private void setupRealTimeChart() {
        if (realTimeChart == null) {
            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis(0, 100, 10);
            realTimeChart = new LineChart<>(xAxis, yAxis);
            realTimeChart.setTitle("Device Parameters Over Time");
            realTimeChart.setAnimated(true);
            xAxis.setLabel("Time");
            yAxis.setLabel("Parameter (%)");
            realTimeChart.getYAxis().setAutoRanging(true);
            realTimeChart.getXAxis().setAutoRanging(false);
            realTimeChart.getXAxis().setTickLength(10);
            logger.info("Real-time chart setup completed.");
        }
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
        executorService.submit(() -> {
            createDevices();
            startMonitoring();
        });
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
            });
        });
    }
    
    private void stopMonitoring() {
        devices.forEach(device -> threatManager.removeDevice(device));
        shutdownExecutorService();
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
    
    private void shutdownExecutorService() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
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