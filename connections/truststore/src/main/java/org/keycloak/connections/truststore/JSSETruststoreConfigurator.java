package org.keycloak.connections.truststore;

import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class JSSETruststoreConfigurator {

    private TruststoreProvider provider;
    private volatile javax.net.ssl.SSLSocketFactory sslFactory;
    private volatile TrustManager[] tm;

    public JSSETruststoreConfigurator(KeycloakSession session) {
        KeycloakSessionFactory factory = session.getKeycloakSessionFactory();
        TruststoreProviderFactory truststoreFactory = (TruststoreProviderFactory) factory.getProviderFactory(TruststoreProvider.class, "file");

        provider = truststoreFactory.create(session);
        if (provider != null && provider.getTruststore() == null) {
            provider = null;
        }
    }

    public JSSETruststoreConfigurator(TruststoreProvider provider) {
        this.provider = provider;
    }

    public javax.net.ssl.SSLSocketFactory getSSLSocketFactory() {
        if (provider == null) {
            return null;
        }

        if (sslFactory == null) {
            synchronized(this) {
                if (sslFactory == null) {
                    try {
                        SSLContext sslctx = SSLContext.getInstance("TLS");
                        sslctx.init(null, getTrustManagers(), null);
                        sslFactory = sslctx.getSocketFactory();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to initialize SSLContext: ", e);
                    }
                }
            }
        }
        return sslFactory;
    }

    public TrustManager[] getTrustManagers() {
        if (provider == null) {
            return null;
        }

        if (tm == null) {
            synchronized (this) {
                if (tm == null) {
                    TrustManagerFactory tmf = null;
                    try {
                        tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                        tmf.init(provider.getTruststore());
                        tm = tmf.getTrustManagers();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to initialize TrustManager: ", e);
                    }
                }
            }
        }
        return tm;
    }

    public HostnameVerifier getHostnameVerifier() {
        if (provider == null) {
            return null;
        }

        HostnameVerificationPolicy policy = provider.getPolicy();
        switch (policy) {
            case ANY:
                return new HostnameVerifier() {
                    @Override
                    public boolean verify(String s, SSLSession sslSession) {
                        return true;
                    }
                };
            case WILDCARD:
                return new BrowserCompatHostnameVerifier();
            case STRICT:
                return new StrictHostnameVerifier();
            default:
                throw new IllegalStateException("Unknown policy: " + policy.name());
        }
    }

    public TruststoreProvider getProvider() {
        return provider;
    }
}
