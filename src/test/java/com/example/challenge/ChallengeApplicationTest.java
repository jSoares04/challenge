package com.example.challenge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.challenge.dto.MeasurementRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ChallengeApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long currentTimestamp;

    @BeforeEach
    void setUp() {
        currentTimestamp = Instant.now().toEpochMilli();
    }

    @Test
    void testAddMeasurement_Success() throws Exception {
        MeasurementRequest request = new MeasurementRequest();
        request.setLineId(10L);
        request.setSpeed(200.5);
        request.setTimestamp(currentTimestamp);

        mockMvc.perform(post("/api/linespeed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void testAddMeasurement_Outdated() throws Exception {
        MeasurementRequest request = new MeasurementRequest();
        request.setLineId(10L);
        request.setSpeed(150.5);
        request.setTimestamp(currentTimestamp - (65 * 60 * 1000)); // 65 minutes ago

        mockMvc.perform(post("/api/linespeed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void testAddMeasurement_UnknownLineId() throws Exception {
        MeasurementRequest request = new MeasurementRequest();
        request.setLineId(99L); // Unknown line ID
        request.setSpeed(100.5);
        request.setTimestamp(currentTimestamp);

        mockMvc.perform(post("/api/linespeed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetMetrics_Success() throws Exception {
        // Add some measurements
        addMeasurement(50L, 100.0, currentTimestamp - 30 * 60 * 1000); // 30 minutes ago
        addMeasurement(50L, 200.0, currentTimestamp - 15 * 60 * 1000); // 15 minutes ago
        addMeasurement(50L, 150.0, currentTimestamp);  // now

        // Fetch metrics
        mockMvc.perform(get("/api/metrics/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.max").value(200.0))
                .andExpect(jsonPath("$.min").value(100.0))
                .andExpect(jsonPath("$.avg").value(137.5));
    }
    
    @Test
    void testGetMetrics_OneMeasurement_Success() throws Exception {
        // Add some measurements
        addMeasurement(70L, 100.0, currentTimestamp - 30 * 60 * 1000); // 30 minutes ago

        // Fetch metrics
        mockMvc.perform(get("/api/metrics/70"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.max").value(100.0))
                .andExpect(jsonPath("$.min").value(100.0))
                .andExpect(jsonPath("$.avg").value(100.0));
    }

    @Test
    void testGetMetrics_UnknownLineId() throws Exception {
        mockMvc.perform(get("/api/metrics/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testHandleDelayedSample() throws Exception {
        // Add measurements with different timestamps
        addMeasurement(60L, 150.0, currentTimestamp - 50 * 60 * 1000); // 50 minutes ago
        addMeasurement(60L, 100.0, currentTimestamp - 10 * 60 * 1000); // 10 minutes ago
        addMeasurement(60L, 200.0, currentTimestamp - 30 * 60 * 1000); // Delayed sample, 30 minutes ago

        mockMvc.perform(get("/api/metrics/60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avg").value(150.0))
                .andExpect(jsonPath("$.max").value(200.0))
                .andExpect(jsonPath("$.min").value(100.0));
    }

    @Test
    void testConcurrentRequests() throws Exception {
        Runnable task = () -> {
            try {
                addMeasurement(10L, Math.random() * 100, currentTimestamp);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        // Start multiple threads to simulate concurrency
        int threadCount = 10;
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(task);
            thread.start();
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Verify metrics endpoint after concurrent writes
        mockMvc.perform(get("/api/metrics/10"))
                .andExpect(status().isOk());
    }
    
    @Test
    void testHighVolumeConcurrentAdds() throws Exception {
        int threadCount = 50;
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        Runnable task = () -> {
            try {
                addMeasurement(10L, Math.random() * 100, currentTimestamp);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        };

        for (int i = 0; i < threadCount; i++) {
            new Thread(task).start();
        }

        // Wait for all threads to finish
        latch.await();

        // Verify the metrics after all requests have completed
        mockMvc.perform(get("/api/metrics/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avg").exists())
                .andExpect(jsonPath("$.max").exists())
                .andExpect(jsonPath("$.min").exists());
    }

    @Test
    void testConcurrentAddAndRetrieveMetrics() throws Exception {
        int addThreadCount = 20;
        int retrieveThreadCount = 20;
        CountDownLatch latch = new CountDownLatch(addThreadCount + retrieveThreadCount);
        
        Runnable addTask = () -> {
            try {
                addMeasurement(10L, Math.random() * 200, currentTimestamp);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        };

        Runnable retrieveTask = () -> {
            try {
                mockMvc.perform(get("/api/metrics/10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.avg").exists())
                        .andExpect(jsonPath("$.max").exists())
                        .andExpect(jsonPath("$.min").exists());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        };

        for (int i = 0; i < addThreadCount; i++) {
            new Thread(addTask).start();
        }

        for (int i = 0; i < retrieveThreadCount; i++) {
            new Thread(retrieveTask).start();
        }

        // Wait for all threads to finish
        latch.await();
    }

    @Test
    void testGetAllMetrics() throws Exception {
        // Add measurements for multiple lines
        addMeasurement(10L, 120.0, currentTimestamp - 30 * 60 * 1000);
        addMeasurement(40L, 130.0, currentTimestamp - 20 * 60 * 1000);

        // Get all metrics
        mockMvc.perform(get("/api/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lineId").exists())
                .andExpect(jsonPath("$[0].avg").exists())
                .andExpect(jsonPath("$[0].max").exists())
                .andExpect(jsonPath("$[0].min").exists());
    }
    
	@Test
	void testConcurrentAddsAcrossMultipleLineIds() throws Exception {
		int addPerLine = 2;
		CountDownLatch latch = new CountDownLatch(4 * addPerLine);
		
		List<Thread> threads = new ArrayList<>();
		for (int lineId = 10; lineId < 41L; lineId+=10) {
		    final Long currentLineId = (long) lineId;
		    Runnable addTask = () -> {
		        for (int i = 0; i < addPerLine; i++) {
		            try {
		                addMeasurement(currentLineId, Math.random() * 300, currentTimestamp);
		            } catch (Exception e) {
		                e.printStackTrace();
		            } finally {
		                latch.countDown();
		            }
		        }
		    };
		    threads.add(new Thread(addTask));
		}
		
		// Start all threads
		for (Thread thread : threads) {
		    thread.start();
		}
		
		// Wait for all threads to complete
		latch.await();
		
		// Verify metrics for each line ID
		for (int lineId = 10; lineId < 41L; lineId+=10) {
		    mockMvc.perform(get("/api/metrics/" + lineId))
		            .andExpect(status().isOk())
		            .andExpect(jsonPath("$.avg").exists())
		            .andExpect(jsonPath("$.max").exists())
		            .andExpect(jsonPath("$.min").exists());
		}
	}

    // Helper method to add measurement via mock request
    private void addMeasurement(Long lineId, double speed, long timestamp) throws Exception {
        MeasurementRequest request = new MeasurementRequest();
        request.setLineId(lineId);
        request.setSpeed(speed);
        request.setTimestamp(timestamp);

        mockMvc.perform(post("/api/linespeed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
    
    @Test
    void testRemoveOldEntriesSheduled_Success() throws Exception {
        // Add some measurements
        addMeasurement(80L, 100.0, currentTimestamp - 59 * 60 * 1000 - 56 * 1000); // 59 minutes 56 seconds ago
       
        //fetch metrics, the measurement shoud return
        mockMvc.perform(get("/api/metrics/80"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avg").value(100.0))
        .andExpect(jsonPath("$.max").value(100.0))
        .andExpect(jsonPath("$.min").value(100.0));
        
        //sleep 5 seconds to allow the schedule to run and remove the old entry
        TimeUnit.SECONDS.sleep(5);
        // Fetch metrics, should receive a not found because the measure was removed by schedule
        mockMvc.perform(get("/api/metrics/80"))
            .andExpect(status().isNotFound());

    }
}
