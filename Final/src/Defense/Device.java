package Defense;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import Defense.Threat.ThreatSeverity;

public class Device {
	
	private final StringProperty deviceId = new SimpleStringProperty();
    private final StringProperty deviceName = new SimpleStringProperty();
    private final StringProperty ipAddress = new SimpleStringProperty();
    private final ObjectProperty<DeviceStatus> status =  new SimpleObjectProperty<>(DeviceStatus.NORMAL);
    private final ObjectProperty<Parameters> normalParameters = new SimpleObjectProperty<>(new Parameters());;
    private final ObjectProperty<Parameters> currentParameters = new SimpleObjectProperty<>(new Parameters());;
    private final ObservableList<Threat> threatHistory = FXCollections.observableArrayList();
    private final ObservableMap<String, Double> metrics = FXCollections.observableHashMap();
    private final Map<String, ThresholdConfig> thresholds = new HashMap<>();
    private final ThreatManager threatManager;

    
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
        
        initializeThresholds();
        setupParameterListeners();
    }
    
    public String getDeviceId() { return deviceId.get(); }
    public void setDeviceId(String deviceId) { this.deviceId.set(deviceId); }
    public StringProperty deviceIdProperty() { return deviceId; }

    public String getDeviceName() { return deviceName.get(); }
    public void setDeviceName(String deviceName) { this.deviceName.set(deviceName); }
    public StringProperty deviceNameProperty() { return deviceName; }

    public String getipAddress() { return ipAddress.get(); }
    public void setIpAddress(String status) { this.ipAddress.set(status); }
    public StringProperty ipAddressProperty() { return ipAddress; }
    
    public DeviceStatus getStatus() { return status.get(); }
    public void setStatus(DeviceStatus value) { status.set(value); }
    
    // Property
    public ObjectProperty<Parameters> normalParametersProperty() { return normalParameters; }
    public ObjectProperty<Parameters> currentParametersProperty() { return currentParameters; }  
    public Parameters getNormalParameters() { return normalParameters.get(); }
    public Parameters getCurrentParameters() { return currentParameters.get(); }
    
    private void setupParameterListeners() {
    	Parameters current = currentParameters.get();
    }
    
    public void setNormalParameters(Parameters params) {
    	normalParameters.set(params);
    	evaluateAllParameters();
    }
    
    public void updateCurrentParameters(Parameters params) {
    	Platform.runLater(() -> {
    		currentParameters.set(params);
    		evaluateAllParameters();});
    }
    
    private void initializeThresholds() {
    	thresholds.put("cpuUsage", new ThresholdConfig(70.0, 90.0));
    }
    
    private void evaluateAllParameters() {
    	Parameters normal = normalParameters.get();
    	Parameters current = currentParameters.get();
    	
    	for (String paramName : thresholds.keySet()) {
            double currentValue = metrics.get(paramName);
            evaluateParameter(paramName, currentValue);
        }

        updateDeviceStatus();
    }
    
    private void evaluateParameter(String paramName, double currentValue) {
    	ThresholdConfig threshold = thresholds.get(paramName);
    	if (threshold == null) return;
    	
    	if (currentValue >= threshold.criticalLevel) {
    		generateThreat(paramName, currentValue, Threat.ThreatSeverity.CRITICAL);
    	} else if (currentValue >= threshold.warningLevel) {
            generateThreat(paramName, currentValue, Threat.ThreatSeverity.WARNING);
    	}
    }
    
    private void generateThreat(String paramName, double value, ThreatSeverity severity) {
    	Threat threat = new Threat(
    		UUID.randomUUID().toString(),
    		getId(),
    		Threat.ThreatType.UNUSUAL_BEHAVIOR,
    		severity);
    	
    	String description = String.format("%s threshold exceeded: Current value: %.2f",
    			paramName, value);
    	threat.setDescription(description);
    	
    	threatManager.addThreat(threat);
    }
    
    private void updateDeviceStatus() {
        List<Threat> activeThreats = threatManager.getActiveThreatsForDevice(getId());
        
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
        evaluateAllParameters();
    }
    
    public boolean isAnomalous() {
        // TODO Add logic to compare current and normal parameters
        return !currentParameters.get().equals(normalParameters.get());
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
}
