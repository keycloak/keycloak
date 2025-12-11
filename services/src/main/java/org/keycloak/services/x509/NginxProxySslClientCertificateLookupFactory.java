package org.keycloak.services.x509;

import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.truststore.TruststoreProvider;
import org.keycloak.truststore.TruststoreProviderFactory;

import org.jboss.logging.Logger;

/**
 * The factory and the corresponding providers extract a client certificate
 * from a NGINX reverse proxy (TLS termination).
 *  
 * @author <a href="mailto:arnault.michel@toad-consulting.com">Arnault MICHEL</a>
 * @version $Revision: 1 $
 * @since 10/09/2018
 */

public class NginxProxySslClientCertificateLookupFactory extends AbstractClientCertificateFromHttpHeadersLookupFactory {

    private static final Logger logger = Logger.getLogger(NginxProxySslClientCertificateLookupFactory.class);

    private static final String PROVIDER = "nginx";

    protected static final String TRUST_PROXY_VERIFICATION = "trust-proxy-verification";

    protected static final String CERT_IS_URL_ENCODED = "cert-is-url-encoded";

    protected boolean trustProxyVerification;

    protected boolean certIsUrlEncoded;

    private volatile boolean isTruststoreLoaded;

    private Set<X509Certificate> trustedRootCerts;

    private Set<X509Certificate> intermediateCerts;

    @Override
    public void init(Config.Scope config) {
        super.init(config);
        this.trustProxyVerification = config.getBoolean(TRUST_PROXY_VERIFICATION, false);
        logger.tracev("{0}: ''{1}''", TRUST_PROXY_VERIFICATION, trustProxyVerification);
        this.certIsUrlEncoded = config.getBoolean(CERT_IS_URL_ENCODED, true);
        logger.tracev("{0}: ''{1}''", CERT_IS_URL_ENCODED, certIsUrlEncoded);
        this.isTruststoreLoaded = false;
        this.trustedRootCerts = ConcurrentHashMap.newKeySet();
        this.intermediateCerts = ConcurrentHashMap.newKeySet();

    }

    @Override
    public X509ClientCertificateLookup create(KeycloakSession session) {
        loadKeycloakTrustStore(session);
        if (trustProxyVerification) {
            return new NginxProxyTrustedClientCertificateLookup(sslClientCertHttpHeader,
                    sslChainHttpHeaderPrefix, certificateChainLength, certIsUrlEncoded);
        } else {
            return new NginxProxySslClientCertificateLookup(sslClientCertHttpHeader,
                    sslChainHttpHeaderPrefix, certificateChainLength, intermediateCerts, trustedRootCerts, isTruststoreLoaded, certIsUrlEncoded);
        }
    }

    @Override
    public String getId() {
        return PROVIDER;
    }

    /**  Loading truststore @ first login
     *
     * @param kcSession keycloak session
     */
    private void loadKeycloakTrustStore(KeycloakSession kcSession) {

        if (isTruststoreLoaded){
            return;
        }

        synchronized (this) {
            if (isTruststoreLoaded) {
                return;
            }
            logger.debug(" Loading Keycloak truststore ...");
            KeycloakSessionFactory factory = kcSession.getKeycloakSessionFactory();
            TruststoreProviderFactory truststoreFactory = (TruststoreProviderFactory) factory.getProviderFactory(TruststoreProvider.class);
            TruststoreProvider provider = truststoreFactory.create(kcSession);

            if (provider != null && provider.getTruststore() != null) {
                Set<X509Certificate> rootCertificates = provider.getRootCertificates().entrySet().stream().flatMap(t -> t.getValue().stream()).collect(Collectors.toSet());
                Set<X509Certificate> intermediateCertficiates = provider.getIntermediateCertificates().entrySet().stream().flatMap(t -> t.getValue().stream()).collect(Collectors.toSet());

                trustedRootCerts.addAll(rootCertificates);
                intermediateCerts.addAll(intermediateCertficiates);
                logger.debug("Keycloak truststore loaded for NGINX x509cert-lookup provider.");

                isTruststoreLoaded = true;
            }
        }
    }
}
