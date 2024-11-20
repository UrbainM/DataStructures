package Defense;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo; // https://github.com/openhab/openhab-addons/tree/main/bundles/org.openhab.binding.systeminfo
import oshi.hardware.HardwareAbstractionLayer;

class DeviceTableController {
	
	private static final Logger logger = LoggerFactory.getLogger(DeviceTableController.class);
	private final TableView<Device> deviceTable;
	private final ThreatManager threatManager;
	private final HardwareAbstractionLayer hal;
	
	public DeviceTableController(TableView<Device> deviceTable) {
		this.deviceTable = deviceTable;
		this.threatManager = new ThreatManager();
		this.hal = new SystemInfo().getHardware();
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
					
					Device device = new Device(deviceId, displayName, ipAddress, threatManager);
					devices.add(device);
					/*
	                 if (ipAddress != null && !ipAddress.equals("N/A")) {
	                     devices.add(new Device(deviceId, displayName, ipAddress, threatManager));    
	                 } else {
	                     logger.info("Device {} is offline (no valid IP).", displayName);
	                 }*/ // Commented out to avoid adding devices to the table if no IP			
				}
			}
		} catch (SocketException e) {
			logger.info("Failed to get network interfaces", e);
		}
		
		try {
            addHardwareDetails(devices); 
        } catch (Exception e) {
            logger.info("Error retrieving hardware details", e);
        }
		
		Platform.runLater(() -> {
            deviceTable.setItems(devices);
            logger.info("Device table setup completed.");
        });
	}
	
	private void addHardwareDetails(ObservableList<Device> devices) {
	    try {
	        devices.add(new Device(getCpuDetails(), "CPU", "N/A", threatManager));
	        devices.add(new Device(getRamDetails(), "RAM", "N/A", threatManager));
	        devices.add(new Device(getDiskDetails(), "Disk Storage", "N/A", threatManager));
	    } catch (Exception e) {
	        logger.info("Error retrieving hardware details", e);
	    }
	}
	
	public List<Device> getDevices() {
		return deviceTable.getItems();
	}
	
	public Device getSelectedDevice() {
        return deviceTable.getSelectionModel().getSelectedItem();
    }
	
	private String getCpuDetails() {
		return hal.getProcessor().getClass().descriptorString();
    }
	
	private String getRamDetails() {
		long availableMemory = hal.getMemory().getAvailable();
		String memoryName = hal.getMemory().getClass().descriptorString();
        return availableMemory / (1024 * 1024) + " MB" + " - " + memoryName;
    }
	
	private String getDiskDetails() {
        return hal.getDiskStores().get(0).getName() + " - " + hal.getDiskStores().get(0).getSize() / (1024 * 1024 * 1024) + " GB";
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
