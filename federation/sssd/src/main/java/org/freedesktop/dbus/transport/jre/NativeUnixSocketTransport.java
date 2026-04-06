package org.freedesktop.dbus.transport.jre;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;

import org.freedesktop.dbus.connections.SASL;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.transports.AbstractUnixTransport;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;

/**
 * Transport type representing a transport connection to a unix socket.
 * This implementation uses features of Java 16+ to connect to a unix
 * socket without a 3rd party library.
 * <p>
 * Please note: The functionality of the native unix sockets in Java are
 * limited. 'Side-channel' communication (e.g. passing file descriptors)
 * is not possible (unlike using jnr-unix socket + dbus-java-nativefd).
 * <br><br>
 * Also using 'abstract' sockets is not possible when using this native implementation.
 * <br>
 * In most cases this implementation should suit our needs.
 * If it does not fit for you, use jnr-unixsocket instead.
 *
 * @author hypfvieh
 * @since v4.0.0 - 2021-09-01
 */
public class NativeUnixSocketTransport extends AbstractUnixTransport {
    private final UnixDomainSocketAddress unixSocketAddress;
    private SocketChannel                 socket;
    private ServerSocketChannel           serverSocket;

    NativeUnixSocketTransport(UnixBusAddress _address, TransportConfig _config) throws TransportConfigurationException {
        super(_address, _config);

        if (_address.hasPath()) {
            unixSocketAddress = UnixDomainSocketAddress.of(_address.getPath());
        } else {
            throw new TransportConfigurationException("Native unix socket url has to specify 'path'");
        }

        getSaslConfig().setAuthMode(SASL.AUTH_EXTERNAL);
    }

    @Override
    protected boolean hasFileDescriptorSupport() {
        return false; // See JEP-380: File descriptor not supported by native implementation (yet)
    }

    /**
     * Establish a connection to DBus using unix sockets.
     *
     * @throws IOException on error
     */
    @Override
    public SocketChannel connectImpl() throws IOException {
        if (getAddress().isListeningSocket()) {
            throw new IOException("Connect connect to a listening socket (use listenImpl() instead)");
        } else {
            socket = SocketChannel.open(unixSocketAddress);
        }

        socket.configureBlocking(true);

        return socket;
    }

    @Override
    protected void bindImpl() throws IOException {
        if (!getAddress().isListeningSocket()) {
            throw new IOException("Cannot listen on a client connection (use connectImpl() instead)");
        }

        if (!isBound()) {
            serverSocket = ServerSocketChannel.open(StandardProtocolFamily.UNIX).bind(unixSocketAddress);
            serverSocket.configureBlocking(true);
        }
    }

    @Override
    public SocketChannel acceptImpl() throws IOException {

        socket = serverSocket.accept();
        socket.configureBlocking(true);

        return socket;
    }

    @Override
    protected boolean isBound() {
        return serverSocket != null && serverSocket.isOpen();
    }

    @Override
    protected void closeTransport() throws IOException {
        if (socket != null && socket.isOpen()) {
            socket.close();
        }

        if (serverSocket != null && serverSocket.isOpen()) {
            serverSocket.close();
            // remove socket file if server
            Files.deleteIfExists(unixSocketAddress.getPath());
        }

    }

    @Override
    public int getUid(SocketChannel _sock) throws IOException {
        return NativeUnixSocketHelper.getUid(_sock);
    }
}
