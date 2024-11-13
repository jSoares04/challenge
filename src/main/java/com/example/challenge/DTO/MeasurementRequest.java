package com.example.challenge.DTO;

public class MeasurementRequest {
    private Long lineId;
    private double speed;
    private long timestamp;

    // Getters and setters
    public Long getLineId() {
    	return lineId;
    }
    
    public void setLineId(Long lineId) {
    	this.lineId = lineId; 
    }
    
    public double getSpeed() {
    	return speed;
    }
    public void setSpeed(double speed) {
    	this.speed = speed;
    }
    public long getTimestamp() {
    	return timestamp;
    }
    public void setTimestamp(long timestamp) {
    	this.timestamp = timestamp;
    }
}
