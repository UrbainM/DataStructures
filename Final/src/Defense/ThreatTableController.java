package Defense;

import javafx.collections.ObservableList;
import javafx.fxml.FXML; 
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class ThreatTableController {

@FXML private TableView<Threat> threatTable;
@FXML private TableColumn<Threat, String> idColumn;
@FXML private TableColumn<Threat, String> deviceIdColumn;
@FXML private TableColumn<Threat, Threat.ThreatType> typeColumn;
@FXML private TableColumn<Threat, Threat.ThreatSeverity> severityColumn;
@FXML private TableColumn<Threat, Boolean> isActiveColumn;
private ThreatManager threatManager;

	public ThreatTableController(TableView<Threat> priorityQueueTable) {
	    this.threatTable = priorityQueueTable;
		this.threatManager = new ThreatManager();
	    this.threatManager.addThreatListener(new ThreatManager.ThreatListener() {
	        @Override
	        public void onNewThreat(Threat threat) {
	            updateThreatTable();
	        }
	
	        @Override
	        public void onThreatResolved(Threat threat) {
	            updateThreatTable();
	        }
	
	        @Override
	        public void onCriticalThreat(Threat threat) {
	            // Handle critical threat
	        }
	    });
	}
	
	@FXML
	private void initialize() {
		idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
	    deviceIdColumn.setCellValueFactory(new PropertyValueFactory<>("deviceId"));
	    typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
	    severityColumn.setCellValueFactory(new PropertyValueFactory<>("severity"));
	    isActiveColumn.setCellValueFactory(new PropertyValueFactory<>("isActive"));
	
	    threatTable.setItems(threatManager.getThreatHistory());
	}
	
	public void addThreat(Threat threat) {
		threatManager.addThreat(threat);
	}
	
	public void removeThreat(String threatId) {
		threatManager.resolveThreat(threatId);
	}
	
	public void updateThreatTable() {
		ObservableList<Threat> threats = threatManager.getThreatHistory();
	    threatTable.setItems(threats);
	}
}