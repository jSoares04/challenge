package com.example.challenge.DTO;

public class AggregatedMetricsResponse {
    private final Long lineId;
    private final double avg;
    private final double max;
    private final double min;

    public AggregatedMetricsResponse(Long lineId, double avg, double max, double min) {
        this.lineId = lineId;
        this.avg = avg;
        this.max = max;
        this.min = min;
    }

    public Long getLineId() { 
    	return lineId;
    }
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
