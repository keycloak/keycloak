package org.freedesktop.dbus.connections.transports;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.SASL;
import org.freedesktop.dbus.connections.config.SaslConfig;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.exceptions.AuthenticationException;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.InvalidBusAddressException;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.spi.message.IMessageReader;
import org.freedesktop.dbus.spi.message.IMessageWriter;
import org.freedesktop.dbus.spi.message.ISocketProvider;
import org.freedesktop.dbus.spi.message.InputStreamMessageReader;
import org.freedesktop.dbus.spi.message.OutputStreamMessageWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all transport types.
 *
 * @author hypfvieh
 * @since v3.2.0 - 2019-02-08
 */
public abstract class AbstractTransport implements Closeable {

    private static final AtomicLong TRANSPORT_ID_GENERATOR = new AtomicLong(0);

    private final ServiceLoader<ISocketProvider> spiLoader = ServiceLoader.load(ISocketProvider.class);

    private final Logger                         logger    = LoggerFactory.getLogger(getClass());
    private final BusAddress                     address;

    private TransportConnection                  transportConnection;
    private boolean                              fileDescriptorSupported;

    private final long                           transportId = TRANSPORT_ID_GENERATOR.incrementAndGet();

    private final TransportConfig                config;

    protected AbstractTransport(BusAddress _address, TransportConfig _config) {
        address = Objects.requireNonNull(_address, "BusAddress required");
        config = Objects.requireNonNull(_config, "Config required");

        if (_address.isListeningSocket()) {
            config.getSaslConfig().setMode(SASL.SaslMode.SERVER);
        } else {
            config.getSaslConfig().setMode(SASL.SaslMode.CLIENT);
        }
        config.getSaslConfig().setGuid(address.getGuid());
        config.getSaslConfig().setFileDescriptorSupport(hasFileDescriptorSupport());
    }

    /**
     * Write a message to the underlying socket.
     *
     * @param _msg message to write
     * @throws IOException on write error or if output was already closed or null
     */
    public void writeMessage(Message _msg) throws IOException {
        if (!fileDescriptorSupported && Message.ArgumentType.FILEDESCRIPTOR == _msg.getType()) {
            throw new IllegalArgumentException("File descriptors are not supported!");
        }
        if (transportConnection.getWriter() != null && !transportConnection.getWriter().isClosed()) {
            transportConnection.getWriter().writeMessage(_msg);
        } else {
            throw new IOException("OutputWriter already closed or null");
        }
    }

    /**
     * Read a message from the underlying socket.
     *
     * @return read message, maybe null
     * @throws IOException when input already close or null
     * @throws DBusException when message could not be converted to a DBus message
     */
    public Message readMessage() throws IOException, DBusException {
        if (transportConnection.getReader() != null && !transportConnection.getReader().isClosed()) {
            return transportConnection.getReader().readMessage();
        }
        throw new IOException("InputReader already closed or null");
    }

    /**
     * Returns true if inputReader and outputWriter are not yet closed.
     * @return boolean
     */
    public synchronized boolean isConnected() {
        return transportConnection != null
                && transportConnection.getWriter() != null && !transportConnection.getWriter().isClosed()
                && transportConnection.getReader() != null && !transportConnection.getReader().isClosed();
    }

    /**
     * Method to indicate if passing of file descriptors is allowed.
     *
     * @return true to allow FD passing, false otherwise
     */
    protected abstract boolean hasFileDescriptorSupport();

    /**
     * Return true if the transport supports 'abstract' sockets.
     * @return true if abstract sockets supported, false otherwise
     *
     * @deprecated Is no longer used and will be removed
     */
    @Deprecated(forRemoval = true, since = "4.2.0 - 2022-07-18")
    protected abstract boolean isAbstractAllowed();

    /**
     * Abstract method implemented by concrete sub classes to establish a connection
     * using whatever transport type (e.g. TCP/Unix socket).
     * @throws IOException when connection fails
     */
    protected abstract SocketChannel connectImpl() throws IOException;

    /**
     * Establish connection on created transport.<br>
     * <p>
     * This method can only be used for <b>non-listening</b> connections.<br>
     * Trying to use this with listening addresses will throw an {@link InvalidBusAddressException}.
     * </p>
     *
     * @return {@link SocketChannel} of the created connection
     * @throws IOException if connection fails
     */
    public final SocketChannel connect() throws IOException {
        if (getAddress().isListeningSocket()) {
            throw new InvalidBusAddressException("Cannot connect when using listening address (try use listen() instead)");
        }
        transportConnection = internalConnect();
        return transportConnection.getChannel();
    }

    /**
     * Start listening on created transport.<br>
     * <p>
     * This method can only be used for <b>listening</b> connections.<br>
     * Trying to use this with non-listening addresses will throw an {@link InvalidBusAddressException}.
     * </p>
     * <p>
     * Will return the {@link TransportConnection} as soon as a client connects.<br>
     * Therefore this method should be called in a loop to accept multiple clients
     * </p>
     *
     * @return {@link TransportConnection} containing created {@link SocketChannel} and {@link IMessageReader}/{@link IMessageWriter}
     * @throws IOException if connection fails
     */
    public final TransportConnection listen() throws IOException {
        if (!getAddress().isListeningSocket()) {
            throw new InvalidBusAddressException("Cannot listen on client connection address (try use connect() instead)");
        }
        transportConnection = internalConnect();
        return transportConnection;
    }

