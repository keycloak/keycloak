package org.keycloak.tests.model;

import org.keycloak.cluster.infinispan.ClockSkewDetectionTask;
import org.keycloak.cluster.infinispan.ClockSkewTimestampSample;
import org.keycloak.cluster.infinispan.InfinispanClusterProvider;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.infinispan.CacheType;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.infinispan.Cache;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;

@KeycloakIntegrationTest(config = ClockSkewDetectionTest.ServerConfig.class)
public class ClockSkewDetectionTest {

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @Test
    public void testClockSkewDetectionTaskPublishesSample() {
        TaskPublishResult result = runOnServer.fetch(session -> {
            InfinispanConnectionProvider ispnConnections = session.getProvider(InfinispanConnectionProvider.class);
            String nodeName = ispnConnections.getNodeInfo().nodeName();
            Cache<String, Object> workCache = ispnConnections.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

            // Record initial cache size
            int initialSize = workCache.size();

            // Create and run the task
            ClockSkewDetectionTask task = new ClockSkewDetectionTask(nodeName, 180);
            task.run(session);

            // Return results for assertion
            return new TaskPublishResult(initialSize, workCache.size(), task.getTaskName(), nodeName);
        }, TaskPublishResult.class);

        // Verify a sample was published
        assertThat("Clock skew sample should be published to cache",
                result.getFinalSize(), greaterThan(result.getInitialSize()));

        // Verify task name includes node name
        assertThat("Task name should contain node name",
                result.getTaskName(), containsString(result.getNodeName()));
    }

    @Test
    public void testClockSkewSampleSerialization() {
        SerializationTestResult result = runOnServer.fetch(session -> {
            InfinispanConnectionProvider ispnConnections = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, Object> workCache = ispnConnections.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

            // Create a sample
            ClockSkewTimestampSample sample = new ClockSkewTimestampSample("test-node", 1234567890L);

            // Store it in cache
            String key = "test-clock-skew-sample";
            workCache.put(key, sample);

            // Retrieve it
            Object retrieved = workCache.get(key);

            // Clean up
            workCache.remove(key);

            // Return results for assertion
            boolean wasRetrieved = retrieved != null;
            boolean correctType = retrieved instanceof ClockSkewTimestampSample;
            String retrievedNodeName = null;
            Long retrievedTimestamp = null;

            if (correctType) {
                ClockSkewTimestampSample deserializedSample = (ClockSkewTimestampSample) retrieved;
                retrievedNodeName = deserializedSample.nodeName();
                retrievedTimestamp = deserializedSample.timestampMillis();
            }

            return new SerializationTestResult(wasRetrieved, correctType, retrievedNodeName, retrievedTimestamp);
        }, SerializationTestResult.class);

        // Verify it was serialized and deserialized correctly
        assertThat("Sample should be retrievable from cache", result.isWasRetrieved(), equalTo(true));
        assertThat("Sample should be of correct type", result.isCorrectType(), equalTo(true));
        assertThat("Node name should match", result.getNodeName(), equalTo("test-node"));
        assertThat("Timestamp should match", result.getTimestamp(), equalTo(1234567890L));
    }

