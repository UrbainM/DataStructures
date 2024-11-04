package Defense;

import javafx.beans.property.*;

public class Parameters {
	private final DoubleProperty cpuUsage = new SimpleDoubleProperty();
    private final DoubleProperty memoryUsage = new SimpleDoubleProperty();
    private final DoubleProperty networkTraffic = new SimpleDoubleProperty();
    private final DoubleProperty diskUsage = new SimpleDoubleProperty();
    private final IntegerProperty processCount = new SimpleIntegerProperty();
    private final IntegerProperty openPorts = new SimpleIntegerProperty();

    // Property getters
    public DoubleProperty cpuUsageProperty() { return cpuUsage; }
    public DoubleProperty memoryUsageProperty() { return memoryUsage; }
    public DoubleProperty networkTrafficProperty() { return networkTraffic; }
    public DoubleProperty diskUsageProperty() { return diskUsage; }
    public IntegerProperty processCountProperty() { return processCount; }
    public IntegerProperty openPortsProperty() { return openPorts; }

    // Regular getters
    public double getCpuUsage() { return cpuUsage.get(); }
    public double getMemoryUsage() { return memoryUsage.get(); }
    public double getNetworkTraffic() { return networkTraffic.get(); }
    public double getDiskUsage() { return diskUsage.get(); }
    public int getProcessCount() { return processCount.get(); }
    public int getOpenPorts() { return openPorts.get(); }

    // Setters
    public void setCpuUsage(double value) { cpuUsage.set(value); }
    public void setMemoryUsage(double value) { memoryUsage.set(value); }
    public void setNetworkTraffic(double value) { networkTraffic.set(value); }
    public void setDiskUsage(double value) { diskUsage.set(value); }
    public void setProcessCount(int value) { processCount.set(value); }
    public void setOpenPorts(int value) { openPorts.set(value); }
}
