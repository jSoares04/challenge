package com.example.challenge.domain;

public class SpeedMeasurement {
    private final long timestamp;
    private final double speed;

    public SpeedMeasurement(long timestamp, double speed) {
        this.timestamp = timestamp;
        this.speed = speed;
    }

    public long getTimestamp() { 
    	return timestamp;
    }
    
    public double getSpeed() {
    	return speed;
    }
}