    @Test
    public void testClockSkewDetectionWithRemoteNodeAhead() {
        // Run the test logic on the server and return the captured logs
        CapturedLogs result = runOnServer.fetch(session -> {
            InfinispanConnectionProvider ispnConnections = session.getProvider(InfinispanConnectionProvider.class);
            String localNodeName = ispnConnections.getNodeInfo().nodeName();
            Cache<String, Object> workCache = ispnConnections.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

            // Set up log capturing - this will catch log messages during the test
            List<String> logs = new ArrayList<>();
            TestLogHandler logHandler = new TestLogHandler(logs);

            // Attach handler to InfinispanClusterProvider's logger to capture warnings
            Logger ispnLogger = Logger.getLogger(InfinispanClusterProvider.class.getName());
            ispnLogger.addHandler(logHandler);

            try {
                // STEP 1: Run the REAL task to publish a timestamp (uses production code)
                ClockSkewDetectionTask task = new ClockSkewDetectionTask(localNodeName, 180);
                task.run(session);

                // STEP 2: Find the sample that was just published by the real task
                String publishedKey = null;
                ClockSkewTimestampSample publishedSample = null;
                for (String key : workCache.keySet()) {
                    if (key.startsWith(ClockSkewDetectionTask.CLOCK_SKEW_SAMPLE_KEY_PREFIX)) {
                        Object value = workCache.get(key);
                        if (value instanceof ClockSkewTimestampSample) {
                            publishedKey = key;
                            publishedSample = (ClockSkewTimestampSample) value;
                            break;
                        }
                    }
                }

                // Verify the task published something (throw exception if not, which will fail the test)
                if (publishedSample == null) {
                    throw new AssertionError("Task should have published a sample");
                }

                // STEP 3: Modify the sample to simulate a remote node that is ahead
                // Change node name to pretend it's from another server
                // Set timestamp 200ms ahead (exceeds the 100ms threshold)
                long currentTime = Time.currentTimeMillis();
                ClockSkewTimestampSample modifiedSample = new ClockSkewTimestampSample(
                    "remote-node-ahead",  // Different node name
                    currentTime + 200     // 200ms ahead - exceeds 100ms threshold
                );

                // Put the modified sample back - this triggers the cache listener
                String testKey = ClockSkewDetectionTask.CLOCK_SKEW_SAMPLE_KEY_PREFIX + "test-remote-ahead";
                workCache.put(testKey, modifiedSample);

                // Give the cache listener time to process the event and log the warning
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Clean up
                if (publishedKey != null) {
                    workCache.remove(publishedKey);
                }
                workCache.remove(testKey);

                // Return the captured logs to the test method
                return new CapturedLogs(logs);

            } finally {
                // Always remove the log handler to clean up
                ispnLogger.removeHandler(logHandler);
            }
        }, CapturedLogs.class);

        // STEP 4: VERIFY the warning was actually logged (assertions run in test JVM, not server)
        assertThat("Logs should have been captured", result, notNullValue());

        List<String> capturedLogs = result.getLogs();
        boolean foundClockSkewWarning = capturedLogs.stream()
            .anyMatch(msg -> msg.contains("Clock skew detected"));
        boolean foundRemoteNodeName = capturedLogs.stream()
            .anyMatch(msg -> msg.contains("remote-node-ahead"));

        assertThat("Warning should be logged for clock skew", foundClockSkewWarning, equalTo(true));
        assertThat("Warning should mention the remote node name", foundRemoteNodeName, equalTo(true));
    }

    @Test
    public void testClockSkewDetectionWithRemoteNodeBehind() {
        runOnServer.run(session -> {
            InfinispanConnectionProvider ispnConnections = session.getProvider(InfinispanConnectionProvider.class);
            String localNodeName = ispnConnections.getNodeInfo().nodeName();
            Cache<String, Object> workCache = ispnConnections.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

            // Simulate a remote node with clock 200ms behind
            long localTime = Time.currentTimeMillis();
            long remoteTimeBehind = localTime - 200;

            ClockSkewTimestampSample remoteSample = new ClockSkewTimestampSample("remote-node", remoteTimeBehind);

            // Put the sample in the cache - this should NOT trigger a warning (negative skew is tolerable)
            String key = ClockSkewDetectionTask.CLOCK_SKEW_SAMPLE_KEY_PREFIX + "test-behind";
            workCache.put(key, remoteSample);

            // Give the listener time to process
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Clean up
            workCache.remove(key);
        });

        // No warning should be logged - remote node being behind is not a security issue
    }

    @Test
    public void testClockSkewDetectionWithinThreshold() {
        runOnServer.run(session -> {
            InfinispanConnectionProvider ispnConnections = session.getProvider(InfinispanConnectionProvider.class);
            String localNodeName = ispnConnections.getNodeInfo().nodeName();
            Cache<String, Object> workCache = ispnConnections.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

            // Simulate a remote node with clock 50ms ahead (within default 100ms threshold)
            long localTime = Time.currentTimeMillis();
            long remoteTimeSlightlyAhead = localTime + 50;

            ClockSkewTimestampSample remoteSample = new ClockSkewTimestampSample("remote-node", remoteTimeSlightlyAhead);

            // Put the sample in the cache - this should NOT trigger a warning (within threshold)
            String key = ClockSkewDetectionTask.CLOCK_SKEW_SAMPLE_KEY_PREFIX + "test-within-threshold";
            workCache.put(key, remoteSample);

            // Give the listener time to process
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Clean up
            workCache.remove(key);
        });

        // No warning should be logged - skew is within acceptable threshold
    }

