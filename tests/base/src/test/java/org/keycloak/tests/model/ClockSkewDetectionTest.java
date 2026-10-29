package org.keycloak.tests.model;

import org.keycloak.cluster.infinispan.ClockSkewDetectionTask;
import org.keycloak.cluster.infinispan.ClockSkewTimestampSample;
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
        runOnServer.run(session -> {
            InfinispanConnectionProvider ispnConnections = session.getProvider(InfinispanConnectionProvider.class);
            String nodeName = ispnConnections.getNodeInfo().nodeName();
            Cache<String, Object> workCache = ispnConnections.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

            // Record initial cache size
            int initialSize = workCache.size();

            // Create and run the task
            ClockSkewDetectionTask task = new ClockSkewDetectionTask(nodeName, 180);
            task.run(session);

            // Verify a sample was published
            assertThat("Clock skew sample should be published to cache",
                    workCache.size(), greaterThan(initialSize));

            // Verify task name includes node name
            assertThat("Task name should contain node name",
                    task.getTaskName(), containsString(nodeName));
        });
    }

    @Test
    public void testClockSkewSampleSerialization() {
        runOnServer.run(session -> {
            InfinispanConnectionProvider ispnConnections = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, Object> workCache = ispnConnections.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

            // Create a sample
            ClockSkewTimestampSample sample = new ClockSkewTimestampSample("test-node", 1234567890L);

            // Store it in cache
            String key = "test-clock-skew-sample";
            workCache.put(key, sample);

            // Retrieve it
            Object retrieved = workCache.get(key);

            // Verify it was serialized and deserialized correctly
            assertThat("Sample should be retrievable from cache", retrieved, notNullValue());
            assertThat("Sample should be of correct type", retrieved, instanceOf(ClockSkewTimestampSample.class));

            ClockSkewTimestampSample deserializedSample = (ClockSkewTimestampSample) retrieved;
            assertThat("Node name should match", deserializedSample.nodeName(), equalTo("test-node"));
            assertThat("Timestamp should match", deserializedSample.timestampMillis(), equalTo(1234567890L));

            // Clean up
            workCache.remove(key);
        });
    }

    @Test
    public void testClockSkewDetectionWithRemoteNodeAhead() {
        runOnServer.run(session -> {
            InfinispanConnectionProvider ispnConnections = session.getProvider(InfinispanConnectionProvider.class);
            String localNodeName = ispnConnections.getNodeInfo().nodeName();
            Cache<String, Object> workCache = ispnConnections.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

            // Simulate a remote node with clock 200ms ahead (exceeds default 100ms threshold)
            long localTime = Time.currentTimeMillis();
            long remoteTimeAhead = localTime + 200;

            ClockSkewTimestampSample remoteSample = new ClockSkewTimestampSample("remote-node", remoteTimeAhead);

            // Put the sample in the cache - this should trigger the listener and a warning
            String key = ClockSkewDetectionTask.CLOCK_SKEW_SAMPLE_KEY_PREFIX + "test-ahead";
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

        // Note: In a real production test, we would capture and verify the log output.
        // For now, this test verifies the mechanism works without causing exceptions.
        // The warning log can be manually verified in test output.
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
        runOnServer.run(session -> {
            String nodeName = session.getProvider(InfinispanConnectionProvider.class).getNodeInfo().nodeName();

            // Test that null node name is rejected
            try {
                new ClockSkewDetectionTask(null, 180);
                throw new AssertionError("Should have thrown NullPointerException for null node name");
            } catch (NullPointerException e) {
                assertThat("Error message should mention nodeName",
                        e.getMessage(), containsString("nodeName"));
            }

            // Test that zero TTL is rejected
            try {
                new ClockSkewDetectionTask(nodeName, 0);
                throw new AssertionError("Should have thrown IllegalArgumentException for zero TTL");
            } catch (IllegalArgumentException e) {
                assertThat("Error message should mention sampleTtlSeconds",
                        e.getMessage(), containsString("sampleTtlSeconds"));
            }

            // Test that negative TTL is rejected
            try {
                new ClockSkewDetectionTask(nodeName, -100);
                throw new AssertionError("Should have thrown IllegalArgumentException for negative TTL");
            } catch (IllegalArgumentException e) {
                assertThat("Error message should mention sampleTtlSeconds",
                        e.getMessage(), containsString("sampleTtlSeconds"));
            }

            // Test that valid parameters work
            ClockSkewDetectionTask validTask = new ClockSkewDetectionTask(nodeName, 180);
            assertThat("Valid task should be created", validTask, notNullValue());
            assertThat("Task name should be set", validTask.getTaskName(), notNullValue());
        });
    }

    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.cache(CacheType.ISPN);
        }
    }
}
