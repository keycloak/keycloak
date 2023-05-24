package org.freedesktop.dbus.bin;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder.SaslAuthMode;
import org.freedesktop.dbus.connections.transports.TransportConnection;
import org.freedesktop.dbus.exceptions.AuthenticationException;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.SocketClosedException;
import org.freedesktop.dbus.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple DBusDaemon implementation to use if no DBusDaemon is running on the OS level.
 */
public class EmbeddedDBusDaemon implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedDBusDaemon.class);

    private final BusAddress address;

    private DBusDaemon daemon;

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean connectionReady = new AtomicBoolean(false);

    private SaslAuthMode saslAuthMode;

    private String unixSocketFileOwner;

    private String unixSocketFileGroup;

    private PosixFilePermission[] unixSocketFilePermissions;

    public EmbeddedDBusDaemon(BusAddress _address) {
        // create copy of address so manipulation happens later does not interfere with our instance
        address = BusAddress.of(Objects.requireNonNull(_address, "Address required"));
    }

    public EmbeddedDBusDaemon(String _address) throws DBusException {
        this(BusAddress.of(_address));
    }

    /**
     * Shutdown the running DBusDaemon instance.
     */
    @Override
    public synchronized void close() throws IOException {
        closed.set(true);
        connectionReady.set(false);
        if (daemon != null) {
            daemon.close();
            daemon = null;
        }
    }

    /**
     * Run the DBusDaemon in foreground.
     * <p>
     * This is a blocking operation.
     */
    public void startInForeground() {
        try {
            startListening();
        } catch (IOException | DBusException _ex) {
            if (!closed.get()) {
                throw new RuntimeException(_ex);
            }
        }
    }

    /**
     * Start the DBusDaemon in background and returns immediately.
     * <p>
     * This method may return before the background thread is ready.
     * To ensure the the background thread is running on return use {@link #startInBackgroundAndWait(long)}.
     */
    public void startInBackground() {
        Thread thread = new Thread(this::startInForeground);
        String threadName = address.toString().replaceAll("^([^,]+),.+", "$1");

        thread.setName("EmbeddedDBusDaemon-" + threadName);
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((th, ex) -> LOGGER.error("Got uncaught exception", ex));
        thread.start();
    }

    /**
     * Starts the DBusDaemon in background.
     * <p>
     * Will wait up to the given period of milliseconds for the background thread to get ready.
     * If given wait time exceeded, a {@link RuntimeException} is thrown.
     *
     * @param _maxWaitMillis maximum wait time in milliseconds
     */
    public void startInBackgroundAndWait(long _maxWaitMillis) {
        startInBackground();
        Util.waitFor("EmbeddedDbusDaemon", this::isRunning, _maxWaitMillis, 100);
    }

    /**
     * Whether the DBusDaemon is still running.
     *
     * @return true if running, false otherwise
     */
    public synchronized boolean isRunning() {
        return connectionReady.get() && daemon != null && daemon.isRunning();
    }

    /**
     * The currently configured {@link SaslAuthMode}.
     * When null is returned, the {@link SaslAuthMode} of the transport provider is used.
     *
     * @return {@link SaslAuthMode} or null
     */
    public SaslAuthMode getSaslAuthMode() {
        return saslAuthMode;
    }

    /**
     * Use this to override the default authentication mode which would
     * be used by the transport based on the {@link BusAddress}.
     *
     * @param _saslAuthMode auth mode, null to use default
     */
    public void setSaslAuthMode(SaslAuthMode _saslAuthMode) {
        saslAuthMode = _saslAuthMode;
    }

    /**
     * The file owner for the created unix socket.<br>
     * Ignored if TCP is used.<br>
     * <br>
     * Will only work if currently running JVM process user
     * has suitable permissions to change the owner.
     *
     * @param _owner owner to set
     */
    public void setUnixSocketOwner(String _owner) {
        unixSocketFileOwner = _owner;
    }

    /**
     * The file group for the created unix socket.<br>
     * Ignored if TCP is used.<br>
     * <br>
     * Will only work if currently running JVM process user
     * has suitable permissions to change the group.
     *
     * @param _group group to set
     */
    public void setUnixSocketGroup(String _group) {
        unixSocketFileGroup = _group;
    }

    /**
     * The file permissions for the created unix socket.<br>
     * Ignored if TCP is used or if the OS is Windows.<br>
     * <br>
     * Will only work if currently running JVM process user
     * has suitable permissions to change the permissions.
     *
     * @param _permissions permissions to set
     */
    public void setUnixSocketPermissions(PosixFilePermission... _permissions) {
        unixSocketFilePermissions = _permissions;
    }

    private synchronized void setDaemonAndStart(AbstractTransport _transport) {
        daemon = new DBusDaemon(_transport);
        daemon.start();
    }

    /**
     * Start listening for incoming connections.
     * <p>
     * Will throw {@link IllegalArgumentException} if a unsupported transport is used.
     *
     * @throws IOException when connection fails
     * @throws DBusException when the provided bus address is wrong
     */
    private void startListening() throws IOException, DBusException {
        if (!TransportBuilder.getRegisteredBusTypes().contains(address.getBusType())) {
            throw new IllegalArgumentException("Unknown or unsupported address type: " + address.getType());
        }

        LOGGER.debug("About to initialize transport on: {}", address);
        try (AbstractTransport transport = TransportBuilder.create(address).configure()
                .withUnixSocketFileOwner(unixSocketFileOwner)
                .withUnixSocketFileGroup(unixSocketFileGroup)
                .withUnixSocketFilePermissions(unixSocketFilePermissions)
                .withAutoConnect(false)
                .configureSasl().withAuthMode(getSaslAuthMode()).back()
                .back()
                .build()) {

            setDaemonAndStart(transport);

            // use tail-controlled loop so we at least try to get a client connection once
            do {
                try {
                    LOGGER.debug("Begin listening to: {}", transport);
                    connectionReady.set(true);
                    TransportConnection s = transport.listen();
                    daemon.addSock(s);
                } catch (AuthenticationException _ex) {
                    LOGGER.error("Authentication failed", _ex);
                } catch (SocketClosedException _ex) {
                    LOGGER.debug("Connection closed", _ex);
                }

            } while (daemon.isRunning());

        }
    }
}
