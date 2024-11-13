
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
        this.realTimeChart.getXAxis().setLabel("Time");
        this.realTimeChart.getYAxis().setLabel("Value");
    }

    public void updateRealTimeChart(Device device) {
        Platform.runLater(() -> {
        	XYChart.Series<String, Number> series = realTimeChart.getData().stream()
                    .filter(s -> s.getName().equals(device.getDeviceName()))
                    .findFirst()
                    .orElseGet(() -> {
                        XYChart.Series<String, Number> newSeries = new XYChart.Series<>();
                        newSeries.setName(device.getDeviceName());
                        realTimeChart.getData().add(newSeries);
                        return newSeries;
                    });

            Map<String, Double> metrics = device.getMetrics();
            if (metrics == null || metrics.isEmpty()) {
                logger.warn("No metrics available for device: {}", device.getDeviceName());
                return;
            }
            
            String currentTime = getCurrentTime();
            Double currentValue = metrics.values().stream().reduce((first, second) -> second).orElse(null);
            String metricName = metrics.keySet().stream().findFirst().orElse("Metric");

            if (currentValue != null) {
                series.getData().add(new XYChart.Data<>(currentTime, currentValue));
                realTimeChart.setTitle(metricName);
                logger.debug("Real-time chart updated for device: {}", device.getDeviceName());
            } else {
                logger.debug("No metrics found for device: {}", device.getDeviceName());
            }

            if (series.getData().size() > 60) {
                series.getData().remove(0);
            }
        });
    }

    private String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
