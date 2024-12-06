
package Defense;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//This Controller is responsible for updating the realTimeChart tied to the selected device in the DeviceTableController
public class ChartController {
	
	private static final Logger logger = LoggerFactory.getLogger(ChartController.class);
    @FXML private LineChart<String, Number> realTimeChart;
    @FXML private CategoryAxis timeAxis;
    private String currentDeviceName = null;
    private List<String> timeLabels = new LinkedList<>();

    public ChartController(LineChart<String, Number> realTimeChart) {
        this.realTimeChart = realTimeChart;
        realTimeChart.setAnimated(true);
        this.timeAxis = (CategoryAxis) realTimeChart.getXAxis();
        initializeTimeAxis();
    }

    private void initializeTimeAxis() {   // This took me weeks to solve.  TimeSeries aren't inherent to javafx so instead there are 60 categorical x-bins representing seconds.
    	for (int i=0; i < 60; i++) {
    		timeLabels.add(i + "s");
    	}
    	timeAxis.setCategories(FXCollections.observableArrayList(timeLabels));
    }
    
    public void updateRealTimeChart(Device device) {  // This method retrieves all populated parameters contained in a device and graphs them on a chart.  If a device has more than 1 parameter it should have its own line.
        Platform.runLater(() -> {
        	Map<String, Number> metrics = device.getCurrentParameters().getParameters();
        	//logger.info("uRTC Device '{}' parameters: {}", device.getDeviceName(), metrics);
        	
        	if (metrics == null || metrics.isEmpty()) {
                logger.warn("No parameters available for device: {}", device.getDeviceName());
                return;
            } 
        	
			if (!device.getDeviceName().equals(currentDeviceName)) {  // This ensures only 1 device is on the chart at a time
				realTimeChart.getData().clear();
				currentDeviceName = device.getDeviceName();
			}
			
			XYChart.Series<String, Number> series = realTimeChart.getData().stream()  // This is supposed to keep historical charts in reserve, but needs some work.
	                .filter(s -> device.getDeviceName().equals(s.getName()))
	                .findFirst()
	                .orElseGet(() -> {
	                    XYChart.Series<String, Number> newSeries = new XYChart.Series<>();
	                    newSeries.setName(device.getDeviceName());
	                    realTimeChart.getData().add(newSeries);
	                    return newSeries;
	                });
			
        	metrics.forEach((metricName, value) -> {  
        		if (value != null && value.doubleValue() > 0.0001) {
        			String currentTime = getCurrentTime();  // Replaces the placeholder "categorical labels" with timestamps.
        			if (timeLabels.size() >= 60) {
                        timeLabels.remove(timeLabels.iterator().next());
                    }
        			timeLabels.add(currentTime);
        			timeAxis.setCategories(FXCollections.observableArrayList(timeLabels));
        			series.getData().add(new XYChart.Data<>(currentTime, value));
        			//logger.info("Added data point: {}, {}", currentTime, value);
        			
					if (series.getData().size() > 60) {  // Max 60 datapoints
						series.getData().remove(0);
					}
        		}
        	});
        	//realTimeChart.getData().forEach(s -> logger.info("Series: {}, Data: {}", s.getName(), s.getData()));
        });
    }
    private String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss:SSS"));
    }
}
