package org.keycloak.tests.model;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@KeycloakIntegrationTest(config = ClockSkewDetectionTest.ServerConfig.class)
public class ClockSkewDetectionTest {

    // The server loads this test class (lambda body) and its nested classes from the test JVM, so its package must be permitted
    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @Test
    public void testClockSkewDetectionWithRemoteNodeAhead() {
        runOnServer.run(session -> {
            InfinispanConnectionProvider ispnConnections = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, Object> workCache = ispnConnections.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

            List<String> logs = new CopyOnWriteArrayList<>();
            TestLogHandler logHandler = new TestLogHandler(logs);

            Logger ispnLogger = Logger.getLogger(InfinispanClusterProvider.class.getName());
            ispnLogger.addHandler(logHandler);

            try {
                // Simulate a remote node 200ms behind, which should not be logged
                workCache.put(ClockSkewDetectionTask.CLOCK_SKEW_SAMPLE_KEY_PREFIX + "test-remote-behind", new ClockSkewTimestampSample("remote-node-behind", Time.currentTimeMillis() - 200));

                // Simulate a remote node 200ms ahead, which exceeds the 100ms threshold
                workCache.put(ClockSkewDetectionTask.CLOCK_SKEW_SAMPLE_KEY_PREFIX + "test-remote-ahead", new ClockSkewTimestampSample("remote-node-ahead", Time.currentTimeMillis() + 200));

                // The cache listener logs asynchronously, so wait for the warning
                await().atMost(Duration.ofSeconds(5))
                        .until(() -> logs.stream().anyMatch(msg -> msg.contains("remote-node-ahead")));

                long warningCount = logs.stream()
                        .filter(msg -> msg.contains("Clock skew detected") && msg.contains("remote-node-ahead"))
                        .count();

                assertThat("Warning for the remote node should be logged exactly once", warningCount, equalTo(1L));
            } finally {
                ispnLogger.removeHandler(logHandler);
            }
        });
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

    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.cache(CacheType.ISPN);
        }
    }
}
