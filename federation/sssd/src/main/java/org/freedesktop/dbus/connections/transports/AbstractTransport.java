package org.freedesktop.dbus.connections.transports;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.Optional;
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
import org.freedesktop.dbus.messages.MessageFactory;
import org.freedesktop.dbus.messages.constants.ArgumentType;
import org.freedesktop.dbus.spi.message.IMessageReader;
import org.freedesktop.dbus.spi.message.IMessageWriter;
import org.freedesktop.dbus.spi.message.ISocketProvider;
import org.freedesktop.dbus.spi.message.InputStreamMessageReader;
import org.freedesktop.dbus.spi.message.OutputStreamMessageWriter;
import org.freedesktop.dbus.utils.IThrowingSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all transport types.
 *
 * @author hypfvieh
 * @since v3.2.0 - 2019-02-08
 */
public abstract class AbstractTransport implements Closeable {

    private static final AtomicLong              TRANSPORT_ID_GENERATOR = new AtomicLong(0);

    private final ServiceLoader<ISocketProvider> spiLoader              = ServiceLoader.load(ISocketProvider.class, AbstractTransport.class.getClassLoader());

    private final Logger                         logger                 = LoggerFactory.getLogger(getClass());
    private final BusAddress                     address;

    private TransportConnection                  transportConnection;
    private boolean                              fileDescriptorSupported;

    private final long                           transportId            = TRANSPORT_ID_GENERATOR.incrementAndGet();

    private final TransportConfig                config;
    private final MessageFactory                 messageFactory;

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
        messageFactory = new MessageFactory(config.getEndianess());
    }

    /**
     * Write a message to the underlying socket.
     *
     * @param _msg message to write
     * @throws IOException on write error or if output was already closed or null
     */
    public void writeMessage(Message _msg) throws IOException {
        if (!fileDescriptorSupported && ArgumentType.FILEDESCRIPTOR == _msg.getType()) {
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
     *
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
     * Abstract method implemented by concrete sub classes to establish a connection.
     * @return socket channel connected to DBus server
     *
     * @throws IOException when connection fails
     */
    protected abstract SocketChannel connectImpl() throws IOException;

    /**
     * Method to accept new incoming listening connections.<br>
     * This is the place where {@code accept()} is called on the server socket created by {@link #bindImpl()}.<br>
     * Therefore this method will block until a client is connected.
     *
     * @return newly connected client socket
     *
     * @throws IOException when connection fails
     *
     * @since 5.0.0 - 2023-10-20
     */
    protected abstract SocketChannel acceptImpl() throws IOException;

    /**
     * Method called to prepare listening for connections.<br>
     * This is usually the place where the {@code ServerSocketChannel} is created and {@code bind()} is called.
     *
     * @throws IOException when connection fails
     *
     * @since 5.0.0 - 2023-10-20
     */
    protected abstract void bindImpl() throws IOException;

    /**
     * Method which is called to close a transport.<br>
     * Should be used to close all sockets and/or serversockets.
     *
     * @throws IOException when something fails while closing transport
     * @since 5.0.0 - 2023-10-20
     */
    protected abstract void closeTransport() throws IOException;

    /**
     * Status of the server socket if this transport is configured to be a server connection.<br>
     * Must be false if {@link #bindImpl()} was not called.
     *
     * @return boolean
     * @since 5.0.0 - 2023-10-20
     */
    protected abstract boolean isBound();

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
        transportConnection = internalConnect(this::connectImpl);
        return transportConnection.getChannel();
    }

    /**
     * True if this transport connection is a listening (server) connection.
     *
     * @return boolean
     */
    public final boolean isListening() {
        return getAddress().isListeningSocket();
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
     * @return {@link TransportConnection} containing created {@link SocketChannel} and
     *         {@link IMessageReader}/{@link IMessageWriter}
     * @throws IOException if connection fails
     */
    public final TransportConnection listen() throws IOException {
        if (!getAddress().isListeningSocket()) {
            throw new InvalidBusAddressException("Cannot listen on client connection address (try use connect() instead)");
        }

        if (!isBound()) {
            bindImpl();
            runCallback(config.getAfterBindCallback());
        }

        transportConnection = internalConnect(this::acceptImpl);
        return transportConnection;
    }

    /**
     * Method used internally to do the actual connect.
     *
     * @param _channelProvider listen or connect call which will return a socket channel
     * @return TransportConnection
     * @throws IOException when channel provider could not create a SocketChannel
     */
    private TransportConnection internalConnect(IThrowingSupplier<SocketChannel, IOException> _channelProvider) throws IOException {
        runCallback(config.getPreConnectCallback());
        SocketChannel channel = _channelProvider.get();

        authenticate(channel);
        return createInputOutput(channel);
    }

    /**
     * Set a callback which will be called right before the connection will be established to the transport.
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
     * Setup message reader/writer. Will look for SPI provider first, if none is found default implementation is used.
     * The default implementation does not support file descriptor passing!
     *
     * @param _socket socket to use
     * @param _messageFactory message factory
     * @return TransportConnection with configured socket channel, reader and writer
     */
    private TransportConnection createInputOutput(SocketChannel _socket) {
        IMessageReader reader = null;
        IMessageWriter writer = null;
        ISocketProvider providerImpl = null;
        try {
            for (ISocketProvider provider : spiLoader) {
                logger.debug("Found ISocketProvider {}", provider);

                provider.setFileDescriptorSupport(hasFileDescriptorSupport() && fileDescriptorSupported);
                reader = provider.createReader(_socket);
                writer = provider.createWriter(_socket);
                if (reader != null && writer != null) {
                    logger.debug("Using ISocketProvider {}", provider);
                    providerImpl = provider;
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
            fileDescriptorSupported = false; // internal implementation does not support file descriptors even if server
                                             // allows it
        }

        return new TransportConnection(messageFactory, _socket, providerImpl, writer, reader);
    }

    /**
     * Runs a callback if not null.
     * @param _callback callback to execute
     */
    private void runCallback(Consumer<AbstractTransport> _callback) {
        Optional.ofNullable(_callback).ifPresent(c -> c.accept(this));
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
     * Returns the current configuration used for SASL authentication.<br>
     *
     * @return SaslConfig, never null
     */
    protected SaslConfig getSaslConfig() {
        return config.getSaslConfig();
    }

    /**
     * Returns the current transport connection.
     *
     * @return TransportConnection, null if not connected yet
     */
    public TransportConnection getTransportConnection() {
        return transportConnection;
    }

    /**
     * Currently configured message factory.
     *
     * @return factory
     */
    public MessageFactory getMessageFactory() {
        return messageFactory;
    }

    public TransportConfig getTransportConfig() {
        return config;
    }

    public boolean isFileDescriptorSupported() {
        return fileDescriptorSupported;
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
    public final void close() throws IOException {
        if (transportConnection != null) {
            transportConnection.close();
            transportConnection = null;
        }

        getLogger().debug("Disconnecting Transport: {}", this);
        closeTransport();
    }

}
