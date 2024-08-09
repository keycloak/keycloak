package org.keycloak.services.x509;

import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.truststore.TruststoreProvider;
import org.keycloak.truststore.TruststoreProviderFactory;

/**
 * The factory and the corresponding providers extract a leaf client certificate
 * from a reverse proxy (TLS termination).
 */
public abstract class AbstractLeafClientCertificateFromHttpHeadersLookupFactory
        extends AbstractClientCertificateFromHttpHeadersLookupFactory {

    private static final Logger logger = Logger
            .getLogger(AbstractLeafClientCertificateFromHttpHeadersLookupFactory.class);

    protected static final String TRUST_PROXY_VERIFICATION = "trust-proxy-verification";
    protected boolean trustProxyVerification;

    protected volatile boolean isTruststoreLoaded;
    protected Set<X509Certificate> trustedRootCerts;
    protected Set<X509Certificate> intermediateCerts;

    @Override
    public void init(Scope config) {

        super.init(config);

        this.trustProxyVerification = config.getBoolean(TRUST_PROXY_VERIFICATION, false);
        logger.tracev("{0}: ''{1}''", TRUST_PROXY_VERIFICATION, trustProxyVerification);

        this.isTruststoreLoaded = false;
        this.trustedRootCerts = ConcurrentHashMap.newKeySet();
        this.intermediateCerts = ConcurrentHashMap.newKeySet();

    }

    /**
     * Loading truststore @ first login
     *
     * @param kcSession keycloak session
     */
    protected void loadKeycloakTrustStore(KeycloakSession kcSession) {

        if (isTruststoreLoaded) {
            return;
        }

        synchronized (this) {
            if (isTruststoreLoaded) {
                return;
            }
            logger.debug(" Loading Keycloak truststore ...");
            KeycloakSessionFactory factory = kcSession.getKeycloakSessionFactory();
            TruststoreProviderFactory truststoreFactory = (TruststoreProviderFactory) factory
                    .getProviderFactory(TruststoreProvider.class, "file");
            TruststoreProvider provider = truststoreFactory.create(kcSession);

            if (provider != null && provider.getTruststore() != null) {
                trustedRootCerts.addAll(provider.getRootCertificates().values());
                intermediateCerts.addAll(provider.getIntermediateCertificates().values());
                logger.debug("Keycloak truststore loaded for the x509cert-lookup provider.");

                isTruststoreLoaded = true;
            }
        }

    }

}
