
package Defense;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ChartController {
    private final LineChart<String, Number> realTimeChart;

    public ChartController(LineChart<String, Number> realTimeChart) {
        this.realTimeChart = realTimeChart;
    }

    public void updateRealTimeChart(Device device) {
        XYChart.Series<String, Number> series;
        if (realTimeChart.getData().isEmpty()) {
            series = new XYChart.Series<>();
            series.setName(device.getDeviceName());
            realTimeChart.getData().add(series);
        } else {
            series = realTimeChart.getData().get(0);
        }

        Map<String, Double> metrics = device.getMetrics();
        String currentTime = getCurrentTime();
        Double currentValue = metrics.get(currentTime);

        if (currentValue != null) {
            series.getData().add(new XYChart.Data<>(currentTime, currentValue));
        }

        if (series.getData().size() > 60) {
            series.getData().remove(0);
        }
    }

    private String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    private Device getSelectedDevice() {
		return null;
    }
}
