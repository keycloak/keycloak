package org.keycloak.tests.client;

import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.https.CertificatesConfig;
import org.keycloak.testframework.https.CertificatesConfigBuilder;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest
public class MutualTLSClientTest extends AbstractMutualTLSClientTest {

    @InjectCertificates(config = MutualTLSClientTest.MutualTLSClientCertificatesEnabled.class)
    ManagedCertificates managedCertificates;

    @Override
    public ManagedCertificates getManagedCertificates() {
        return managedCertificates;
    }

    private static class MutualTLSClientCertificatesEnabled implements CertificatesConfig {

        @Override
        public CertificatesConfigBuilder configure(CertificatesConfigBuilder config) {
            // use pem file that would use the normal trust-store
            return config
                    .tlsEnabled(true)
                    .mTlsEnabled(true)
                    .keystoreFormat(KeystoreUtil.KeystoreFormat.BCFKS)
                    .stores("keycloak.bcfks", "keycloak-truststore.pem", "client.bcfks", "keycloak-truststore.bcfks");
        }
    }
}
