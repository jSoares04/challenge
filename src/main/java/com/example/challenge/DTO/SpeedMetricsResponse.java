package com.example.challenge.dto;

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
}