    @Test
    public void testClockSkewDetectionIgnoresSelfSamples() {
        runOnServer.run(session -> {
            InfinispanConnectionProvider ispnConnections = session.getProvider(InfinispanConnectionProvider.class);
            String localNodeName = ispnConnections.getNodeInfo().nodeName();
            Cache<String, Object> workCache = ispnConnections.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

            // Create a sample from the same node with time far in the future
            long futureTime = Time.currentTimeMillis() + 10000;
            ClockSkewTimestampSample selfSample = new ClockSkewTimestampSample(localNodeName, futureTime);

            // Put the sample in the cache - this should be IGNORED (same node)
            String key = ClockSkewDetectionTask.CLOCK_SKEW_SAMPLE_KEY_PREFIX + "test-self";
            workCache.put(key, selfSample);

            // Give the listener time to process
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Clean up
            workCache.remove(key);
        });

        // No warning should be logged - samples from self are ignored
    }

    @Test
    public void testClockSkewDetectionTaskValidation() {
        ValidationTestResult result = runOnServer.fetch(session -> {
            String nodeName = session.getProvider(InfinispanConnectionProvider.class).getNodeInfo().nodeName();

            String nullNodeErrorMsg = null;
            String zeroTtlErrorMsg = null;
            String negativeTtlErrorMsg = null;
            boolean validTaskCreated = false;
            String validTaskName = null;

            // Test that null node name is rejected
            try {
                new ClockSkewDetectionTask(null, 180);
                throw new AssertionError("Should have thrown NullPointerException for null node name");
            } catch (NullPointerException e) {
                nullNodeErrorMsg = e.getMessage();
            }

            // Test that zero TTL is rejected
            try {
                new ClockSkewDetectionTask(nodeName, 0);
                throw new AssertionError("Should have thrown IllegalArgumentException for zero TTL");
            } catch (IllegalArgumentException e) {
                zeroTtlErrorMsg = e.getMessage();
            }

            // Test that negative TTL is rejected
            try {
                new ClockSkewDetectionTask(nodeName, -100);
                throw new AssertionError("Should have thrown IllegalArgumentException for negative TTL");
            } catch (IllegalArgumentException e) {
                negativeTtlErrorMsg = e.getMessage();
            }

            // Test that valid parameters work
            ClockSkewDetectionTask validTask = new ClockSkewDetectionTask(nodeName, 180);
            if (validTask != null) {
                validTaskCreated = true;
                validTaskName = validTask.getTaskName();
            }

            return new ValidationTestResult(nullNodeErrorMsg, zeroTtlErrorMsg, negativeTtlErrorMsg,
                    validTaskCreated, validTaskName);
        }, ValidationTestResult.class);

        assertThat("Error message should mention nodeName",
                result.getNullNodeErrorMsg(), containsString("nodeName"));
        assertThat("Error message should mention sampleTtlSeconds",
                result.getZeroTtlErrorMsg(), containsString("sampleTtlSeconds"));
        assertThat("Error message should mention sampleTtlSeconds",
                result.getNegativeTtlErrorMsg(), containsString("sampleTtlSeconds"));
        assertThat("Valid task should be created", result.isValidTaskCreated(), equalTo(true));
        assertThat("Task name should be set", result.getValidTaskName(), notNullValue());
    }

    /**
     * Simple log handler for capturing log messages during tests.
     * Must be static to avoid capturing outer class reference (serialization issue).
     */
    static class TestLogHandler extends Handler {
        private final List<String> capturedLogs;

        TestLogHandler(List<String> capturedLogs) {
            this.capturedLogs = capturedLogs;
        }

        @Override
        public void publish(LogRecord record) {
            if (record != null && record.getMessage() != null) {
                capturedLogs.add(record.getMessage());
            }
        }

        @Override
        public void flush() {
            // No-op for in-memory capture
        }

        @Override
        public void close() {
            // No-op
        }
    }

