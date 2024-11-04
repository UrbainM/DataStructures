package Defense;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import oshi.SystemInfo; // https://github.com/openhab/openhab-addons/tree/main/bundles/org.openhab.binding.systeminfo
import oshi.hardware.CentralProcessor; 
import oshi.hardware.CentralProcessor.ProcessorIdentifier;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;

public class DeviceTableController {
	
	private final TableView<Device> deviceTable;
	
	public DeviceTableController(TableView<Device> deviceTable) {
		this.deviceTable = deviceTable;
	}
	
	public void setupDeviceTable() {
		ObservableList<Device> devices = FXCollections.observableArrayList();
		
		devices.add(new Device("CPU", "CPU", "OK", "Normal", getCpuDetails()));
		devices.add(new Device("RAM", "Memory", "OK", "Normal", getRamDetails()));
		devices.add(new Device("DISK", "Disk Drive", "OK", "Normal", getDiskDetails()));
		
		try { //https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/NetworkInterface.html
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (networkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = networkInterfaces.nextElement();
				if (networkInterface.isUp() && !networkInterface.isLoopback()) {
					String deviceId = networkInterface.getName();
					String displayName = networkInterface.getDisplayName();
					String ipAddress = getIpAddress(networkInterface);
					
					devices.add(new Device(deviceId, displayName, "OK", "Normal",
							"IP: " + ipAddress));
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		deviceTable.setItems(devices);
	}
	
	private String getCpuDetails() {
		SystemInfo systemInfo = new SystemInfo();
		CentralProcessor processor = systemInfo.getHardware().getProcessor();
		ProcessorIdentifier id = processor.getProcessorIdentifier();
		
		String cpuDetails = id.toString() + processor.toString();
		
		return cpuDetails;
	}
	
	private String getRamDetails() {
		SystemInfo systemInfo = new SystemInfo();
		GlobalMemory memory = systemInfo.getHardware().getMemory();
		
		String ramDetails = memory.toString();
		
		return ramDetails;
	}
	
	private String getDiskDetails() {
		SystemInfo systemInfo = new SystemInfo();
		List<HWDiskStore> diskStores = systemInfo.getHardware().getDiskStores();
		
		String diskDetails = diskStores.toString();
		
		return diskDetails;
	}
	
	private String getIpAddress(NetworkInterface networkInterface) {
		Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
		while (inetAddresses.hasMoreElements()) {
			InetAddress inetAddress = inetAddresses.nextElement();
			if (!inetAddress.isLoopbackAddress()) {
				return inetAddress.getHostAddress();
			}
		}
		return "N/A";
	}
}
