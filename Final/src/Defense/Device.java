package Defense;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Device {
	
    private final StringProperty deviceId = new SimpleStringProperty();
    private final StringProperty deviceName = new SimpleStringProperty();
    private final StringProperty ipAddress = new SimpleStringProperty();
    private final ObjectProperty<DeviceStatus> status =  new SimpleObjectProperty<>(DeviceStatus.NORMAL);
    //private final ObjectProperty<Parameters> parameters = new SimpleObjectProperty<>(new Parameters());
    private final ObjectProperty<Parameters> normalParameters;
    private final ObjectProperty<Parameters> currentParameters;
    private final ObservableList<Threat> threatHistory = FXCollections.observableArrayList();
    private final ObservableMap<String, Number> metrics = FXCollections.observableHashMap();
    private final Map<String, ThresholdConfig> thresholds = new HashMap<>();
    private final ThreatManager threatManager;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);  // For updating current parameters
    private final Logger logger = LoggerFactory.getLogger(Device.class);
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    
    public enum DeviceStatus {    // A way to easily control a device's status
    	NORMAL("Normal", "green"), 
    	WARNING("Warning", "orange"), 
    	CRITICAL("Critical", "red"), 
    	OFFLINE("Offline", "gray");
    	
    	private final String display;
        private final String color;
        
        DeviceStatus(String display, String color) {
        	this.display = display;
        	this.color = color;
        }
        public String getDisplay() { return display; }
        public String getColor() { return color; }
    }
    
    public enum DeviceType {  // For determining which parameters to capture
        CPU, MEMORY, DISK, NETWORK, USB, PROCESS;

    	public static DeviceType fromDeviceName(String deviceName) {  // This determines device type by matching common names for devices
    		String name = deviceName.toLowerCase();
    		if (name.contains("cpu")) return CPU;
    		if (name.contains("memory") || name.contains("ram")) return MEMORY;
    		if (name.contains("disk")) return DISK;
    		if (name.contains("net")||name.contains("wifi")||name.contains("wireless")||name.contains("wan")||name
    				.contains("lan")||name.contains("virtual")) return NETWORK;
    		if (name.contains("usb")) return USB;
    		return PROCESS;
    	}
    }
    
    private static class ThresholdConfig {  // This holds double values for the normal operating parameter range for devices.
        double warningLevel;
        double criticalLevel;

        public ThresholdConfig(double warningLevel, double criticalLevel) {
            this.warningLevel = warningLevel;
            this.criticalLevel = criticalLevel;
        }       
        @Override
        public String toString() {
            return String.format("ThresholdConfig{warningLevel=%.2f, criticalLevel=%.2f}", warningLevel, criticalLevel);
        }
    }
    
    public Device(String deviceId, String deviceName, String ipAddress, ThreatManager threatManager) {  // The instantiation of a Device object will follow these steps
        this.deviceId.set(deviceId);
        this.deviceName.set(deviceName);
        this.ipAddress.set(ipAddress);
        this.threatManager = threatManager;
        
        this.normalParameters = new SimpleObjectProperty<>(new Parameters(this));
        this.currentParameters = new SimpleObjectProperty<>(new Parameters(this));
        
        setupParameterListeners();
        startCurrentParametersCapture();
        captureNormalParameters();
        initializeThresholds();
    }
    
    public String getDeviceId() { return deviceId.get(); }
    public void setDeviceId(String deviceId) { this.deviceId.set(deviceId); }
    public StringProperty deviceIdProperty() { return deviceId; }

    public String getDeviceName() { return deviceName.get(); }
    public void setDeviceName(String deviceName) { this.deviceName.set(deviceName); }
    public StringProperty deviceNameProperty() { return deviceName; }

    public String getipAddress() { return ipAddress.get(); }
    public void setIpAddress(String ipAddress) { this.ipAddress.set(ipAddress); }
    public StringProperty ipAddressProperty() { return ipAddress; }
    
    public DeviceStatus getStatus() { return status.get(); }
    public void setStatus(DeviceStatus value) { status.set(value); }
    public ObjectProperty<DeviceStatus> statusProperty() { return status; }
    
    public ObjectProperty<Parameters> normalParametersProperty() { return normalParameters; }
    public ObjectProperty<Parameters> currentParametersProperty() { return currentParameters; }  
    public Parameters getNormalParameters() { return normalParameters.get(); }
    public Parameters getCurrentParameters() { return currentParameters.get(); }
    
    public ObservableList<Threat> getThreatHistory() { return threatHistory; }
    public ThreatManager getThreatManager() { return threatManager; }
    
    private void setupParameterListeners() {  // When a value is changed it will be evaluated by a method communicating with the threatManager directly
    	Parameters current = currentParameters.get();
    	DeviceType deviceType = DeviceType.fromDeviceName(deviceName.get());
        switch (deviceType) {
            case CPU -> current.cpuUsageProperty().addListener((obs, oldVal, newVal) -> evaluateParameter("cpuUsage", newVal.doubleValue()));
            case MEMORY -> current.memoryUsageProperty().addListener((obs, oldVal, newVal) -> evaluateParameter("memoryUsage", newVal.doubleValue()));
            case DISK -> current.diskUsageProperty().addListener((obs, oldVal, newVal) -> evaluateParameter("diskUsage", newVal.doubleValue()));
            case NETWORK -> current.networkTrafficProperty().addListener((obs, oldVal, newVal) -> evaluateParameter("networkTraffic", newVal.doubleValue()));
            case USB -> current.usbDeviceCountProperty().addListener((obs, oldVal, newVal) -> evaluateParameter("usbTraffic", newVal.doubleValue()));
            default -> logger.warn("Unhandled device type: {}", deviceName.get());
        }
    } 
    
    private void setNormalParameters(Parameters params) {
        Platform.runLater(() -> normalParameters.set(params));
        logger.info("Normal parameters set for device: {}", getDeviceName());
    }
    
    private Parameters captureParameters(DeviceType deviceType) {   // Determines device type and runs the relevant parameter capture method on it.
        Parameters params = new Parameters();
        switch (deviceType) {
            case CPU -> params.captureCpuParameters();
            case MEMORY -> params.captureMemoryParameters();
            case DISK -> params.captureDiskParameters();
            case NETWORK -> {
                if (!"N/A".equals(ipAddress.get())) {
                    params.captureNetworkParameters();
                } else {
                    setStatus(DeviceStatus.OFFLINE);
                    return params;
                }
            }
            case USB -> params.captureUsbParameters();
            default -> params.captureProcessParameters();
        }
        //logger.info("Captured parameters for device: {}", params.getParameters());
        return params;
    }
    
    public void captureNormalParameters() {
        DeviceType deviceType = DeviceType.fromDeviceName(deviceName.get());
        executor.submit(() -> 
        setNormalParameters(captureParameters(deviceType)));  
        initializeThresholds();
    }
	
    public void updateCurrentParameters(Parameters params) {  // Will only update an already running device
    	if (params == null || params.getParameters().isEmpty()) {
            return;
        }	
    	Platform.runLater(() -> {
    		synchronized (currentParameters) {
    			currentParameters.set(params);
    		}
    		evaluateAllParameters();});  		
    }
    
    private void startCurrentParametersCapture() {
        scheduler.scheduleAtFixedRate(this::captureCurrentParameters, 0, 500, TimeUnit.MILLISECONDS);
    }

    public void captureCurrentParameters() {
        DeviceType deviceType = DeviceType.fromDeviceName(deviceName.get());
        Parameters params = captureParameters(deviceType);
        updateCurrentParameters(params);
    }
    
   /* private void updateMetrics(Parameters params) {
    	Platform.runLater(() -> {
        metrics.clear();
        logger.debug("Updating Metrics from parameters: {}", params);
        for (Method method : Parameters.class.getDeclaredMethods()) {
            if (method.getName().startsWith("get")) {
                String metricName = StringUtils.uncapitalize(method.getName().substring(3));
                try {
                    Object value = method.invoke(params);
                    if (value instanceof Number) {
                        metrics.put(metricName, ((Number) value).doubleValue());
                        logger.debug("Updated metric {} with value {}", metricName, value);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    logger.error("Error updating metric " + metricName, e);
                }
            }
        }   	
        logger.info("Updated metrics: {}", metrics);
		});
	}*/
    
    private void initializeThresholds() {  
        Parameters normalParams = normalParameters.get();
		if (normalParams == null) {
			logger.warn("Normal parameters not set for device: {}", getDeviceName());
			return;
		}
		
        thresholds.put("cpuUsage", createThresholdConfig(normalParams.getCpuUsage(), "CPU Usage"));
        thresholds.put("memoryUsage", createThresholdConfig(normalParams.getMemoryUsage(), "Memory Usage"));
        thresholds.put("networkTraffic", createThresholdConfig(normalParams.getNetworkTraffic(), "Network Traffic"));
        thresholds.put("diskUsage", createThresholdConfig(normalParams.getDiskUsage(), "Disk Usage"));
        thresholds.put("processCount", createThresholdConfig(normalParams.getProcessCount(), "Process Count"));
        logger.info("Initialized thresholds for device '{}': {}", getDeviceName(), thresholds);
    }
    
    private ThresholdConfig createThresholdConfig(double baseValue, String parameterName) {
        if (baseValue <= 0 || Double.isNaN(baseValue)) {
            logger.debug("Base value for '{}' is invalid ({}). Using default thresholds.", parameterName, baseValue);
            return new ThresholdConfig(0, 0); // Default values
        }

        double warningLevel = baseValue * 3; // Cranked up from 120% to reduce threats
        double criticalLevel = baseValue * 6; // 600%
        return new ThresholdConfig(warningLevel, criticalLevel);
    }
    
    private void evaluateAllParameters() {
    	Parameters current = currentParameters.get();
    	boolean hasValidMetrics = false;
    	
    	if (current.getParameters().isEmpty()) {
            setStatus(DeviceStatus.OFFLINE);
            logger.info("Current parameters are null for device '{}'. Setting status to OFFLINE.", getDeviceName());
            return;
        }
    	
    	for (String metric : thresholds.keySet()) {   // Measures all thresholds, if all have 0 values device is probably offline
            double currentValue = getMetricValue(current, metric);
            if (currentValue > 0.0) {
            	hasValidMetrics = true;
            	evaluateParameter(metric, currentValue);
            }
    	}
    	
    	if (!hasValidMetrics) {
    		setStatus(DeviceStatus.OFFLINE);
    		return;
    	}
    	updateDeviceStatus();
    }
    
    private double getMetricValue(Parameters current, String metric) {  // Metrics are deprecated
        return switch (metric) {
            case "cpuUsage" -> current.getCpuUsage();
            case "memoryUsage" -> current.getMemoryUsage();
            case "diskUsage" -> current.getDiskUsage();
            case "networkTraffic" -> current.getNetworkTraffic();
            case "usbDeviceCount" -> current.getUsbDeviceCount();
            default -> 0;
        };
    }

    
    private void evaluateParameter(String paramName, double currentValue) { // Threat for specific metric
    	ThresholdConfig threshold = thresholds.get(paramName);
    	if (currentValue >= threshold.criticalLevel) {
    		generateThreat(paramName, currentValue, Threat.ThreatSeverity.CRITICAL);
    	} else if (currentValue >= threshold.warningLevel) {
            generateThreat(paramName, currentValue, Threat.ThreatSeverity.WARNING);
    	}   
    }
    
    private void generateThreat(String paramName, double value, Threat.ThreatSeverity severity) { // Threat generator
    	Threat threat = new Threat(
    		UUID.randomUUID().toString(),
    		getDeviceId(),
    		Threat.ThreatType.UNUSUAL_BEHAVIOR,
    		severity);
    	
    	String description = String.format("%s threshold exceeded: Current value: %.2f",
    			paramName, value);
    	threat.setDescription(description);
    	threatManager.addThreat(threat);
    	logger.info("Generated threat: {}", threat.getId());
    }
    
    private void updateDeviceStatus() {
        List<Threat> activeThreats = threatManager.getActiveThreatsForDevice(getDeviceId());
        
        boolean hasCritical = activeThreats.stream()
            .anyMatch(t -> t.getSeverity() == Threat.ThreatSeverity.CRITICAL);
        boolean hasWarning = activeThreats.stream()
            .anyMatch(t -> t.getSeverity() == Threat.ThreatSeverity.WARNING);

        if (hasCritical) {
            setStatus(DeviceStatus.CRITICAL);
        } else if (hasWarning) {
            setStatus(DeviceStatus.WARNING);
        } else {
            setStatus(DeviceStatus.NORMAL);
        }
    }
    
    public void setThreshold(String paramName, double warningLevel, double criticalLevel) {
        thresholds.put(paramName, new ThresholdConfig(warningLevel, criticalLevel));
    }
    
    public void setBaselineParameters(Parameters params) { // Unused
        setNormalParameters(params);
        logger.debug("Baseline parameters set for device: {}", getDeviceName());
        evaluateAllParameters();
    }
    
    public boolean isAnomalous() { // Unused
        // TODO Add logic to compare current and normal parameters
        return !currentParameters.get().equals(normalParameters.get());
    }
    
    public synchronized Map<String, Number> getMetrics() {
        return metrics;
    }
    
    @Override
    public String toString() {
        return "Device{" +
                "deviceId=" + deviceId.get() +
                ", deviceName=" + deviceName.get() +
                ", status=" + status.get() +
                ", normalParameters=" + normalParameters.get() +
                ", currentParameters=" + currentParameters.get() +
                '}';
    }
    
    public String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

	public boolean isOnline() {
		return this.getStatus() != DeviceStatus.OFFLINE;
	}
}
