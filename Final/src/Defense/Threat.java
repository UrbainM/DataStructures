package Defense;

import java.time.LocalDateTime;

import javafx.beans.property.*;

public class Threat implements Comparable<Threat> {
	private final StringProperty id = new SimpleStringProperty();
    private final StringProperty deviceId = new SimpleStringProperty();
    private final ObjectProperty<ThreatType> type = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> timestamp = new SimpleObjectProperty<>();
    private final StringProperty description = new SimpleStringProperty();
    private final ObjectProperty<ThreatSeverity> severity = new SimpleObjectProperty<>();
    private final BooleanProperty isActive = new SimpleBooleanProperty(true);

    public enum ThreatType {
        UNUSUAL_BEHAVIOR(3),
        UNAUTHORIZED_ACCESS(4),
        MALWARE_DETECTED(5),
        CONFIGURATION_CHANGE(2),
        NETWORK_ANOMALY(3),
        DENIAL_OF_SERVICE(5);

        private final int priorityWeight;

        ThreatType(int priorityWeight) {
            this.priorityWeight = priorityWeight;
        }

        public int getPriorityWeight() {
        	// TODO Return the priority by comparison
            return priorityWeight;
        }
    }

    public enum ThreatSeverity {
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        WARNING(4),
        CRITICAL(5), ;

        private final int level;

        ThreatSeverity(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    public Threat(String id, String deviceId, ThreatType type, ThreatSeverity severity) {
        this.id.set(id);
        this.deviceId.set(deviceId);
        this.type.set(type);
        this.severity.set(severity);
        this.timestamp.set(LocalDateTime.now());
    }

    // Property getters
    public StringProperty idProperty() { return id; }
    public StringProperty deviceIdProperty() { return deviceId; }
    public ObjectProperty<ThreatType> typeProperty() { return type; }
    public ObjectProperty<LocalDateTime> timestampProperty() { return timestamp; }
    public StringProperty descriptionProperty() { return description; }
    public ObjectProperty<ThreatSeverity> severityProperty() { return severity; }
    public BooleanProperty isActiveProperty() { return isActive; }

    // Regular getters
    public String getId() { return id.get(); }
    public String getDeviceId() { return deviceId.get(); }
    public ThreatType getType() { return type.get(); }
    public LocalDateTime getTimestamp() { return timestamp.get(); }
    public String getDescription() { return description.get(); }
    public ThreatSeverity getSeverity() { return severity.get(); }
    public boolean isActive() { return isActive.get(); }

    public void setDescription(String value) { description.set(value); }
    public void setActive(boolean value) { isActive.set(value); }

    // Implement Comparable for PriorityQueue ordering
    @Override
    public int compareTo(Threat other) {
        // First compare by severity
        int severityComparison = other.getSeverity().getLevel() - this.getSeverity().getLevel();
        if (severityComparison != 0) {
            return severityComparison;
        }
        
        // Then by threat type priority
        int typeComparison = other.getType().getPriorityWeight() - this.getType().getPriorityWeight();
        if (typeComparison != 0) {
            return typeComparison;
        }
        
        // Finally by timestamp (more recent first)
        return other.getTimestamp().compareTo(this.getTimestamp());
    }
}