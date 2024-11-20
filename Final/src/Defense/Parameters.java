package Defense;

import javafx.beans.property.*;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.OperatingSystem;
import java.util.concurrent.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Parameters {
	private final DoubleProperty cpuUsage = new SimpleDoubleProperty();
	private final DoubleProperty memoryUsage = new SimpleDoubleProperty();
	private final DoubleProperty diskUsage = new SimpleDoubleProperty();
	private final DoubleProperty networkTraffic = new SimpleDoubleProperty();
	private final IntegerProperty usbDeviceCount = new SimpleIntegerProperty();
	private final IntegerProperty processCount = new SimpleIntegerProperty();
	private final IntegerProperty threadCount = new SimpleIntegerProperty();
	private final IntegerProperty openPortCount = new SimpleIntegerProperty();
	private final IntegerProperty listeningPortCount = new SimpleIntegerProperty();
	private final IntegerProperty establishedConnectionCount = new SimpleIntegerProperty();
	private final SystemInfo systemInfo = new SystemInfo();
	private final HardwareAbstractionLayer hal = systemInfo.getHardware();
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors());
    private static final Logger logger = LoggerFactory.getLogger(Parameters.class);
	
	public Parameters() {
		
	}
	
	public double getCpuUsage() { return cpuUsage.get(); }
	public void setCpuUsage(double cpuUsage) { this.cpuUsage.set(cpuUsage); }
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
		
	public void captureCpuParameters() {
        executor.submit(() -> {
           try { CentralProcessor processor = hal.getProcessor();
            double cpuLoad = processor.getSystemCpuLoad(250);
            setCpuUsage(cpuLoad);
            logger.debug("cpuLoad = {}", cpuLoad);
           } catch (Exception e) {
        	logger.error("Failed to capture CPU parameters: {}", e.getMessage(), e);
           }
        });
	}
    
	
	public void captureMemoryParameters() {
        executor.submit(() -> {
            GlobalMemory memory = hal.getMemory();
            setMemoryUsage(1.0 - ((double) memory.getAvailable() / (double) memory.getTotal()));
            logger.debug("memoryUsage = {}", memoryUsage);
        });
    }
	
	public void captureDiskParameters() {
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
	public void captureNetworkParameters() {
        executor.submit(() -> {
            List<NetworkIF> networks = hal.getNetworkIFs();
            double totalTraffic = networks.stream().mapToDouble(network -> (double) (network.getBytesRecv() + network.getBytesSent())).sum();
            setNetworkTraffic(totalTraffic);
            logger.debug("networkTraffic = {}", totalTraffic);
        });
    }
	
	public void captureUsbParameters() {
        executor.submit(() -> {
            List<UsbDevice> usbDevices = hal.getUsbDevices(true);
            setUsbDeviceCount(usbDevices.size());
            logger.debug("usbDeviceCount = {}", usbDevices.size());
        });
    }
	
	public void captureProcessParameters() {
        executor.submit(() -> {
            OperatingSystem os = systemInfo.getOperatingSystem();
            setProcessCount(os.getProcessCount());
            logger.debug("processCount = {}", os.getProcessCount());
        });
    }
	
	public synchronized Map<String, Number> getParameters() {
        Map<String, Number> parameters = new HashMap<>();
        if (getCpuUsage() > 0) parameters.put("CPU Usage", getCpuUsage());
        if (getMemoryUsage() > 0) parameters.put("Memory Usage", getMemoryUsage());
        if (getDiskUsage() > 0) parameters.put("Disk Usage", getDiskUsage());
        if (getNetworkTraffic() > 0) parameters.put("Network Traffic", getNetworkTraffic());
        if (getUsbDeviceCount() > 0) parameters.put("USB Device Count", getUsbDeviceCount());
        if (getProcessCount() > 0) parameters.put("Process Count", getProcessCount());
        if (getThreadCount() > 0) parameters.put("Thread Count", getThreadCount());
        if (getOpenPortCount() > 0) parameters.put("Open Port Count", getOpenPortCount());
        if (getListeningPortCount() > 0) parameters.put("Listening Port Count", getListeningPortCount());
        if (getEstablishedConnectionCount() > 0) parameters.put("Established Connection Count", getEstablishedConnectionCount());
        return parameters;
    }

    public void evaluateParameters() {
        Map<String, Number> parameters = getParameters();
        parameters.forEach((key, value) -> {
            // Update current parameters based on the key and value
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
