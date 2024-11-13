package com.example.challenge.Controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.challenge.DTO.MeasurementRequest;
import com.example.challenge.DTO.SpeedMetricsResponse;
import com.example.challenge.Domain.RollingMetrics;
import com.example.challenge.Domain.SpeedMeasurement;

import java.util.NavigableMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ChallengApplicationTestUnit {

    @Mock
    private NavigableMap<Long, SpeedMeasurement> measurementsMock;

    @Mock
    private RollingMetrics rollingMetricsMock;

    @InjectMocks
    private ControllerRest controllerRest;

    private long currentTimestamp;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        currentTimestamp = System.currentTimeMillis();
    }

    @Test
    void testAddMeasurement_Success() {
        // Arrange
        MeasurementRequest request = new MeasurementRequest();
        request.setLineId(10L);
        request.setSpeed(200.0);
        request.setTimestamp(currentTimestamp);

        // Act
        ResponseEntity<Void> response = controllerRest.addSpeedMeasurement(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(measurementsMock, times(1)).put(eq(request.getTimestamp()), any(SpeedMeasurement.class));
        verify(rollingMetricsMock, times(1)).recalculateMetrics(any(NavigableMap.class));
    }

    @Test
    void testAddMeasurement_Outdated() {
        // Arrange
        MeasurementRequest request = new MeasurementRequest();
        request.setLineId(20L);
        request.setSpeed(200.0);
        request.setTimestamp(currentTimestamp - (65 * 60 * 1000)); // 65 minutes ago

        // Act
        ResponseEntity<Void> response = controllerRest.addSpeedMeasurement(request);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(measurementsMock, never()).put(anyLong(), any(SpeedMeasurement.class));
        verify(rollingMetricsMock, never()).recalculateMetrics(any(NavigableMap.class));
    }

    @Test
    void testAddMeasurement_UnknownLineId() {
        // Arrange
        MeasurementRequest request = new MeasurementRequest();
        request.setLineId(99L); // Unknown line ID
        request.setSpeed(200.0);
        request.setTimestamp(currentTimestamp);

        // Act
        ResponseEntity<Void> response = controllerRest.addSpeedMeasurement(request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(measurementsMock, never()).put(anyLong(), any(SpeedMeasurement.class));
        verify(rollingMetricsMock, never()).recalculateMetrics(any(NavigableMap.class));
    }

    @Test
    void testGetMetrics_Success() {
        // Arrange
        Long lineId = 10L;
        double expectedAvg = 150.0;
        double expectedMax = 200.0;
        double expectedMin = 100.0;

        when(rollingMetricsMock.getWeightedAverage()).thenReturn(expectedAvg);
        when(rollingMetricsMock.getMax()).thenReturn(expectedMax);
        when(rollingMetricsMock.getMin()).thenReturn(expectedMin);

        // Act
        ResponseEntity<SpeedMetricsResponse> response = controllerRest.getMetrics(lineId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        SpeedMetricsResponse metrics = response.getBody();
        assert metrics != null;
        assertEquals(expectedAvg, metrics.getAvg());
        assertEquals(expectedMax, metrics.getMax());
        assertEquals(expectedMin, metrics.getMin());

        verify(rollingMetricsMock, times(1)).getWeightedAverage();
        verify(rollingMetricsMock, times(1)).getMax();
        verify(rollingMetricsMock, times(1)).getMin();
    }

    @Test
    void testGetMetrics_UnknownLineId() {
        // Arrange
        Long unknownLineId = 99L;

        // Act
        ResponseEntity<SpeedMetricsResponse> response = controllerRest.getMetrics(unknownLineId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(rollingMetricsMock, never()).getWeightedAverage();
        verify(rollingMetricsMock, never()).getMax();
        verify(rollingMetricsMock, never()).getMin();
    }
}
