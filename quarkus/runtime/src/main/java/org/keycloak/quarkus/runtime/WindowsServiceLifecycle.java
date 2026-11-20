package org.keycloak.quarkus.runtime;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.win32.StdCallLibrary;
import io.quarkus.runtime.Quarkus;
import org.jboss.logging.Logger;

/** A service-account-owned Windows event used by Procrun's stop process. */
public final class WindowsServiceLifecycle implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(WindowsServiceLifecycle.class);
    public static final String SERVICE_ID_ENV = "KC_WINDOWS_SERVICE_ID";
    private static final int EVENT_MODIFY_STATE = 0x0002;
    private static final int WAIT_OBJECT_0 = 0;
    private static final int WAIT_TIMEOUT = 258;
    private static final int WAIT_FAILED = -1;
    private static final int WAIT_INTERVAL_MILLIS = 250;
    private static final int ERROR_FILE_NOT_FOUND = 2;
    private static final int ERROR_ALREADY_EXISTS = 183;

    private final Pointer event;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Thread listener;
    private final IntConsumer shutdown;
    private boolean started;
    private int requestedExitCode = -1;

    private WindowsServiceLifecycle(String serviceId, IntConsumer shutdown) {
        this.shutdown = shutdown;
        WString name = eventName(serviceId);
        // Keep the request visible to both the listener and startup cancellation checkpoints.
        event = Kernel32.INSTANCE.CreateEventW(null, true, false, name);
        if (event == null) {
            throw nativeError("CreateEvent");
        }
        if (Native.getLastError() == ERROR_ALREADY_EXISTS) {
            closeHandle(event);
            throw new IllegalStateException("Windows service shutdown event already exists");
        }
        try {
            listener = new Thread(this::awaitStop, "keycloak-windows-service-shutdown");
            listener.setDaemon(true);
            listener.setUncaughtExceptionHandler((thread, failure) -> {
                LOG.error("Windows service shutdown listener failed", failure);
                requestExit(1);
            });
            listener.start();
        } catch (RuntimeException | Error e) {
            closeHandle(event);
            throw e;
        }
    }

    public static WindowsServiceLifecycle start() {
        String serviceId = System.getenv(SERVICE_ID_ENV);
        return Environment.isWindows() && serviceId != null ? listen(serviceId, Quarkus::asyncExit) : null;
    }

    static WindowsServiceLifecycle listen(String serviceId, IntConsumer shutdown) {
        return new WindowsServiceLifecycle(serviceId, shutdown);
    }

    private void awaitStop() {
        while (!closed.get()) {
            // A finite wait lets close() stop the listener even if signaling an event fails.
            int result = Kernel32.INSTANCE.WaitForSingleObject(event, WAIT_INTERVAL_MILLIS);
            if (result == WAIT_OBJECT_0) {
                if (!closed.get()) {
                    requestExit(0);
                }
                return;
            }
            if (result == WAIT_FAILED) {
                throw nativeError("WaitForSingleObject");
            }
            if (result != WAIT_TIMEOUT) {
                throw new IllegalStateException("Unexpected Windows event wait result: " + result);
            }
        }
    }

    private synchronized void requestExit(int exitCode) {
        if (requestedExitCode == exitCode || requestedExitCode == 1) {
            return;
        }
        requestedExitCode = exitCode;
        if (started) {
            shutdown.accept(exitCode);
        } else if (exitCode == 0) {
            LOG.info("Windows service stop requested during startup");
        }
    }

    public synchronized void started() {
        if (started) {
            return;
        }
        started = true;
        if (requestedExitCode != -1) {
            shutdown.accept(requestedExitCode);
        }
    }

    public synchronized void checkStartupStop() {
        // Poll as well as listening: a stop must not be missed if the listener is slow to run.
        int result = Kernel32.INSTANCE.WaitForSingleObject(event, 0);
        if (result == WAIT_OBJECT_0) {
            requestExit(0);
        } else if (result == WAIT_FAILED) {
            throw nativeError("WaitForSingleObject");
        } else if (result != WAIT_TIMEOUT) {
            throw new IllegalStateException("Unexpected Windows event wait result: " + result);
        }
        if (!started && requestedExitCode == 0) {
            throw new StartupCancelledException();
        }
    }

    static boolean isStartupCancellation(Throwable cause) {
        while (cause != null) {
            if (cause instanceof StartupCancelledException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    static final class StartupCancelledException extends RuntimeException {
        StartupCancelledException() {
            super("Windows service startup cancelled");
        }
    }

    public static boolean requestStop(String serviceId) {
        WString name = eventName(serviceId);
        Pointer event = Kernel32.INSTANCE.OpenEventW(EVENT_MODIFY_STATE, false, name);
        if (event == null) {
            if (Native.getLastError() == ERROR_FILE_NOT_FOUND) {
                return false;
            }
            throw nativeError("OpenEvent");
        }
        try {
            if (!Kernel32.INSTANCE.SetEvent(event)) {
                throw nativeError("SetEvent");
            }
            return true;
        } finally {
            closeHandle(event);
        }
    }

    public static void requestStopWhenReady(String serviceId) throws InterruptedException {
        awaitEvent(() -> requestStop(serviceId));
    }

    static void awaitEvent(BooleanSupplier signal) throws InterruptedException {
        // Procrun owns the total stop deadline, including the time to launch this JVM.
        // A separate timer here would start late and would not reserve shutdown time.
        while (true) {
            if (Thread.interrupted()) {
                throw new InterruptedException("Interrupted while waiting for the Windows service shutdown event");
            }
            if (signal.getAsBoolean()) {
                return;
            }
            // The server may still be building or starting and has not created its event yet.
            TimeUnit.MILLISECONDS.sleep(WAIT_INTERVAL_MILLIS);
        }
    }

    private static WString eventName(String serviceId) {
        // Only accept installer-generated IDs, never an arbitrary Windows object name.
        return new WString("Local\\KeycloakShutdown-" + UUID.fromString(serviceId));
    }

    private static IllegalStateException nativeError(String operation) {
        return new IllegalStateException(operation + " failed with Windows error " + Native.getLastError());
    }

    private static void closeHandle(Pointer handle) {
        if (!Kernel32.INSTANCE.CloseHandle(handle)) {
            LOG.warnf("CloseHandle failed with Windows error %d", Native.getLastError());
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        boolean interrupted = false;
        while (listener.isAlive()) {
            try {
                listener.join();
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        // Windows forbids closing a handle while a native wait is pending.
        closeHandle(event);
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

        Pointer CreateEventW(Pointer attributes, boolean manualReset, boolean initialState, WString name);
        Pointer OpenEventW(int access, boolean inherit, WString name);
        boolean SetEvent(Pointer event);
        int WaitForSingleObject(Pointer handle, int timeout);
        boolean CloseHandle(Pointer handle);
    }
}
