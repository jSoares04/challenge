package com.example.challenge.dto;

import java.util.Objects;

public class SpeedMetricsResponse {
    private final double avg;
    private final double max;
    private final double min;

    public SpeedMetricsResponse(double avg, double max, double min) {
        this.avg = avg;
        this.max = max;
        this.min = min;
    }

    // Getters
    public double getAvg() {
    	return avg;
    }
    public double getMax() {
    	return max; 
    }
    public double getMin() {
    	return min; 
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpeedMetricsResponse that = (SpeedMetricsResponse) o;
        return Double.compare(that.avg, avg) == 0 &&
                Double.compare(that.max, max) == 0 &&
                Double.compare(that.min, min) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(avg, max, min);
    }
}
