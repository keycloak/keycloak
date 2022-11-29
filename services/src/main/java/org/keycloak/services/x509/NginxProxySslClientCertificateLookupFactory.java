package org.keycloak.services.x509;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;

/**
 * The factory and the corresponding providers extract a client certificate
 * from a NGINX reverse proxy (TLS termination).
 *  
 * @author <a href="mailto:arnault.michel@toad-consulting.com">Arnault MICHEL</a>
 * @version $Revision: 1 $
 * @since 10/09/2018
 */

public class NginxProxySslClientCertificateLookupFactory extends AbstractClientCertificateFromHttpHeadersLookupFactory {

    private final static Logger logger = Logger.getLogger(NginxProxySslClientCertificateLookupFactory.class);

    private final static String PROVIDER = "nginx";

    protected final static String TRUST_PROXY_VERIFICATION = "trust-proxy-verification";

    protected boolean trustProxyVerification = false;

    @Override
    public void init(Config.Scope config) {
        super.init(config);
        trustProxyVerification = config.getBoolean(TRUST_PROXY_VERIFICATION, false);
        logger.tracev("{0}: ''{1}''", TRUST_PROXY_VERIFICATION, trustProxyVerification);
    }

    @Override
    public X509ClientCertificateLookup create(KeycloakSession session) {
        if (trustProxyVerification) {
            return new NginxProxyTrustedClientCertificateLookup(sslClientCertHttpHeader,
                    sslChainHttpHeaderPrefix, certificateChainLength);
        } else {
            return new NginxProxySslClientCertificateLookup(sslClientCertHttpHeader,
                    sslChainHttpHeaderPrefix, certificateChainLength, session);
        }
    }

    @Override
    public String getId() {
        return PROVIDER;
    }
}
