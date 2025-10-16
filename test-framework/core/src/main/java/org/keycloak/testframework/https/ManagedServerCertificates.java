package org.keycloak.testframework.https;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;

import org.apache.http.ssl.SSLContextBuilder;
import org.jboss.logging.Logger;

public class ManagedServerCertificates extends AbstractManagedCertificates {

    public ManagedServerCertificates(CertificatesConfigBuilder configBuilder) throws ManagedCertificatesException {
        super(configBuilder);
    }

    @Override
    public Logger getLogger() {
        return Logger.getLogger(ManagedServerCertificates.class);
    }

    @Override
    public SSLContext getClientSSLContext() {
        try {
            return SSLContextBuilder.create()
                    .loadTrustMaterial(clientsTrustStore, null)
                    .build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new ManagedCertificatesException(e);
        }
    }
}
