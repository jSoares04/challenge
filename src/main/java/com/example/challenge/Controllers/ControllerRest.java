package com.example.challenge.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.challenge.dto.AggregatedMetricsResponse;
import com.example.challenge.dto.MeasurementRequest;
import com.example.challenge.dto.SpeedMetricsResponse;
import com.example.challenge.services.MetricsService;

import java.util.*;



@RestController
@RequestMapping("/api")
class ControllerRest {
	
	@Autowired
	private MetricsService metricService;

    @PostMapping("/linespeed")
    public ResponseEntity<Void> addSpeedMeasurement(@RequestBody MeasurementRequest request) {
    	return metricService.addSpeedMeasurement(request);
    }

	@GetMapping("/metrics/{lineid}")
    public ResponseEntity<SpeedMetricsResponse> getMetrics(@PathVariable("lineid") Long lineId) {
		return metricService.getMetrics(lineId);
    }
	
    @GetMapping("/metrics")
    public ResponseEntity<List<AggregatedMetricsResponse>> getAllMetrics() {
    	return metricService.getAllMetrics();
    }
    

}
