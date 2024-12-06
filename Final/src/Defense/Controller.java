package Defense;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Controller {

	private static final Logger logger = LoggerFactory.getLogger(Controller.class);	
	@FXML private Label lineChartTitle;
	@FXML private Button captureParameterButton;
	@FXML private Button resetButton;
    @FXML private TableView<Device> deviceTable;
    @FXML private TableView<Threat> priorityQueueTable;
    @FXML private DeviceTableController deviceTableController;
    @FXML private ThreatTableController threatTableController;
    @FXML private TableColumn<Device, String> deviceIdColumn;
    @FXML private TableColumn<Device, String> deviceNameColumn;
    @FXML private TableColumn<Device, String> deviceStatusColumn;
    @FXML private TableColumn<Device, String> deviceParametersColumn;
    @FXML private TableColumn<Device, String> currentParametersColumn;
    @FXML private TableColumn<Device, String> deviceIpAddressColumn;    
    @FXML private LineChart<String,Number> realTimeChart;
    @FXML private CategoryAxis timeAxis;
    @FXML private NumberAxis parameterAxis;
    
    private ChartController chartController;
    private Timeline timeline;
    private ObservableList<Device> devices = FXCollections.observableArrayList();
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
        deviceTableController = new DeviceTableController(deviceTable);
        threatTableController = new ThreatTableController(priorityQueueTable);
        handleStartMonitoring();
        setupRealTimeChart();
        chartController = new ChartController(realTimeChart);
        threatManager = new ThreatManager();
        setupPriorityQueueTable();
        threatManager.addThreatListener(new ThreatManager.ThreatListener() {
        	@Override
        	public void onNewThreat(Threat threat) {
        		threatManager.addThreat(threat);
        		updatePriorityQueueTable();
        	}
        	@Override
            public void onThreatResolved(Threat threat) {
        		threatManager.resolveThreat(threat.getId());
                updatePriorityQueueTable();
            }

            @Override
            public void onCriticalThreat(Threat threat) {
                threatManager.addThreat(threat);
                updatePriorityQueueTable();
            }
        });
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
    	if (timeline != null) {
            timeline.stop();
        }
        timeline = new Timeline(new KeyFrame(Duration.seconds(.25), event -> {	
        	Device selectedDevice = deviceTable.getSelectionModel().getSelectedItem();
            if (selectedDevice != null) {
                lineChartTitle.setText(selectedDevice.getDeviceName());
                chartController.updateRealTimeChart(selectedDevice);
				deviceTable.refresh();
				ObservableList<Threat> activeThreats = selectedDevice.getThreatManager().getThreatHistory();
	            priorityQueueTable.setItems(activeThreats);
	            priorityQueueTable.refresh();
                
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

                //logger.info("Real-time chart updated for device: {}", selectedDevice.getDeviceName());
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
            xAxis.setLabel("Time");
            yAxis.setLabel("Parameter (%)");
            realTimeChart.getYAxis().setAutoRanging(true);
            realTimeChart.setAnimated(true);
            logger.info("Real-time chart setup completed.");
        }
    }
    
    @SuppressWarnings("unchecked")
	@FXML
    private void setupPriorityQueueTable() {
    	TableColumn<Threat, String> threatIdColumn = new TableColumn<>("ID");
    	threatIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Threat, String> threatDeviceNameColumn = new TableColumn<>("Device");
        threatDeviceNameColumn.setCellValueFactory(new PropertyValueFactory<>("deviceId"));
        TableColumn<Threat, Integer> threatLevelColumn = new TableColumn<>("Threat Level");
        threatLevelColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getSeverity().getLevel()).asObject());
        TableColumn<Threat, String> threatTimeColumn = new TableColumn<>("Time");
        threatTimeColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        priorityQueueTable.getColumns().setAll(threatIdColumn, threatDeviceNameColumn, threatLevelColumn, threatTimeColumn);
        priorityQueueTable.setItems(threatManager.getThreatHistory());
        
        logger.info("Priority queue table setup with threat data.");
    }
    
    private void updatePriorityQueueTable() {
    	priorityQueueTable.setItems(threatManager.getThreatHistory());
    	priorityQueueTable.refresh();
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
                    //logger.info("Captured normal parameters for {}", device.getDeviceName());
                } catch (Exception e) {
                    logger.error("Error capturing parameters for {}: {}", device.getDeviceName(), e.getMessage());
                }
            });
        });
    }
    
    private void stopMonitoring() {
        devices.forEach(device -> threatManager.removeDevice(device));
        shutdownExecutorService();
        devices.clear();
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
            Thread.currentThread().interrupt();
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