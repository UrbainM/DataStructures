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

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;


import Defense.Threat.ThreatSeverity;

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
    private final HardwareAbstractionLayer hal;
    private final SystemInfo systemInfo = new SystemInfo();
    
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
        this.hal = systemInfo.getHardware();
        
        System.out.println("Creating Device: " + deviceId + ", " + deviceName + ", " + ipAddress);
        // Initialization code
        initializeThresholds();
        detectDevice();
        setupParameterListeners();
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
    
    // Property
    public ObjectProperty<Parameters> normalParametersProperty() { return normalParameters; }
    public ObjectProperty<Parameters> currentParametersProperty() { return currentParameters; }  
    public Parameters getNormalParameters() { return normalParameters.get(); }
    public Parameters getCurrentParameters() { return currentParameters.get(); }
    
    private void setupParameterListeners() {
    	Parameters current = currentParameters.get();
    	
    	if (deviceName.get().toLowerCase().contains("cpu")) {
            current.cpuUsageProperty().addListener((obs, oldVal, newVal) ->
                evaluateParameter("cpuUsage", newVal.doubleValue()));
        } else if (deviceName.get().toLowerCase().contains("memory")) {
            current.memoryUsageProperty().addListener((obs, oldVal, newVal) ->
                evaluateParameter("memoryUsage", newVal.doubleValue()));
        } else if (deviceName.get().toLowerCase().contains("disk")) {
            current.diskUsageProperty().addListener((obs, oldVal, newVal) ->
                evaluateParameter("diskUsage", newVal.doubleValue()));
        } else if (deviceName.get().toLowerCase().contains("network")) {
            current.networkTrafficProperty().addListener((obs, oldVal, newVal) ->
                evaluateParameter("networkTraffic", newVal.doubleValue()));
        } else if (deviceName.get().toLowerCase().contains("usb")) {
            // Add USB-specific listeners if needed
        } else {
            current.processCountProperty().addListener((obs, oldVal, newVal) ->
                evaluateParameter("processCount", newVal.doubleValue()));
        }
    	// TODO other listeners
    }
    
    
    public void setNormalParameters(Parameters params) {
    	normalParameters.set(params);
    	evaluateAllParameters();
    }
    
    private void detectDevice() {
        System.out.println("Starting device detection for: " + deviceName.get());
        if (deviceName.get().toLowerCase().contains("cpu")) {
            System.out.println("Capturing CPU parameters...");
            currentParameters.get().captureCpuParameters();
        } else if (deviceName.get().toLowerCase().contains("memory")) {
            System.out.println("Capturing Memory parameters...");
            currentParameters.get().captureMemoryParameters();
        } else if (deviceName.get().toLowerCase().contains("disk")) {
            System.out.println("Capturing Disk parameters...");
            currentParameters.get().captureDiskParameters();
        } else if (deviceName.get().toLowerCase().contains("network")) {
            System.out.println("Capturing Network parameters...");
            currentParameters.get().captureNetworkParameters();
        } else if (deviceName.get().toLowerCase().contains("usb")) {
            System.out.println("Capturing USB parameters...");
            currentParameters.get().captureUsbParameters();
        } else {
            System.out.println("Capturing Process parameters...");
            currentParameters.get().captureProcessParameters();
        }
        System.out.println("Device detected: " + deviceName.get());
    }
    
    public void captureNormalParameters() {
    	ExecutorService executor = Executors.newFixedThreadPool(4);
    	Parameters params = new Parameters();
    	
    	Future<Double> cpuFuture = null;
    	Future<Double> memoryFuture = null;
    	Future<Double> diskFuture = null;
    	Future<Double> networkFuture = null;
		
    	if (deviceName.get().toLowerCase().contains("cpu")) {
            cpuFuture = executor.submit(() -> captureCpuUsage());
        }
        if (deviceName.get().toLowerCase().contains("memory")) {
            memoryFuture = executor.submit(() -> captureMemoryUsage());
        }
        if (deviceName.get().toLowerCase().contains("disk")) {
            diskFuture = executor.submit(() -> captureDiskUsage());
        }
        if (deviceName.get().toLowerCase().contains("net")||
        		deviceName.get().toLowerCase().contains("wifi")||
        		deviceName.get().toLowerCase().contains("wireless")||
        	    deviceName.get().toLowerCase().contains("wan")||
        	    deviceName.get().toLowerCase().contains("lan")||
        	    deviceName.get().toLowerCase().contains("virtual")) {
        	if (ipAddress.get().contains("N/A")) {
    			System.out.println("Device is offline.");
    			setStatus(DeviceStatus.OFFLINE);
    			return;
    		}
            networkFuture = executor.submit(() -> captureNetworkTraffic());
        }

        try {
            if (cpuFuture != null) {
                params.setCpuUsage(cpuFuture.get());
            }
            if (memoryFuture != null) {
                params.setMemoryUsage(memoryFuture.get());
            }
            if (diskFuture != null) {
                params.setDiskUsage(diskFuture.get());
            }
            if (networkFuture != null) {
                params.setNetworkTraffic(networkFuture.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    	setBaselineParameters(params);
    	updateMetrics(params);
    	System.out.println("Parameters captured Successfully.");
    }
    
    private double captureCpuUsage() {
    	double min = Double.MAX_VALUE;
    	double max = Double.MIN_VALUE;
    	for (int i = 0; i < 10; i++) {
			double usage = hal.getProcessor().getSystemCpuLoad(250000);
			min = Math.min(min, usage);
			max = Math.max(max, usage);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    	System.out.println("CPU Usage - Min: " + min + ", Max: " + max);
    	return (min + max) / 2;
    }
    
    private double captureNetworkTraffic() {
    	System.out.println("Capturing Network Traffic...");
		List<NetworkIF> networks = hal.getNetworkIFs();
		ExecutorService executor = Executors.newFixedThreadPool(networks.size());
		List<Future<Double>> futures = new ArrayList<>();
		for (NetworkIF network : networks) {
	        futures.add(executor.submit(() -> {
	            double min = Double.MAX_VALUE;
	            double max = Double.MIN_VALUE;
	            for (int i = 0; i < 10; i++) {
	                double traffic = network.getSpeed();
	                min = Math.min(min, traffic);
	                max = Math.max(max, traffic);
	                try {
	                    Thread.sleep(1000);
	                } catch (InterruptedException e) {
	                    Thread.currentThread().interrupt();
	                    return 0.0;
	                }
	            }
	            return (min + max) / 2;
	        }));
	    }

        double totalTraffic = 0;
		for (Future<Double> future : futures) {
			try {
				totalTraffic += future.get(1, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				totalTraffic += 0;
			}
		}
		
		executor.shutdown();
	    try {
	        if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
	            executor.shutdownNow();
	        }
	    } catch (InterruptedException e) {
	        executor.shutdownNow();
	    }
		System.out.println("Network Traffic: " + totalTraffic);
		return totalTraffic;
    }
    
	private double captureMemoryUsage() {
		GlobalMemory memory = hal.getMemory();
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		for (int i = 0; i < 10; i++) {
			double usage = 1.0 - (memory.getAvailable() / (double) memory.getTotal());
			min = Math.min(min, usage);
			max = Math.max(max, usage);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Memory Usage - Min: " + min + ", Max: " + max);
		return (min + max) / 2;
	}
    
	private double captureDiskUsage() {
		List<HWDiskStore> disks = hal.getDiskStores();
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		for (int i = 0; i < 10; i++) {
			double usage = disks.stream().mapToDouble(HWDiskStore::getReadBytes).sum()
					/ disks.stream().mapToDouble(HWDiskStore::getSize).sum();
			min = Math.min(min, usage);
			max = Math.max(max, usage);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Disk Usage - Min: " + min + ", Max: " + max);
		return (min + max) / 2;
	}
	
    public void updateCurrentParameters(Parameters params) {
    	Platform.runLater(() -> {
    		currentParameters.set(params);
    		evaluateAllParameters();});
    }
    
    private void updateMetrics(Parameters params) {
        metrics.clear();
        metrics.put("cpuUsage", params.getCpuUsage());
        metrics.put("memoryUsage", params.getMemoryUsage());
        metrics.put("networkTraffic", params.getNetworkTraffic());
        metrics.put("diskUsage", params.getDiskUsage());
        metrics.put("processCount", (double) params.getProcessCount());
        
        for (Method method : Parameters.class.getDeclaredMethods()) {
            if (method.getName().startsWith("get")) {
                String metricName = StringUtils.uncapitalize(method.getName().substring(3));
				try {
					Object value = method.invoke(params);
					if (value instanceof Number) {
						metrics.put(metricName, ((Number) value).doubleValue());
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
            }
        }
        System.out.println("Metrics updated: " + metrics);
    }
    
    private void initializeThresholds() {
    	thresholds.put("cpuUsage", new ThresholdConfig(70.0, 90.0));
    	System.out.println("Thresholds initialized");
    }
    
    private void evaluateAllParameters() {
    	Parameters normal = normalParameters.get();
    	Parameters current = currentParameters.get();
    	updateMetrics(current);
    	
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
    	System.out.println("Parameter evaluated: " + paramName + ", " + currentValue);   
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
        System.out.println("Baseline parameters set: " + params);
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
