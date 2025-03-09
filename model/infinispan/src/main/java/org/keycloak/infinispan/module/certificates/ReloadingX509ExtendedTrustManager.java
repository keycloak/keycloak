package org.keycloak.infinispan.module.certificates;

import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;

/**
 * A {@link X509ExtendedTrustManager} that allows to update the trusted certificate at runtime.
 */
class ReloadingX509ExtendedTrustManager extends X509ExtendedTrustManager {

    private volatile X509ExtendedTrustManager delegate;
    private volatile Runnable onException;

    public ReloadingX509ExtendedTrustManager(X509ExtendedTrustManager delegate) {
        this.delegate = Objects.requireNonNull(delegate);
        this.onException = () -> {
        };
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        try {
            delegate.checkClientTrusted(chain, authType, engine);
        } catch (CertificateException e) {
            onException.run();
            throw e;
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        try {
            delegate.checkClientTrusted(chain, authType, socket);
        } catch (CertificateException e) {
            onException.run();
            throw e;
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        try {

            delegate.checkServerTrusted(chain, authType, engine);
        } catch (CertificateException e) {
            onException.run();
            throw e;
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        try {

            delegate.checkServerTrusted(chain, authType, socket);
        } catch (CertificateException e) {
            onException.run();
            throw e;
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {

            delegate.checkClientTrusted(chain, authType);
        } catch (CertificateException e) {
            onException.run();
            throw e;
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {
            delegate.checkServerTrusted(chain, authType);
        } catch (CertificateException e) {
            onException.run();
            throw e;
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return delegate.getAcceptedIssuers();
    }

    public void setExceptionHandler(Runnable runnable) {
        this.onException = Objects.requireNonNullElse(runnable, () -> {
        });
    }

    public void reload(X509ExtendedTrustManager trustManager) {
        delegate = Objects.requireNonNull(trustManager);
    }
}
