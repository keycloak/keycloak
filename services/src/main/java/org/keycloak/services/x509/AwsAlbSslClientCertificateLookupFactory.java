package org.keycloak.services.x509;

import org.keycloak.models.KeycloakSession;

/**
 * The factory and the corresponding providers extract a client certificate from
 * an AWS ALB reverse proxy (TLS termination).
 */
public class AwsAlbSslClientCertificateLookupFactory extends AbstractLeafClientCertificateFromHttpHeadersLookupFactory {

    private static final String PROVIDER = "awsalb";

    @Override
    public X509ClientCertificateLookup create(KeycloakSession session) {

        loadKeycloakTrustStore(session);

        if (trustProxyVerification) {
            return new AwsAlbProxyTrustedClientCertificateLookup(sslClientCertHttpHeader, sslChainHttpHeaderPrefix,
                    certificateChainLength);
        } else {
            return new AwsAlbSslClientCertificateLookup(sslClientCertHttpHeader, sslChainHttpHeaderPrefix,
                    certificateChainLength, intermediateCerts, trustedRootCerts, isTruststoreLoaded);
        }

    }

    @Override
    public String getId() {

        return PROVIDER;

    }

}
