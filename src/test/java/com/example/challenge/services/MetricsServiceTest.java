package com.example.challenge.services;

import com.example.challenge.domain.RollingMetrics;
import com.example.challenge.dto.AggregatedMetricsResponse;
import com.example.challenge.dto.MeasurementRequest;
import com.example.challenge.dto.SpeedMetricsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.NavigableMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MetricsServiceTest {

    @Mock
    private RollingMetrics rollingMetricsMock;

    @InjectMocks
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

//    @Test
//    void testAddSpeedMeasurement_Success() {
//        // Arrange
//        MeasurementRequest request = new MeasurementRequest();
//        request.setLineId(10L);
//        request.setTimestamp(System.currentTimeMillis());
//        request.setSpeed(100.0);
//
//        NavigableMap<Long, SpeedMeasurement> measurements = new TreeMap<>();
//        when(rollingMetricsMock.recalculateMetrics(any(NavigableMap.class))).thenReturn(true);
//
//        // Act
//        ResponseEntity<Void> response = metricsService.addSpeedMeasurement(request);
//
//        // Assert
//        assertEquals(HttpStatus.CREATED, response.getStatusCode());
//        verify(rollingMetricsMock, times(1)).recalculateMetrics(any(NavigableMap.class));
//    }

    @Test
    void testAddSpeedMeasurement_Outdated() {
        // Arrange
        MeasurementRequest request = new MeasurementRequest();
        request.setLineId(10L);
        request.setTimestamp(System.currentTimeMillis() - 65 * 60 * 1000);  // More than 60 minutes ago
        request.setSpeed(100.0);

        // Act
        ResponseEntity<Void> response = metricsService.addSpeedMeasurement(request);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(rollingMetricsMock, never()).recalculateMetrics(any(NavigableMap.class));
    }

    @Test
    void testGetMetrics_Success() {    // Arrange
        Long lineId = 10L;
        
        // Create an expected response with predefined values
        SpeedMetricsResponse expectedResponse = new SpeedMetricsResponse(150.0, 200.0, 100.0);

        // Create a mock for RollingMetrics and configure its return values
        RollingMetrics rollingMetricsMock = mock(RollingMetrics.class);
        when(rollingMetricsMock.getWeightedAverage()).thenReturn(150.0);
        when(rollingMetricsMock.getMax()).thenReturn(200.0);
        when(rollingMetricsMock.getMin()).thenReturn(100.0);

        // Populate metricsMap in the MetricsService with the mocked RollingMetrics instance
        metricsService.metricsMap.put(lineId, rollingMetricsMock);

        // Act
        ResponseEntity<SpeedMetricsResponse> response = metricsService.getMetrics(lineId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());

        // Verify that the mocked methods were called exactly once
        verify(rollingMetricsMock, times(1)).getWeightedAverage();
        verify(rollingMetricsMock, times(1)).getMax();
        verify(rollingMetricsMock, times(1)).getMin();
    }

    @Test
    void testGetMetrics_NotFound() {
        // Arrange
        Long unknownLineId = 99L;

        // Act
        ResponseEntity<SpeedMetricsResponse> response = metricsService.getMetrics(unknownLineId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(rollingMetricsMock, never()).getWeightedAverage();
    }

    @Test
    void testGetAllMetrics() {
        // Arrange
        Long lineId = 10L;
        RollingMetrics rollingMetrics = mock(RollingMetrics.class);
        when(rollingMetrics.getWeightedAverage()).thenReturn(120.0);
        when(rollingMetrics.getMax()).thenReturn(150.0);
        when(rollingMetrics.getMin()).thenReturn(90.0);

        // Populate metricsMap with the mocked RollingMetrics instance
        metricsService.metricsMap.put(lineId, rollingMetrics);

        // Expected aggregated response
        AggregatedMetricsResponse expectedResponse = new AggregatedMetricsResponse(lineId, 120.0, 150.0, 90.0);

        // Act
        ResponseEntity<List<AggregatedMetricsResponse>> response = metricsService.getAllMetrics();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(expectedResponse, response.getBody().get(0));
        verify(rollingMetrics, times(1)).getWeightedAverage();
        verify(rollingMetrics, times(1)).getMax();
        verify(rollingMetrics, times(1)).getMin();}

    @Test
    void testRemoveOldEntriesScheduled() {
        // Arrange
        // Initialize olderEntries and lineData for this test
        metricsService.removeOldEntriesSheduled();

        // Act
        metricsService.removeOldEntriesSheduled();

        // Assert
        verify(rollingMetricsMock, atLeastOnce()).recalculateMetrics(any(NavigableMap.class));
    }

}
