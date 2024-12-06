package Defense;

import javafx.beans.property.*;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.OperatingSystem;
import java.util.concurrent.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Parameters {
	private final Device device;
	private DoubleProperty cpuUsage = new SimpleDoubleProperty();
	private DoubleProperty memoryUsage = new SimpleDoubleProperty();
	private DoubleProperty diskUsage = new SimpleDoubleProperty();
	private DoubleProperty networkTraffic = new SimpleDoubleProperty();
	private IntegerProperty usbDeviceCount = new SimpleIntegerProperty();
	private IntegerProperty processCount = new SimpleIntegerProperty();
	private IntegerProperty threadCount = new SimpleIntegerProperty();
	private IntegerProperty openPortCount = new SimpleIntegerProperty();
	private IntegerProperty listeningPortCount = new SimpleIntegerProperty();
	private IntegerProperty establishedConnectionCount = new SimpleIntegerProperty();
	private final Map<String, Number> parameters = new HashMap<>();
	private final SystemInfo systemInfo = new SystemInfo();
	private final HardwareAbstractionLayer hal = systemInfo.getHardware();
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors());
    private static final Logger logger = LoggerFactory.getLogger(Parameters.class);
	
	public Parameters() {
		this.device = null; // won't initiate as captures require a non-zero value
	}
	
	public Parameters(Device device) {
		this.device = device;
		captureParameters();  // Immediately populates so other methods depending on non-zeros work
    }
	
	public double getCpuUsage() { return cpuUsage.get(); }
	public void setCpuUsage(double cpuUsage) { this.cpuUsage.set(cpuUsage); logger.debug("cpuUsage = {}", cpuUsage); }
	public DoubleProperty cpuUsageProperty() { return cpuUsage; }
	
	public int getThreadCount() { return threadCount.get(); }
	public void setThreadCount(int threads) { this.threadCount.set(threads); }
	public IntegerProperty threadCountProperty() { return threadCount; }
	
	public double getMemoryUsage() { return memoryUsage.get(); }
	public void setMemoryUsage(double memoryUsage) { this.memoryUsage.set(memoryUsage); }
	public DoubleProperty memoryUsageProperty() { return memoryUsage; }
	
	public void setDiskUsage(double totalUsage) { this.diskUsage.set(totalUsage); }
	public double getDiskUsage() { return diskUsage.get(); }
	public DoubleProperty diskUsageProperty() { return diskUsage; }
	
	public double getNetworkTraffic() { return networkTraffic.get(); }
	public void setNetworkTraffic(double networkTraffic) { this.networkTraffic.set(networkTraffic); }
	public DoubleProperty networkTrafficProperty() { return networkTraffic; }
	
	public int getUsbDeviceCount() { return usbDeviceCount.get(); }
	public void setUsbDeviceCount(int usbDeviceCount) { this.usbDeviceCount.set(usbDeviceCount); }
	public IntegerProperty usbDeviceCountProperty() { return usbDeviceCount; }
	
	public int getProcessCount() { return processCount.get(); }
	public void setProcessCount(int processCount) { this.processCount.set(processCount); }
	public IntegerProperty processCountProperty() { return processCount; }
	
	private void captureParameters() {
	    Device.DeviceType deviceType = Device.DeviceType.fromDeviceName(device.getDeviceName());
	    switch (deviceType) {
	        case CPU -> captureCpuParameters();
	        case MEMORY -> captureMemoryParameters();
	        case DISK -> captureDiskParameters();
	        case NETWORK -> {
	            if (!"N/A".equals(device.getipAddress())) {
	                captureNetworkParameters();
	            } else {
	                device.setStatus(Device.DeviceStatus.OFFLINE);
	            }
	        }
	        case USB -> captureUsbParameters();
	        default -> captureProcessParameters();
	    }
	}
	
	public void captureCpuParameters() {
        executor.submit(() -> {
           try { 
        	   CentralProcessor processor = hal.getProcessor();
        	   double cpuLoad = processor.getSystemCpuLoad(400);
        	   setCpuUsage(cpuLoad);
           } catch (Exception e) {
        	   logger.error("Failed to capture CPU parameters: {}", e.getMessage(), e);
           }
        });
	}
    
	
	public void captureMemoryParameters() {
        executor.submit(() -> {
            GlobalMemory memory = hal.getMemory();
            setMemoryUsage(1.0 - ((double) memory.getAvailable() / (double) memory.getTotal())); // Uses percentage of free memory, could just use in-use memory
        });
    }
	
	public void captureDiskParameters() {  // Captures hard-drive space for all hard-drives, if something is installed should generate Threat
	    executor.submit(() -> {
	        try {
	            List<HWDiskStore> disks = hal.getDiskStores();
	            disks.forEach(disk -> logger.debug("Disk: {} - Size: {}", disk.getName(), disk.getSize()));
	            
	            double totalUsage = disks.stream()
	                .filter(disk -> disk.getSize() > 0) // Only include valid disks
	                .mapToDouble(disk -> (double) (disk.getReadBytes() + disk.getWriteBytes()) / disk.getSize())
	                .average()
	                .orElse(0.0); // Fallback to 0.0 if no disks are found
	            
	            setDiskUsage(totalUsage);
	            logger.debug("diskUsage = {}", totalUsage);
	        } catch (Exception e) {
	            logger.error("Failed to capture disk parameters: {}", e.getMessage(), e);
	        }
	    });
	}
	
	/* public void captureNetworkParameters() {
	    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	    List<NetworkIF> networks = hal.getNetworkIFs();
	    List<Future<Double>> futures = new ArrayList<>();

	    for (NetworkIF network : networks) {
	        futures.add(executor.submit(() -> (double) (network.getBytesRecv() + network.getBytesSent())));
	    }

	    double totalTraffic = 0;
	    for (Future<Double> future : futures) {
	        try {
	            totalTraffic += future.get(250, TimeUnit.MILLISECONDS);
	        } catch (InterruptedException | ExecutionException | TimeoutException e) {
	            e.printStackTrace();
	        }
	    }

	    setNetworkTraffic(totalTraffic);
	    executor.shutdown();
	    try {
	        if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
	            executor.shutdownNow();
	        }
	    } catch (InterruptedException e) {
	        executor.shutdownNow();
	    }
	} */
	public void captureNetworkParameters() {  // Ok, this one is measuring the difference in overall network activity over 400ms for all network devices whether offline-or-not
        executor.submit(() -> {
            List<NetworkIF> networks = hal.getNetworkIFs().stream().filter(network -> network.getBytesRecv() > 0 || network.getBytesSent() > 0).toList();
			double totalTraffic = 0;
			
			for (NetworkIF network : networks) {
                long initialBytesRecv = network.getBytesRecv();
                long initialBytesSent = network.getBytesSent();

                try {
					Thread.sleep(400);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
                
                network.updateAttributes();
                long finalBytesRecv = network.getBytesRecv();
                long finalBytesSent = network.getBytesSent();

                double downloadSpeed = (finalBytesRecv - initialBytesRecv) / 1024.0; // KB/s
                double uploadSpeed = (finalBytesSent - initialBytesSent) / 1024.0; // KB/s

                totalTraffic += downloadSpeed + uploadSpeed;
            }
			
			setNetworkTraffic(totalTraffic);
        });
    }
	
	public void captureUsbParameters() {  // Not sure if this works, haven't plugged in a usb device
        executor.submit(() -> {
            List<UsbDevice> usbDevices = hal.getUsbDevices(true);
            setUsbDeviceCount(usbDevices.size());
            logger.debug("usbDeviceCount = {}", usbDevices.size());
        });
    }
	
	public void captureProcessParameters() { // Number of processes the Operating System has running, the threshold for this should be easier to set independently
        executor.submit(() -> {
            OperatingSystem os = systemInfo.getOperatingSystem();
            setProcessCount(os.getProcessCount());
            logger.debug("processCount = {}", os.getProcessCount());
        });
    }
	
	public synchronized Map<String, Number> getParameters() {    // getParameters returns a Map of non-zero parameters
		parameters.clear();
		if (this.device != null) { captureParameters(); }
        if (getCpuUsage() > 0.0001) parameters.put("CPU Usage", getCpuUsage());
        if (getMemoryUsage() > 0.0001) parameters.put("Memory Usage", getMemoryUsage());
        if (getDiskUsage() > 0.0001) parameters.put("Disk Usage", getDiskUsage());
        if (getNetworkTraffic() > 0.0001) parameters.put("Network Traffic", getNetworkTraffic());
        if (getUsbDeviceCount() > 0) parameters.put("USB Device Count", getUsbDeviceCount());
        if (getProcessCount() > 0) parameters.put("Process Count", getProcessCount());
        if (getThreadCount() > 0) parameters.put("Thread Count", getThreadCount());
        if (getOpenPortCount() > 0) parameters.put("Open Port Count", getOpenPortCount());
        if (getListeningPortCount() > 0) parameters.put("Listening Port Count", getListeningPortCount());
        if (getEstablishedConnectionCount() > 0) parameters.put("Established Connection Count", getEstablishedConnectionCount());
        /*if (!parameters.isEmpty()) {
        	logger.info("Parameters: {}", parameters);
        } else {
        	logger.debug("No relevant parameters to log.");
        } */
        return parameters;
    }
	

    public void evaluateParameters() {  // evaluates all parameters for a device, not just non-zero
        Map<String, Number> parameters = getParameters();
        parameters.forEach((key, value) -> {
            switch (key) {
                case "CPU Usage":
                    setCpuUsage((Double) value);
                    break;
                case "Memory Usage":
                    setMemoryUsage((Double) value);
                    break;
                case "Disk Usage":
                    setDiskUsage((Double) value);
                    break;
                case "Network Traffic":
                    setNetworkTraffic((Double) value);
                    break;
                case "USB Device Count":
                    setUsbDeviceCount((Integer) value);
                    break;
                case "Process Count":
                    setProcessCount((Integer) value);
                    break;
                case "Thread Count":
                    setThreadCount((Integer) value);
                    break;
                case "Open Port Count":
                    setOpenPortCount((Integer) value);
                    break;
                case "Listening Port Count":
                    setListeningPortCount((Integer) value);
                    break;
                case "Established Connection Count":
                    setEstablishedConnectionCount((Integer) value);
                    break;
            }
        });
    }

    public int getOpenPortCount() {
        return openPortCount.get();
    }

    public void setOpenPortCount(int openPortCount) {
        this.openPortCount.set(openPortCount);
    }

    public IntegerProperty openPortCountProperty() {
        return openPortCount;
    }

    public int getListeningPortCount() {
        return listeningPortCount.get();
    }

    public void setListeningPortCount(int listeningPortCount) {
        this.listeningPortCount.set(listeningPortCount);
    }

    public IntegerProperty listeningPortCountProperty() {
        return listeningPortCount;
    }

    public int getEstablishedConnectionCount() {
        return establishedConnectionCount.get();
    }

    public void setEstablishedConnectionCount(int establishedConnectionCount) {
        this.establishedConnectionCount.set(establishedConnectionCount);
    }

    public IntegerProperty establishedConnectionCountProperty() {
        return establishedConnectionCount;
    }
	
	public void shutdown() {
        try {
        	executor.shutdown();
            if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
	
	@Override
    public String toString() {
        return "Parameters{" +
                "cpuUsage=" + getCpuUsage() +
                ", memoryUsage=" + getMemoryUsage() +
                ", networkTraffic=" + getNetworkTraffic() +
                ", diskUsage=" + getDiskUsage() +
                ", processCount=" + getProcessCount() +
                '}';
    }
}
