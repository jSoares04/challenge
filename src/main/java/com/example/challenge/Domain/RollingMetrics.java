package com.example.challenge.Domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.NavigableMap;

public class RollingMetrics {
    private double weightedSum = 0;
    private double min = Double.MAX_VALUE;
    private double max = Double.MIN_VALUE;
    private long totalDuration = 0;

    public void addMeasurement(double speed, long duration) {
        weightedSum += speed * duration;
        totalDuration += duration;
        min = Math.min(min, speed);
        max = Math.max(max, speed);
    }

    public double getWeightedAverage() {
    	double value = totalDuration > 0 ? weightedSum / totalDuration : 0;

        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public double getMin() {
    	return min;
    }
    
    public double getMax() {
    	return max;
    }

    public void recalculateMetrics(NavigableMap<Long, SpeedMeasurement> measurements) {
        weightedSum = 0;
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;
        totalDuration = 0;

        Long previousTimestamp = measurements.lastKey() - 60 * 60 * 1000;
        for (Map.Entry<Long, SpeedMeasurement> entry : measurements.entrySet()) {
            long timestamp = entry.getKey();
            double speed = entry.getValue().getSpeed();
            long duration = previousTimestamp != null ? timestamp - previousTimestamp : 0;
            addMeasurement(speed, duration);
            previousTimestamp = timestamp;
        }
    }
}