    private TransportConnection internalConnect() throws IOException {
        if (config.getPreConnectCallback() != null) {
            config.getPreConnectCallback().accept(this);
        }
        SocketChannel channel = connectImpl();
        authenticate(channel);
        return createInputOutput(channel);
    }

    /**
     * Set a callback which will be called right before the connection will
     * be established to the transport.
     *
     * @param _run runnable to execute, null if no callback should be executed
     *
     * @since 4.2.0 - 2022-07-20
     */
    public void setPreConnectCallback(Consumer<AbstractTransport> _run) {
        config.setPreConnectCallback(_run);
    }

    /**
     * Helper method to authenticate to DBus using SASL.
     *
     * @param _sock socketchannel
     * @throws IOException on any error
     */
    private void authenticate(SocketChannel _sock) throws IOException {
        SASL sasl = new SASL(config.getSaslConfig());
        try {
            if (!sasl.auth(_sock, this)) {
                throw new AuthenticationException("Failed to authenticate");
            }
        } catch (IOException _ex) {
            _sock.close();
            throw _ex;
        }
        fileDescriptorSupported = sasl.isFileDescriptorSupported(); // false if server does not support file descriptors
    }

    /**
     * Setup message reader/writer.
     * Will look for SPI provider first, if none is found default implementation is used.
     * The default implementation does not support file descriptor passing!
     *
     * @param _socket socket to use
     * @return TransportConnection with configured socket channel, reader and writer
     */
    private TransportConnection createInputOutput(SocketChannel _socket) {
        IMessageReader reader = null;
        IMessageWriter writer = null;
        try {
            for (ISocketProvider provider : spiLoader) {
                logger.debug("Found ISocketProvider {}", provider);

                provider.setFileDescriptorSupport(hasFileDescriptorSupport() && fileDescriptorSupported);
                reader = provider.createReader(_socket);
                writer = provider.createWriter(_socket);
                if (reader != null && writer != null) {
                    logger.debug("Using ISocketProvider {}", provider);
                    break;
                }
            }
        } catch (ServiceConfigurationError _ex) {
            logger.error("Could not initialize service provider", _ex);
        } catch (IOException _ex) {
            logger.error("Could not initialize alternative message reader/writer", _ex);
        }

        if (reader == null || writer == null) {
            logger.debug("No alternative ISocketProvider found, using built-in implementation");
            reader = new InputStreamMessageReader(_socket);
            writer = new OutputStreamMessageWriter(_socket);
            fileDescriptorSupported = false; // internal implementation does not support file descriptors even if server allows it
        }

        return new TransportConnection(_socket, writer, reader);
    }

    /**
     * Returns the {@link BusAddress} used for this transport.
     *
     * @return BusAddress, never null
     */
    protected BusAddress getAddress() {
        return address;
    }

    /**
     * Get the logger in subclasses.
     *
     * @return Logger, never null
     */
    protected Logger getLogger() {
        return logger;
    }

    /**
     * Returns the current configuration used for SASL authentication.
     *
     * @return SaslConfig, never null
     */
    protected SaslConfig getSaslConfig() {
        return config.getSaslConfig();
    }

    /**
     * Set the SASL authentication mode.
     *
     * @deprecated please use {@link #getSaslConfig()}.getAuthMode() instead
     */
    @Deprecated(since = "4.2.0 - 2022-07-22", forRemoval = true)
    protected int getSaslAuthMode() {
        return getSaslConfig().getAuthMode();
    }

    /**
     * Set the SASL authentication mode.
     *
     * @deprecated please use {@link #getSaslConfig()}.getMode() instead
     */
    @Deprecated(since = "4.2.0 - 2022-07-22", forRemoval = true)
    protected SASL.SaslMode getSaslMode() {
        return getSaslConfig().getMode();
    }
    /**
     * Set the SASL mode (server or client).
     *
     * @param _saslMode mode to set
     * @deprecated please use {@link #getSaslConfig()}.setMode(int) instead
     */
    @Deprecated(since = "4.2.0 - 2022-07-22", forRemoval = true)
    protected void setSaslMode(SASL.SaslMode _saslMode) {
        getSaslConfig().setMode(_saslMode);
    }

    /**
     * Set the SASL authentication mode.
     *
     * @param _mode mode to set
     * @deprecated please use {@link #getSaslConfig()}.setSaslAuthMode(int) instead
     */
    @Deprecated(since = "4.2.0 - 2022-07-22", forRemoval = true)
    protected void setSaslAuthMode(int _mode) {
        getSaslConfig().setAuthMode(_mode);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(" [id=")
                .append(transportId)
                .append(", ");

        if (transportConnection != null) {
            sb.append("connectionId=")
                    .append(transportConnection.getId())
                    .append(", ");
        }

        sb.append("address=")
                .append(address)
                .append("]");

        return sb.toString();
    }

    @Override
    public void close() throws IOException {
        if (transportConnection != null) {
            transportConnection.close();
            transportConnection = null;
        }
    }

}
