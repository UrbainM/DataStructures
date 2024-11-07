package Defense;

import javafx.beans.property.*;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.OperatingSystem;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

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
	private final IntegerProperty listeningConnectionCount = new SimpleIntegerProperty();
	private final IntegerProperty establishedConnectionCountLocal = new SimpleIntegerProperty();
	private final IntegerProperty listeningConnectionCountLocal = new SimpleIntegerProperty();
	private final IntegerProperty establishedConnectionCountRemote = new SimpleIntegerProperty();
	private final IntegerProperty listeningConnectionCountRemote = new SimpleIntegerProperty();
	private final SystemInfo systemInfo = new SystemInfo();
	private final HardwareAbstractionLayer hal = systemInfo.getHardware();
	private String parameterName;
	private String parameterValue;
	
	public Parameters() {
		this.parameterName = "Not Set";
		this.parameterValue = "Not Set";
	}
	
	public Parameters(String parameterName, String parameterValue) {
		this.parameterName = parameterName;
		this.parameterValue = parameterValue;
	}
	
	@Override
	public String toString() {
		return parameterName + ": " + parameterValue;
	}
	
	public double getCpuUsage() {
		return cpuUsage.get();
	}
	public void setCpuUsage(double cpuUsage) {
		this.cpuUsage.set(cpuUsage);
	}
	public DoubleProperty cpuUsageProperty() {
		return cpuUsage;
	}
	
	public int getThreadCount() {
		return threadCount.get();
	}
	public void setThreadCount(int threads) {
		this.threadCount.set(threads);
	}
	public IntegerProperty threadCountProperty() {
		return threadCount;
	}
	
	public double getMemoryUsage() {
		return memoryUsage.get();
	}
	public void setMemoryUsage(double memoryUsage) {
		this.memoryUsage.set(memoryUsage);
	}
	public DoubleProperty memoryUsageProperty() {
		return memoryUsage;
	}
	
	public void setDiskUsage(double totalUsage) {
		this.diskUsage.set(totalUsage);
	}
	public double getDiskUsage() {
		return diskUsage.get();
	}
	public DoubleProperty diskUsageProperty() {
		return diskUsage;
	}
	
	public double getNetworkTraffic() {
		return networkTraffic.get();
	}
	public void setNetworkTraffic(double networkTraffic) {
		this.networkTraffic.set(networkTraffic);
	}
	public DoubleProperty networkTrafficProperty() {
		return networkTraffic;
	}
	
	public int getUsbDeviceCount() {
		return usbDeviceCount.get();
	}
	public void setUsbDeviceCount(int usbDeviceCount) {
		this.usbDeviceCount.set(usbDeviceCount);
	}
	public IntegerProperty usbDeviceCountProperty() {
		return usbDeviceCount;
	}
	
	public int getProcessCount() {
		return processCount.get();
	}
	public void setProcessCount(int processCount) {
		this.processCount.set(processCount);
	}
	public IntegerProperty processCountProperty() {
		return processCount;
	}
	
	public void captureCpuParameters() {
		CentralProcessor processor = hal.getProcessor();
		System.out.println("Capturing CPU usage...");
		long[] ticks = processor.getSystemCpuLoadTicks();
		double cpuLoad = processor.getSystemCpuLoadBetweenTicks(ticks);
		System.out.println("CPU usage captured: " + cpuLoad);
		setCpuUsage(cpuLoad);
		setThreadCount(processor.getLogicalProcessorCount());
	}
	
	public void captureMemoryParameters() {
		GlobalMemory memory = hal.getMemory();
		setMemoryUsage(1.0 - (memory.getAvailable() / (double) memory.getTotal()));
	}
	
	public void captureDiskParameters() {
		List<HWDiskStore> disks = hal.getDiskStores();
		double totalUsage = disks.stream().mapToDouble(HWDiskStore::getReadBytes).sum() /
				disks.stream().mapToDouble(HWDiskStore::getSize).sum();
		setDiskUsage(totalUsage);
	}
	
	public void captureNetworkParameters() {
	    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()); // line changed
	    List<NetworkIF> networks = hal.getNetworkIFs();
	    List<Future<Double>> futures = new ArrayList<>(); 

	    for (NetworkIF network : networks) {
	        futures.add(executor.submit(() -> (double) network.getSpeed())); 
	    }

	    double totalTraffic = 0;
	    for (Future<Double> future : futures) {
	        try {
	            totalTraffic += future.get(1, TimeUnit.SECONDS); 
	        } catch (InterruptedException | ExecutionException | TimeoutException e) {
	            totalTraffic += 0; 
	        }
	    }

	    setNetworkTraffic(totalTraffic);
	    executor.shutdown(); 
	}
	
	public void captureUsbParameters() {
		List<UsbDevice> usbDevices = hal.getUsbDevices(true);
		setUsbDeviceCount(usbDevices.size());
	}
	
	public void captureProcessParameters() {
		OperatingSystem os = systemInfo.getOperatingSystem();
		setProcessCount(os.getProcessCount());
	}
}
