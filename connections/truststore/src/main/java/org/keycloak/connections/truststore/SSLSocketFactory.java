package org.keycloak.connections.truststore;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;


/**
 * Using this class is ugly, but it is the only way to push our truststore to the default LDAP client implementation.
 * <p>
 * This SSLSocketFactory can only use truststore configured by TruststoreProvider after the ProviderFactory was
 * initialized using standard Spi load / init mechanism. That will only happen if "truststore" provider is configured
 * in keycloak-server.json.
 * <p>
 * If TruststoreProvider is not available this SSLSocketFactory will delegate all operations to javax.net.ssl.SSLSocketFactory.getDefault().
 *
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */

public class SSLSocketFactory extends javax.net.ssl.SSLSocketFactory {

    private static final Logger log = Logger.getLogger(SSLSocketFactory.class);

    private static SSLSocketFactory instance;

    private final javax.net.ssl.SSLSocketFactory sslsf;

    private SSLSocketFactory() {

        TruststoreProvider provider = TruststoreProviderSingleton.get();
        javax.net.ssl.SSLSocketFactory sf = null;
        if (provider != null) {
            sf = new JSSETruststoreConfigurator(provider).getSSLSocketFactory();
        }

        if (sf == null) {
            log.info("No truststore provider found - using default SSLSocketFactory");
            sf = (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault();
        }

        sslsf = sf;
    }

    public static synchronized SSLSocketFactory getDefault() {
        if (instance == null) {
            instance = new SSLSocketFactory();
        }
        return instance;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return sslsf.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return sslsf.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return sslsf.createSocket(socket, host, port, autoClose);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return sslsf.createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return sslsf.createSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return sslsf.createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return sslsf.createSocket(address, port, localAddress, localPort);
    }
}
