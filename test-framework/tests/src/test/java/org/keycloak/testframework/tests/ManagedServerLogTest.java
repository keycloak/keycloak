package org.keycloak.testframework.tests;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class ManagedServerLogTest {

    private static final String MARKER = "ManagedServerLogTest-marker";

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void multiLineRecordIsLoggedAsSingleRecordWithOriginalLevel() throws Exception {
        String selected = Config.getSelectedSupplier(KeycloakServer.class);
        Assumptions.assumeTrue(selected == null || "distribution".equals(selected), "Only applies to the distribution server");

        BlockingQueue<LogRecord> records = new LinkedBlockingQueue<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getMessage() != null && record.getMessage().contains(MARKER)) {
                    records.add(record);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        java.util.logging.Logger managedLogger = java.util.logging.Logger.getLogger("managed.keycloak");
        managedLogger.addHandler(handler);
        try {
            runOnServer.run(session -> org.jboss.logging.Logger.getLogger("test.managed.log")
                    .error("ManagedServerLogTest-marker message", new RuntimeException("outer", new IllegalStateException("root cause"))));

            LogRecord record = records.poll(30, TimeUnit.SECONDS);
            Assertions.assertNotNull(record, "Record from managed server not received");
            Assertions.assertEquals(Level.SEVERE, record.getLevel());
            String message = record.getMessage();
            Assertions.assertTrue(message.contains("java.lang.RuntimeException: outer"), message);
            Assertions.assertTrue(message.contains("Caused by: java.lang.IllegalStateException: root cause"), message);
            Assertions.assertTrue(message.contains("\n\tat "), message);
            Assertions.assertNull(records.poll(1, TimeUnit.SECONDS), "Record should have been logged only once");
        } finally {
            managedLogger.removeHandler(handler);
        }
    }

}
