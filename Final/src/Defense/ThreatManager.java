package Defense;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import Defense.Threat.ThreatSeverity;
import Defense.Threat.ThreatType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreatManager {
	private static final Logger logger = LoggerFactory.getLogger(ThreatManager.class);
    private final PriorityQueue<Threat> activeThreatQueue;
    private final ObservableList<Threat> threatHistory;
    private final Map<String, List<Threat>> deviceThreats;
    private final List<ThreatListener> threatListeners;
    

    public interface ThreatListener {
        void onNewThreat(Threat threat);
        void onThreatResolved(Threat threat);
        void onCriticalThreat(Threat threat);
    }
    
    public void addDevice(Device device) {
        // TODO Add device
        logger.info("Device added to threat management: {}", device.getDeviceName());
    }
    
    public void removeDevice(Device device) {
        // TODO Remove device
        logger.info("Device removed from threat management: {}", device.getDeviceName());
    }

    public ThreatManager() {
        this.activeThreatQueue = new PriorityQueue<>();
        this.threatHistory = FXCollections.observableArrayList();
        this.deviceThreats = new HashMap<>();
        this.threatListeners = new ArrayList<>();
        logger.info("Threat manager initialized.");
    }

    public void addThreat(Threat threat) {
        Platform.runLater(() -> {
        	threat.timestampProperty().set(timeStamp());
            activeThreatQueue.offer(threat);
            threatHistory.add(threat);
            
            deviceThreats.computeIfAbsent(threat.getDeviceId(), k -> new ArrayList<>())
                        .add(threat);
			for (ThreatListener listener : threatListeners) {
				listener.onNewThreat(threat);
			}
            
            notifyNewThreat(threat);
            
            if (threat.getSeverity() == ThreatSeverity.CRITICAL) {
                notifyCriticalThreat(threat);
            }
        });
    }

    public void resolveThreat(String threatId) {
        Threat threat = threatHistory.stream()
                .filter(t -> t.getId().equals(threatId))
                .findFirst()
                .orElse(null);
        if (threat != null) {
            threat.setActive(false);
            notifyThreatResolved(threat);
            for (ThreatListener listener : threatListeners) {
                listener.onThreatResolved(threat);
            }
        }
    }
    public Threat getNextHighestThreat() {
        return activeThreatQueue.peek();
    }

    public List<Threat> getActiveThreatsForDevice(String deviceId) {
        return deviceThreats.getOrDefault(deviceId, new ArrayList<>()).stream()
                .filter(Threat::isActive)
                .collect(Collectors.toList());
    }

    public ObservableList<Threat> getThreatHistory() {
        return FXCollections.unmodifiableObservableList(threatHistory);
    }

    public void addThreatListener(ThreatListener listener) {
        threatListeners.add(listener);
    }

    private void notifyNewThreat(Threat threat) {
        threatListeners.forEach(listener -> listener.onNewThreat(threat));
    }

    private void notifyThreatResolved(Threat threat) {
        threatListeners.forEach(listener -> listener.onThreatResolved(threat));
    }

    private void notifyCriticalThreat(Threat threat) {
        threatListeners.forEach(listener -> listener.onCriticalThreat(threat));
    }

    /* private Threat findThreatById(String threatId) {
        return threatHistory.stream()
                .filter(t -> t.getId().equals(threatId))
                .findFirst()
                .orElse(null);
    }*/

    // Analysis methods
    public Map<ThreatType, Long> analyzeThreatsByType() {
        return threatHistory.stream()
                .collect(Collectors.groupingBy(
                    Threat::getType,
                    Collectors.counting()
                ));
    }

    public Map<String, Long> analyzeThreatsPerDevice() {
        return threatHistory.stream()
                .collect(Collectors.groupingBy(
                    Threat::getDeviceId,
                    Collectors.counting()
                ));
    }

    public double calculateThreatSeverityScore(String deviceId) {
        return getActiveThreatsForDevice(deviceId).stream()
                .mapToDouble(threat -> 
                    threat.getSeverity().getLevel() * threat.getType().getPriorityWeight())
                .sum();
    }
    
    private LocalDateTime timeStamp() {
    	        return LocalDateTime.now();
    }
}