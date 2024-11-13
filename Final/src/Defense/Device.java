package Defense;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Device {
	
	private final StringProperty deviceId = new SimpleStringProperty();
    private final StringProperty deviceName = new SimpleStringProperty();
    private final StringProperty ipAddress = new SimpleStringProperty();
    private final ObjectProperty<DeviceStatus> status =  new SimpleObjectProperty<>(DeviceStatus.NORMAL);
    //private final ObjectProperty<Parameters> parameters = new SimpleObjectProperty<>(new Parameters());
    private final ObjectProperty<Parameters> normalParameters = new SimpleObjectProperty<>(new Parameters());
    private final ObjectProperty<Parameters> currentParameters = new SimpleObjectProperty<>(new Parameters());
    private final ObservableList<Threat> threatHistory = FXCollections.observableArrayList();
    private final ObservableMap<String, Double> metrics = FXCollections.observableHashMap();
    private final Map<String, ThresholdConfig> thresholds = new HashMap<>();
    private final ThreatManager threatManager;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);  // For updating current parameters
    private final Logger logger = LoggerFactory.getLogger(Device.class);
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    
    public enum DeviceStatus {
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
    
    private enum DeviceType {
        CPU, MEMORY, DISK, NETWORK, USB, PROCESS;

    	public static DeviceType fromDeviceName(String deviceName) {
    		String name = deviceName.toLowerCase();
    		if (name.contains("cpu")) return CPU;
    		if (name.contains("memory")) return MEMORY;
    		if (name.contains("disk")) return DISK;
    		if (name.contains("net")||name.contains("wifi")||name.contains("wireless")||name.contains("wan")||name
    				.contains("lan")||name.contains("virtual")) return NETWORK;
    		if (name.contains("usb")) return USB;
    		return PROCESS;
    	}
    }
    
    private static class ThresholdConfig {
        double warningLevel;
        double criticalLevel;

        public ThresholdConfig(double warningLevel, double criticalLevel) {
            this.warningLevel = warningLevel;
            this.criticalLevel = criticalLevel;
        }
    }
    
    public Device(String deviceId, String deviceName, String ipAddress, ThreatManager threatManager) {
        this.deviceId.set(deviceId);
        this.deviceName.set(deviceName);
        this.ipAddress.set(ipAddress);
        this.threatManager = threatManager;
        this.status.set(DeviceStatus.OFFLINE);
        
        System.out.println("Creating Device: " + deviceName);
        
        captureNormalParameters();
        initializeThresholds(); // TODO FIX
        setupParameterListeners();
        startCurrentParametersCapture();
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
    
    private void setupParameterListeners() {
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
    
    public void setNormalParameters(Parameters params) {
    	normalParameters.set(params);
    	evaluateAllParameters();
    }
    
    public void captureNormalParameters() {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        Parameters params = new Parameters();
        DeviceType deviceType = DeviceType.fromDeviceName(deviceName.get());

        List<Future<?>> futures = new ArrayList<>();
        switch (deviceType) {
            case CPU -> futures.add(executor.submit(params::captureCpuParameters));
            case MEMORY -> futures.add(executor.submit(params::captureMemoryParameters));
            case DISK -> futures.add(executor.submit(params::captureDiskParameters));
            case NETWORK -> {
                if (!"N/A".equals(ipAddress.get())) {
                    futures.add(executor.submit(params::captureNetworkParameters));
                } else {
                    setStatus(DeviceStatus.OFFLINE);
                    executor.shutdownNow();
                    return;
                }
            }
            case USB -> futures.add(executor.submit(params::captureUsbParameters));
            default -> logger.warn("Device type not recognized: {}", deviceName.get());
        }

        try {
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }

        setNormalParameters(params);
    }
	
    public void updateCurrentParameters(Parameters params) {
    	Platform.runLater(() -> {
    		currentParameters.set(params);
    		evaluateAllParameters();
    		updateMetrics(params);});  		
    }
    
    private void startCurrentParametersCapture() {
        Runnable captureTask = this::captureCurrentParameters;
        scheduler.scheduleAtFixedRate(captureTask, 0, 500, TimeUnit.MILLISECONDS);
    }

    private void captureCurrentParameters() {
        Parameters params = new Parameters();
        DeviceType deviceType = DeviceType.fromDeviceName(deviceName.get());

        switch (deviceType) {
            case CPU -> params.captureCpuParameters();
            case MEMORY -> params.captureMemoryParameters();
            case DISK -> params.captureDiskParameters();
            case NETWORK -> {
                if (!"N/A".equals(ipAddress.get())) {
                    params.captureNetworkParameters();
                }
            }
            case USB -> params.captureUsbParameters();
            default -> params.captureProcessParameters();
        }
        updateCurrentParameters(params);
    }
    
    private void updateMetrics(Parameters params) {
        metrics.clear();
        for (Method method : Parameters.class.getDeclaredMethods()) {
            if (method.getName().startsWith("get")) {
                String metricName = StringUtils.uncapitalize(method.getName().substring(3));
                try {
                    Object value = method.invoke(params);
                    if (value instanceof Number) {
                        metrics.put(metricName, ((Number) value).doubleValue());
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    logger.error("Error updating metric " + metricName, e);
                }
            }
        }
    }
    
    private void initializeThresholds() {
        Parameters normalParams = getNormalParameters();
        thresholds.put("cpuUsage", new ThresholdConfig(
            normalParams.getCpuUsage() * 1.1, normalParams.getCpuUsage() * 1.2)); // 10% for warning, 20% for critical
        thresholds.put("memoryUsage", new ThresholdConfig(
            normalParams.getMemoryUsage() * 1.1, normalParams.getMemoryUsage() * 1.2)); 
        thresholds.put("networkTraffic", new ThresholdConfig(
            normalParams.getNetworkTraffic() * 1.1, normalParams.getNetworkTraffic() * 1.2)); 
        thresholds.put("diskUsage", new ThresholdConfig(
            normalParams.getDiskUsage() * 1.1, normalParams.getDiskUsage() * 1.2)); 
        thresholds.put("processCount", new ThresholdConfig(
            normalParams.getProcessCount() * 1.1, normalParams.getProcessCount() * 1.2)); 
        logger.info("Thresholds initialized based on normal parameters");
    }
    
    private void evaluateAllParameters() {
    	Parameters current = currentParameters.get();
    	DeviceStatus overallStatus = DeviceStatus.NORMAL;
    	
    	for (String metric : thresholds.keySet()) {
            double currentValue;
            switch (metric) {
                case "cpuUsage" -> currentValue = current.getCpuUsage();
                case "memoryUsage" -> currentValue = current.getMemoryUsage();
                case "diskUsage" -> currentValue = current.getDiskUsage();
                case "networkTraffic" -> currentValue = current.getNetworkTraffic();
                case "usbDeviceCount" -> currentValue = current.getUsbDeviceCount();
                default -> { continue; }
            }
			evaluateParameter(metric, currentValue);
    	}
    	if (overallStatus != status.get()) {
            setStatus(overallStatus);
        }
    }
    
    private void evaluateParameter(String paramName, double currentValue) {
    	ThresholdConfig threshold = thresholds.get(paramName);
    	if (threshold == null || currentValue == 0) {this.setStatus(DeviceStatus.OFFLINE); return;}
    	if (currentValue >= threshold.criticalLevel) {
    		generateThreat(paramName, currentValue, Threat.ThreatSeverity.CRITICAL);
    	} else if (currentValue >= threshold.warningLevel) {
            generateThreat(paramName, currentValue, Threat.ThreatSeverity.WARNING);
    	}   
    }
    
    private void generateThreat(String paramName, double value, Threat.ThreatSeverity severity) {
    	Threat threat = new Threat(
    		UUID.randomUUID().toString(),
    		getDeviceId(),
    		Threat.ThreatType.UNUSUAL_BEHAVIOR,
    		severity);
    	
    	String description = String.format("%s threshold exceeded: Current value: %.2f",
    			paramName, value);
    	threat.setDescription(description);
    	threatManager.addThreat(threat);
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
    
    public void setBaselineParameters(Parameters params) {
        normalParameters.set(params);
        System.out.println("Baseline parameters set: " + params.getParameters().toString());
        evaluateAllParameters();
    }
    
    public boolean isAnomalous() {
        // TODO Add logic to compare current and normal parameters
        return !currentParameters.get().equals(normalParameters.get());
    }
    
    public Map<String, Double> getMetrics() {
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
    
    private String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
