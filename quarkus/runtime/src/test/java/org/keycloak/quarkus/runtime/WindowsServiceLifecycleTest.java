package org.keycloak.quarkus.runtime;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class WindowsServiceLifecycleTest {

    private void requireWindows() {
        assumeTrue(Environment.isWindows());
    }

    @Test
    public void startupCancellationIsRecognizedThroughQuarkusWrappers() {
        assertTrue(WindowsServiceLifecycle.isStartupCancellation(
                new IllegalStateException("Failed to start", new WindowsServiceLifecycle.StartupCancelledException())));
        assertFalse(WindowsServiceLifecycle.isStartupCancellation(new IllegalStateException("Database unavailable")));
        assertFalse(WindowsServiceLifecycle.isStartupCancellation(null));
    }

    @Test
    public void stopSignalsOnlyTheMatchingService() throws Exception {
        requireWindows();
        String firstId = UUID.randomUUID().toString();
        String secondId = UUID.randomUUID().toString();
        CountDownLatch first = new CountDownLatch(1);
        CountDownLatch second = new CountDownLatch(1);
        AtomicInteger firstExitCode = new AtomicInteger(-1);
        AtomicInteger secondExitCode = new AtomicInteger(-1);
        try (var firstService = WindowsServiceLifecycle.listen(firstId, code -> {
            firstExitCode.set(code);
            first.countDown();
        }); var secondService = WindowsServiceLifecycle.listen(secondId, code -> {
            secondExitCode.set(code);
            second.countDown();
        })) {
            firstService.started();
            secondService.started();
            assertTrue(WindowsServiceLifecycle.requestStop(firstId));
            assertTrue(first.await(5, TimeUnit.SECONDS));
            assertEquals(0, firstExitCode.get());
            assertEquals(1, second.getCount());
            assertTrue(WindowsServiceLifecycle.requestStop(secondId));
            assertTrue(second.await(5, TimeUnit.SECONDS));
            assertEquals(0, secondExitCode.get());
        }
        assertFalse(WindowsServiceLifecycle.requestStop(firstId));
        assertFalse(WindowsServiceLifecycle.requestStop(secondId));
    }

    @Test
    public void closingDoesNotRequestShutdownAndAllowsRestart() throws Exception {
        requireWindows();
        String id = UUID.randomUUID().toString();
        CountDownLatch shutdown = new CountDownLatch(1);
        try (var service = WindowsServiceLifecycle.listen(id, code -> shutdown.countDown())) {
            assertThrows(IllegalStateException.class, () -> WindowsServiceLifecycle.listen(id, code -> shutdown.countDown()));
        }
        assertEquals(1, shutdown.getCount());
        AtomicInteger exitCode = new AtomicInteger(-1);
        try (var restarted = WindowsServiceLifecycle.listen(id, code -> {
            exitCode.set(code);
            shutdown.countDown();
        })) {
            restarted.started();
            assertTrue(WindowsServiceLifecycle.requestStop(id));
            assertTrue(shutdown.await(5, TimeUnit.SECONDS));
            assertEquals(0, exitCode.get());
        }
    }

    @Test(timeout = 5000)
    public void closePreservesInterruptAndReleasesTheEvent() {
        requireWindows();
        String id = UUID.randomUUID().toString();
        AtomicInteger exitCode = new AtomicInteger(-1);
        var service = WindowsServiceLifecycle.listen(id, exitCode::set);
        Thread.currentThread().interrupt();
        try {
            service.close();
            assertTrue(Thread.currentThread().isInterrupted());
            service.close();
        } finally {
            Thread.interrupted();
        }
        assertEquals(-1, exitCode.get());
        assertFalse(WindowsServiceLifecycle.requestStop(id));
    }

    @Test
    public void listenerFailureRequestsFailedShutdown() throws Exception {
        requireWindows();
        String id = UUID.randomUUID().toString();
        CountDownLatch failedShutdown = new CountDownLatch(1);
        AtomicInteger exitCode = new AtomicInteger(-1);
        try (var service = WindowsServiceLifecycle.listen(id, code -> {
            if (code == 0) {
                throw new IllegalStateException("Simulated listener failure");
            }
            exitCode.set(code);
            failedShutdown.countDown();
        })) {
            service.started();
            assertTrue(WindowsServiceLifecycle.requestStop(id));
            assertTrue(failedShutdown.await(5, TimeUnit.SECONDS));
            assertEquals(1, exitCode.get());
        }
        assertFalse(WindowsServiceLifecycle.requestStop(id));
    }

    @Test
    public void stopWaitsForStartup() throws Exception {
        CountDownLatch firstAttempt = new CountDownLatch(1);
        AtomicBoolean registered = new AtomicBoolean();
        var executor = Executors.newSingleThreadExecutor();
        try {
            var request = executor.submit(() -> {
                WindowsServiceLifecycle.awaitEvent(() -> {
                    boolean signaled = registered.get();
                    firstAttempt.countDown();
                    return signaled;
                });
                return null;
            });
            assertTrue(firstAttempt.await(5, TimeUnit.SECONDS));
            assertFalse(request.isDone());
            registered.set(true);
            request.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void waitingForStartupCanBeCancelledExternally() throws Exception {
        CountDownLatch firstAttempt = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var request = executor.submit(() -> {
                WindowsServiceLifecycle.awaitEvent(() -> {
                    firstAttempt.countDown();
                    return false;
                });
                return null;
            });
            assertTrue(firstAttempt.await(5, TimeUnit.SECONDS));
            assertFalse(request.isDone());
            assertTrue(request.cancel(true));
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void waitingForStartupCanBeInterrupted() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(InterruptedException.class,
                    () -> WindowsServiceLifecycle.awaitEvent(() -> false));
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void stopBeforeEventRegistrationRequestsShutdown() throws Exception {
        requireWindows();
        String id = UUID.randomUUID().toString();
        CountDownLatch firstAttempt = new CountDownLatch(1);
        CountDownLatch shutdown = new CountDownLatch(1);
        AtomicInteger exitCode = new AtomicInteger(-1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var request = executor.submit(() -> {
                WindowsServiceLifecycle.awaitEvent(() -> {
                    boolean signaled = WindowsServiceLifecycle.requestStop(id);
                    firstAttempt.countDown();
                    return signaled;
                });
                return null;
            });
            assertTrue(firstAttempt.await(5, TimeUnit.SECONDS));
            assertFalse(request.isDone());
            try (var service = WindowsServiceLifecycle.listen(id, code -> {
                exitCode.set(code);
                shutdown.countDown();
            })) {
                service.started();
                request.get(5, TimeUnit.SECONDS);
                assertTrue(shutdown.await(5, TimeUnit.SECONDS));
                assertEquals(0, exitCode.get());
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
        assertFalse(WindowsServiceLifecycle.requestStop(id));
    }

    @Test
    public void stopDuringStartupCancelsBeforeServerRuns() {
        requireWindows();
        String id = UUID.randomUUID().toString();
        AtomicInteger exitCode = new AtomicInteger(-1);
        try (var service = WindowsServiceLifecycle.listen(id, exitCode::set)) {
            assertTrue(WindowsServiceLifecycle.requestStop(id));
            assertThrows(WindowsServiceLifecycle.StartupCancelledException.class, service::checkStartupStop);
            assertEquals(-1, exitCode.get());
        }
        assertFalse(WindowsServiceLifecycle.requestStop(id));
    }

    @Test
    public void stopAfterStartupCheckpointIsDeliveredOnlyOnce() throws Exception {
        requireWindows();
        String id = UUID.randomUUID().toString();
        CountDownLatch shutdown = new CountDownLatch(1);
        AtomicInteger requests = new AtomicInteger();
        CountDownLatch received = new CountDownLatch(1);
        Logger logger = Logger.getLogger(WindowsServiceLifecycle.class.getName());
        Level previousLevel = logger.getLevel();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if ("Windows service stop requested during startup".equals(record.getMessage())) {
                    received.countDown();
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        try (var service = WindowsServiceLifecycle.listen(id, code -> {
            requests.incrementAndGet();
            shutdown.countDown();
        })) {
            service.checkStartupStop();
            assertTrue(WindowsServiceLifecycle.requestStop(id));
            assertTrue("Stop request should be retained before startup completes", received.await(5, TimeUnit.SECONDS));
            assertEquals(1, shutdown.getCount());
            service.started();
            service.started();
            assertTrue(shutdown.await(5, TimeUnit.SECONDS));
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(previousLevel);
        }
        assertEquals(1, requests.get());
    }
}
