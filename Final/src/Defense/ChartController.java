
package Defense;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.application.Platform;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChartController {
	
	private static final Logger logger = LoggerFactory.getLogger(ChartController.class);
    private final LineChart<String, Number> realTimeChart;

    public ChartController(LineChart<String, Number> realTimeChart) {
        this.realTimeChart = realTimeChart;
        this.realTimeChart.getYAxis().setAutoRanging(true);
    }

    public void updateRealTimeChart(Device device) {
        Platform.runLater(() -> {
        	Map<String, Number> metrics = device.getCurrentParameters().getParameters();
        	logger.debug("uRTC Device '{}' parameters: {}", device.getDeviceName(), metrics);
        	
        	if (metrics == null || metrics.isEmpty()) {
                logger.warn("No metrics available for device: {}", device.getDeviceName());
                return;
            }
        	
        	XYChart.Series<String, Number> series = realTimeChart.getData().stream()
                    .filter(s -> device.getDeviceName() != null && device.getDeviceName().equals(s.getName()))
                    .findFirst()
                    .orElseGet(() -> {
                        XYChart.Series<String, Number> newSeries = new XYChart.Series<>();
                        newSeries.setName(device.getDeviceName());
                        realTimeChart.getData().add(newSeries);
                        return newSeries;
                    });
            
            String currentTime = getCurrentTime();
            Number currentValue = metrics.values().stream().reduce((first, second) -> second).orElse(null);
            String metricName = metrics.keySet().stream().findFirst().orElse("Metric");

            if (currentValue != null) {
                series.getData().add(new XYChart.Data<>(currentTime, currentValue));
                realTimeChart.getXAxis().setLabel(metricName);
                logger.debug("Real-time chart updated for device: {}, metric: {}, value: {}", device.getDeviceName(), metricName, currentValue);
            } else {
                logger.debug("No metrics found for device: {}", device.getDeviceName());
            }

            if (series.getData().size() > 60) {  // https://www.youtube.com/watch?v=FeDBcKbO29M
                series.getData().remove(0);
            }
        });
    }

    private String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
