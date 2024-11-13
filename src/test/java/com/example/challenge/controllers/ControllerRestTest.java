package com.example.challenge.controllers;

import com.example.challenge.dto.AggregatedMetricsResponse;
import com.example.challenge.dto.MeasurementRequest;
import com.example.challenge.dto.SpeedMetricsResponse;
import com.example.challenge.services.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ControllerRestTest {

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private ControllerRest controllerRest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAddSpeedMeasurement_Success() {
        // Arrange
        MeasurementRequest request = new MeasurementRequest();
        when(metricsService.addSpeedMeasurement(any(MeasurementRequest.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        // Act
        ResponseEntity<Void> response = controllerRest.addSpeedMeasurement(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(metricsService, times(1)).addSpeedMeasurement(request);
    }

    @Test
    void testAddSpeedMeasurement_Outdated() {
        // Arrange
        MeasurementRequest request = new MeasurementRequest();
        when(metricsService.addSpeedMeasurement(any(MeasurementRequest.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).build());

        // Act
        ResponseEntity<Void> response = controllerRest.addSpeedMeasurement(request);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(metricsService, times(1)).addSpeedMeasurement(request);
    }

    @Test
    void testGetMetrics_Success() {
        // Arrange
        Long lineId = 11L;
        SpeedMetricsResponse mockResponse = new SpeedMetricsResponse(150.0, 200.0, 100.0);
        when(metricsService.getMetrics(eq(lineId)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // Act
        ResponseEntity<SpeedMetricsResponse> response = controllerRest.getMetrics(lineId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(metricsService, times(1)).getMetrics(lineId);
    }

    @Test
    void testGetMetrics_NotFound() {
        // Arrange
        Long lineId = 99L;
        when(metricsService.getMetrics(eq(lineId)))
                .thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());

        // Act
        ResponseEntity<SpeedMetricsResponse> response = controllerRest.getMetrics(lineId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(metricsService, times(1)).getMetrics(lineId);
    }

    @Test
    void testGetAllMetrics() {
        // Arrange
        AggregatedMetricsResponse aggregatedMetrics = new AggregatedMetricsResponse(11L, 150.0, 200.0, 100.0);
        List<AggregatedMetricsResponse> mockMetricsList = Collections.singletonList(aggregatedMetrics);
        when(metricsService.getAllMetrics())
                .thenReturn(ResponseEntity.ok(mockMetricsList));

        // Act
        ResponseEntity<List<AggregatedMetricsResponse>> response = controllerRest.getAllMetrics();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockMetricsList, response.getBody());
        verify(metricsService, times(1)).getAllMetrics();
    }
}
