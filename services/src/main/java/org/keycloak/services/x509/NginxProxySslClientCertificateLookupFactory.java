package org.keycloak.services.x509;

import org.keycloak.models.KeycloakSession;

/**
 * The factory and the corresponding providers extract a client certificate
 * from a NGINX reverse proxy (TLS termination).
 *  
 * @author <a href="mailto:arnault.michel@toad-consulting.com">Arnault MICHEL</a>
 * @version $Revision: 1 $
 * @since 10/09/2018
 */

public class NginxProxySslClientCertificateLookupFactory extends AbstractLeafClientCertificateFromHttpHeadersLookupFactory {

    private static final String PROVIDER = "nginx";

    @Override
    public X509ClientCertificateLookup create(KeycloakSession session) {
        loadKeycloakTrustStore(session);
        if (trustProxyVerification) {
            return new NginxProxyTrustedClientCertificateLookup(sslClientCertHttpHeader,
                    sslChainHttpHeaderPrefix, certificateChainLength);
        } else {
            return new NginxProxySslClientCertificateLookup(sslClientCertHttpHeader,
                    sslChainHttpHeaderPrefix, certificateChainLength, intermediateCerts, trustedRootCerts, isTruststoreLoaded);
        }
    }

    @Override
    public String getId() {
        return PROVIDER;
    }

}
