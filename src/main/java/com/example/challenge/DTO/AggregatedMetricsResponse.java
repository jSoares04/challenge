package com.example.challenge.dto;

import java.util.Objects;

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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregatedMetricsResponse that = (AggregatedMetricsResponse) o;
        return Double.compare(that.avg, avg) == 0 &&
                Double.compare(that.max, max) == 0 &&
                Double.compare(that.min, min) == 0 &&
                Objects.equals(lineId, that.lineId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineId, avg, max, min);
    }
}
