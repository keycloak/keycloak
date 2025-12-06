package org.keycloak.testframework.https;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;

import org.apache.http.ssl.SSLContextBuilder;
import org.jboss.logging.Logger;

public class ManagedClientCertificates extends AbstractManagedCertificates {

    public ManagedClientCertificates(CertificatesConfigBuilder configBuilder) throws ManagedCertificatesException {
        super(configBuilder);
    }

    @Override
    public Logger getLogger() {
        return Logger.getLogger(ManagedClientCertificates.class);
    }

    @Override
    public SSLContext getClientSSLContext() {
        try {
            return SSLContextBuilder.create()
                    .loadTrustMaterial(clientsTruststorePath.toFile(), password)
                    .loadKeyMaterial(serverKeyStore, password)
                    .build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new ManagedCertificatesException(e);
        } catch (UnrecoverableKeyException | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