    /**
     * Wrapper class for transporting captured logs from server to test.
     * Must be serializable for use with runOnServer.fetch().
     */
    static class CapturedLogs {
        private List<String> logs = new ArrayList<>();

        // Default constructor required for Jackson deserialization
        public CapturedLogs() {
        }

        public CapturedLogs(List<String> logs) {
            this.logs = new ArrayList<>(logs);
        }

        public List<String> getLogs() {
            return logs;
        }

        public void setLogs(List<String> logs) {
            this.logs = logs;
        }
    }

    /**
     * Wrapper for task publish test results.
     */
    static class TaskPublishResult {
        private int initialSize;
        private int finalSize;
        private String taskName;
        private String nodeName;

        public TaskPublishResult() {
        }

        public TaskPublishResult(int initialSize, int finalSize, String taskName, String nodeName) {
            this.initialSize = initialSize;
            this.finalSize = finalSize;
            this.taskName = taskName;
            this.nodeName = nodeName;
        }

        public int getInitialSize() {
            return initialSize;
        }

        public void setInitialSize(int initialSize) {
            this.initialSize = initialSize;
        }

        public int getFinalSize() {
            return finalSize;
        }

        public void setFinalSize(int finalSize) {
            this.finalSize = finalSize;
        }

        public String getTaskName() {
            return taskName;
        }

        public void setTaskName(String taskName) {
            this.taskName = taskName;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }
    }

    /**
     * Wrapper for serialization test results.
     */
    static class SerializationTestResult {
        private boolean wasRetrieved;
        private boolean correctType;
        private String nodeName;
        private Long timestamp;

        public SerializationTestResult() {
        }

        public SerializationTestResult(boolean wasRetrieved, boolean correctType, String nodeName, Long timestamp) {
            this.wasRetrieved = wasRetrieved;
            this.correctType = correctType;
            this.nodeName = nodeName;
            this.timestamp = timestamp;
        }

        public boolean isWasRetrieved() {
            return wasRetrieved;
        }

        public void setWasRetrieved(boolean wasRetrieved) {
            this.wasRetrieved = wasRetrieved;
        }

        public boolean isCorrectType() {
            return correctType;
        }

        public void setCorrectType(boolean correctType) {
            this.correctType = correctType;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }
    }

    /**
     * Wrapper for validation test results.
     */
    static class ValidationTestResult {
        private String nullNodeErrorMsg;
        private String zeroTtlErrorMsg;
        private String negativeTtlErrorMsg;
        private boolean validTaskCreated;
        private String validTaskName;

        public ValidationTestResult() {
        }

        public ValidationTestResult(String nullNodeErrorMsg, String zeroTtlErrorMsg, String negativeTtlErrorMsg,
                                     boolean validTaskCreated, String validTaskName) {
            this.nullNodeErrorMsg = nullNodeErrorMsg;
            this.zeroTtlErrorMsg = zeroTtlErrorMsg;
            this.negativeTtlErrorMsg = negativeTtlErrorMsg;
            this.validTaskCreated = validTaskCreated;
            this.validTaskName = validTaskName;
        }

        public String getNullNodeErrorMsg() {
            return nullNodeErrorMsg;
        }

        public void setNullNodeErrorMsg(String nullNodeErrorMsg) {
            this.nullNodeErrorMsg = nullNodeErrorMsg;
        }

        public String getZeroTtlErrorMsg() {
            return zeroTtlErrorMsg;
        }

        public void setZeroTtlErrorMsg(String zeroTtlErrorMsg) {
            this.zeroTtlErrorMsg = zeroTtlErrorMsg;
        }

        public String getNegativeTtlErrorMsg() {
            return negativeTtlErrorMsg;
        }

        public void setNegativeTtlErrorMsg(String negativeTtlErrorMsg) {
            this.negativeTtlErrorMsg = negativeTtlErrorMsg;
        }

        public boolean isValidTaskCreated() {
            return validTaskCreated;
        }

        public void setValidTaskCreated(boolean validTaskCreated) {
            this.validTaskCreated = validTaskCreated;
        }

        public String getValidTaskName() {
            return validTaskName;
        }

        public void setValidTaskName(String validTaskName) {
            this.validTaskName = validTaskName;
        }
    }

    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.cache(CacheType.ISPN);
        }
    }
}
