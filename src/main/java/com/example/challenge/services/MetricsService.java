package com.example.challenge.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import com.example.challenge.domain.RollingMetrics;
import com.example.challenge.domain.SpeedMeasurement;
import com.example.challenge.dto.AggregatedMetricsResponse;
import com.example.challenge.dto.MeasurementRequest;
import com.example.challenge.dto.SpeedMetricsResponse;

import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;



@Service
public class MetricsService {
	
	private static final Logger LOG = LogManager.getLogger(MetricsService.class);

	
    private static final long TIME_WINDOW = 60 * 60 * 1000; // 60 minutes in milliseconds
    private final Set<Long> knownLineIds = Set.of(10L, 20L, 30L, 40L, 50L, 60l, 70L, 80L); // Example line IDs
    private final Map<Long, NavigableMap<Long, SpeedMeasurement>> lineData = new ConcurrentHashMap<>();
    private final Map<Long, RollingMetrics> metricsMap = new ConcurrentHashMap<>();
    private final Map<Long, Lock> lineLocks = new ConcurrentHashMap<>();
    private final Map<Long, Long> olderEntries = new ConcurrentHashMap<>();

    public ResponseEntity<Void> addSpeedMeasurement(@RequestBody MeasurementRequest request) {
    	HttpStatus errorStatus = requestValidation(request);
    	if (errorStatus != null) {
    		return ResponseEntity.status(errorStatus).build();
    	}
        Long lineId = request.getLineId();

        Lock lock = lineLocks.computeIfAbsent(lineId, k -> new ReentrantLock());
        lock.lock();
        try {
            NavigableMap<Long, SpeedMeasurement> measurements = lineData.computeIfAbsent(lineId, k -> new TreeMap<>());
            measurements.put(request.getTimestamp(), new SpeedMeasurement(request.getTimestamp(), request.getSpeed()));
            removeOldEntries(lineId, false);
            metricsMap.computeIfAbsent(lineId, k -> new RollingMetrics()).recalculateMetrics(measurements);
            fillOlderEntriesMap(lineId, measurements.firstKey());
        } finally {
            lock.unlock();
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public ResponseEntity<SpeedMetricsResponse> getMetrics(@PathVariable("lineid") Long lineId) {
        if (!metricsMap.containsKey(lineId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        RollingMetrics metrics;
        Lock lock = lineLocks.computeIfAbsent(lineId, k -> new ReentrantLock());
        lock.lock();
        try {
        	updateMetricsIfNecessary(lineId);
            metrics = metricsMap.get(lineId);
        } finally {
            lock.unlock();
        }

        SpeedMetricsResponse response = new SpeedMetricsResponse(metrics.getWeightedAverage(), metrics.getMax(), metrics.getMin());
        return ResponseEntity.ok(response);
    }
	
    public ResponseEntity<List<AggregatedMetricsResponse>> getAllMetrics() {
        List<AggregatedMetricsResponse> allMetrics = new ArrayList<>();

        for (Long lineId : knownLineIds) {
            if (metricsMap.containsKey(lineId)) {
                Lock lock = lineLocks.computeIfAbsent(lineId, k -> new ReentrantLock());
                lock.lock();
                try {
                	updateMetricsIfNecessary(lineId);
                    RollingMetrics metrics = metricsMap.get(lineId);
                    allMetrics.add(new AggregatedMetricsResponse(lineId, metrics.getWeightedAverage(), metrics.getMax(), metrics.getMin()));               	
                } finally {
					lock.unlock();
				}
            }
        }

        return ResponseEntity.ok(allMetrics);
    }
    
    private void updateMetricsIfNecessary(Long lineId) {
        long now = Instant.now().toEpochMilli();
        Long timeStamp= olderEntries.getOrDefault(lineId, null);
        if (timeStamp != null && now - timeStamp > TIME_WINDOW) {
        	removeOldEntries(lineId, true);
        }
	}

	private HttpStatus requestValidation(MeasurementRequest request) {
    	//check if lineId exists
		Long lineId = request.getLineId();
		if (!knownLineIds.contains(lineId)) {
			return HttpStatus.NOT_FOUND;
		}
		
		//check if timestamp is older than time window
		long now = Instant.now().toEpochMilli();
		if (now - request.getTimestamp() > TIME_WINDOW) {
			return HttpStatus.NO_CONTENT;
		}
		return null;
	}

	private void fillOlderEntriesMap(Long lineId, Long olderEntry) {
    	if (olderEntry== null) {
    		//there is no entries for lineId so remove the key from hashmap
    		olderEntries.remove(lineId);
    		return;
    	}
    	olderEntries.put(lineId, olderEntry);
	}

    private void removeOldEntries(Long lineId, boolean recalculation) {
        long now = Instant.now().toEpochMilli();
        NavigableMap<Long, SpeedMeasurement> measurements = lineData.get(lineId);
        
        boolean metricsRecalculation = false;
        while (!measurements.isEmpty() && now - measurements.firstKey() > TIME_WINDOW) {
            measurements.pollFirstEntry();
            metricsRecalculation = true;
        }
        
        if (metricsRecalculation && recalculation) {
        	//recalculate Metrics
        	if (measurements.isEmpty()) {
        		metricsMap.remove(lineId);
        	} else {
                metricsMap.computeIfAbsent(lineId, k -> new RollingMetrics()).recalculateMetrics(measurements);
        	}
            //update olderEntriesMap after remove
            if (measurements.size() > 0) {
                fillOlderEntriesMap(lineId, measurements.firstKey());
            } else {
            	fillOlderEntriesMap(lineId, null);
            }
        }
        
        
    }
    
    //called every 15 seconds to clear old entries
	@Scheduled(fixedDelay = 5 * 1000)
	public void removeOldEntriesSheduled() {
        LOG.info("Starting removing old entries shecdule");
        long now = Instant.now().toEpochMilli();
        Set<Long> toUpdateLines = new HashSet<Long>();
        //flag the lineIds that must be updated due to older entries
        for (Entry<Long, Long> entry : olderEntries.entrySet()) {
        	if (now - entry.getValue()>TIME_WINDOW) {
            	toUpdateLines.add(entry.getKey());
        	}
        }
        
        for (Long lineId : toUpdateLines) {
            LOG.info("removing old entry for lineID " + lineId);
            Lock lock = lineLocks.computeIfAbsent(lineId, k -> new ReentrantLock());
            lock.lock();
            try {
    			removeOldEntries(lineId, true);
            } finally {
                lock.unlock();
			}
		}


    }

}
