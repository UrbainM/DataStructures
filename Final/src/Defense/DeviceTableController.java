package Defense;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import oshi.SystemInfo; // https://github.com/openhab/openhab-addons/tree/main/bundles/org.openhab.binding.systeminfo
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.CentralProcessor; 
import oshi.hardware.CentralProcessor.ProcessorIdentifier;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;

class DeviceTableController {
	
	private static final Logger LOGGER = Logger.getLogger(DeviceTableController.class.getName());
	private final TableView<Device> deviceTable;
	private final ThreatManager threatManager = new ThreatManager();
	private final HardwareAbstractionLayer hal = new SystemInfo().getHardware();
	
	public DeviceTableController(TableView<Device> deviceTable) {
		this.deviceTable = deviceTable;
	}
	
	public void setupDeviceTable() {
		ObservableList<Device> devices = FXCollections.observableArrayList();
		
		try { //https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/NetworkInterface.html
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (networkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = networkInterfaces.nextElement();
				if (networkInterface.isUp() && !networkInterface.isLoopback()) {
					String deviceId = networkInterface.getName();
					String displayName = networkInterface.getDisplayName();
					String ipAddress = getIpAddress(networkInterface);
					
					devices.add(new Device(deviceId, displayName, ipAddress, threatManager));							
				}
			}
		} catch (SocketException e) {
			LOGGER.log(Level.SEVERE, "Failed to get network interfaces", e);
		}
		try {
            addHardwareDetails(devices); 
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving hardware details", e);
        }
		deviceTable.setItems(devices);
	}
	
	private void addHardwareDetails(ObservableList<Device> devices) { 
        devices.add(new Device(getCpuDetails(), "CPU", "N/A", threatManager)); 
        devices.add(new Device(getRamDetails(), "RAM", "N/A", threatManager)); 
        devices.add(new Device(getDiskDetails(), "Disk Storage", "N/A", threatManager)); 
    }
	
	public List<Device> getDevices() {
		ObservableList<Device> devices = deviceTable.getItems();
		return new ArrayList<>(devices);
	}
	
	public Device getSelectedDevice() {
        return deviceTable.getSelectionModel().getSelectedItem();
    }
	
	private String getCpuDetails() {
        return hal.getProcessor().toString(); 
    }
	
	private String getRamDetails() {
        return hal.getMemory().getPhysicalMemory().toString(); 
    }
	
	private String getDiskDetails() {
        return hal.getDiskStores().toString(); 
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
