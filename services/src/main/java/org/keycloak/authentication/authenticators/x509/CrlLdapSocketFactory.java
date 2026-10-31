package org.keycloak.authentication.authenticators.x509;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.keycloak.connections.httpclient.SafeInputStream;

/**
 * Limits the bytes read and the time spent when JNDI loads a CRL from LDAP. JNDI only accepts a class name and calls
 * the static {@link #getDefault()}, so the configured instance is bound to the current thread with {@link #bind()}.
 */
public class CrlLdapSocketFactory extends SocketFactory {

    public static final String JNDI_SOCKET_FACTORY_PROPERTY = "java.naming.ldap.factory.socket";

    private static final ThreadLocal<CrlLdapSocketFactory> BOUND_FACTORY = new ThreadLocal<>();

    private final long maxBytes;
    private final int connectTimeoutMillis;
    private final int socketTimeoutMillis;
    private final SSLSocketFactory sslSocketFactory;
    private volatile IOException failure;

    /**
     * Non-positive timeouts disable the timeout, a {@code null} SSL socket factory means plain LDAP.
     */
    public CrlLdapSocketFactory(long maxBytes, long connectTimeoutMillis, long socketTimeoutMillis, SSLSocketFactory sslSocketFactory) {
        this.maxBytes = maxBytes;
        this.connectTimeoutMillis = toTimeout(connectTimeoutMillis);
        this.socketTimeoutMillis = toTimeout(socketTimeoutMillis);
        this.sslSocketFactory = sslSocketFactory;
    }

    /**
     * Binds this factory to the current thread until the returned binding is closed.
     */
    public Binding bind() {
        BOUND_FACTORY.set(this);
        return BOUND_FACTORY::remove;
    }

    /**
     * The error that closed the connection, if any. JNDI itself only reports that the connection was closed.
     */
    public IOException getFailure() {
        return failure;
    }

    /**
     * Called by JNDI. Fails if no factory is bound, so a socket without limits is never created.
     */
    public static SocketFactory getDefault() {
        CrlLdapSocketFactory factory = BOUND_FACTORY.get();
        if (factory == null) {
            throw new IllegalStateException("No CRL LDAP socket factory bound to the current thread");
        }
        return factory;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return connect(new InetSocketAddress(host, port), null, host);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return connect(new InetSocketAddress(host, port), new InetSocketAddress(localHost, localPort), host);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return connect(new InetSocketAddress(host, port), null, host.getHostAddress());
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return connect(new InetSocketAddress(address, port), new InetSocketAddress(localAddress, localPort), address.getHostAddress());
    }

    private Socket connect(InetSocketAddress endpoint, InetSocketAddress localAddress, String host) throws IOException {
        BoundedSocket socket = new BoundedSocket();
        try {
            socket.setSoTimeout(socketTimeoutMillis);
            if (localAddress != null) {
                socket.bind(localAddress);
            }
            socket.connect(endpoint, connectTimeoutMillis);
            if (sslSocketFactory != null) {
                // TLS on top of the bounded socket, so the limit also applies to ldaps
                return sslSocketFactory.createSocket(socket, host, endpoint.getPort(), true);
            }
            return socket;
        } catch (IOException | RuntimeException e) {
            socket.close();
            throw e;
        }
    }

    private static int toTimeout(long millis) {
        return millis <= 0 ? 0 : (int) Math.min(millis, Integer.MAX_VALUE);
    }

    private IOException record(IOException e) {
        if (failure == null) {
            failure = e;
        }
        return e;
    }

    public interface Binding extends AutoCloseable {

        @Override
        void close();
    }

    private final class BoundedSocket extends Socket {

        private InputStream inputStream;

        @Override
        public synchronized InputStream getInputStream() throws IOException {
            if (inputStream == null) {
                inputStream = new FilterInputStream(new SafeInputStream(super.getInputStream(), maxBytes)) {
                    @Override
                    public int read() throws IOException {
                        try {
                            return super.read();
                        } catch (IOException e) {
                            throw record(e);
                        }
                    }

                    @Override
                    public int read(byte[] b, int off, int len) throws IOException {
                        try {
                            return super.read(b, off, len);
                        } catch (IOException e) {
                            throw record(e);
                        }
                    }
                };
            }
            return inputStream;
        }
    }
}
