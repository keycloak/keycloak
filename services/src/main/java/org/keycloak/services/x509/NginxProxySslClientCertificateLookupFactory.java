package org.keycloak.services.x509;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.truststore.TruststoreProvider;
import org.keycloak.truststore.TruststoreProviderFactory;
import org.keycloak.truststore.TruststoreReloadListener;

import org.jboss.logging.Logger;

/**
 * The factory and the corresponding providers extract a client certificate
 * from a NGINX reverse proxy (TLS termination).
 *  
 * @author <a href="mailto:arnault.michel@toad-consulting.com">Arnault MICHEL</a>
 * @version $Revision: 1 $
 * @since 10/09/2018
 */

public class NginxProxySslClientCertificateLookupFactory extends AbstractClientCertificateFromHttpHeadersLookupFactory
        implements TruststoreReloadListener {

    private static final Logger logger = Logger.getLogger(NginxProxySslClientCertificateLookupFactory.class);

    private static final String PROVIDER = "nginx";

    protected static final String TRUST_PROXY_VERIFICATION = "trust-proxy-verification";

    protected static final String CERT_IS_URL_ENCODED = "cert-is-url-encoded";

    private final ReloadableX509ClientCertificateLookup x509ClientCertificateLookup = new ReloadableX509ClientCertificateLookup();

    protected boolean trustProxyVerification;

    protected boolean certIsUrlEncoded;

    private volatile boolean isTruststoreLoaded;

    @Override
    public void init(Config.Scope config) {
        super.init(config);
        this.trustProxyVerification = config.getBoolean(TRUST_PROXY_VERIFICATION, false);
        logger.tracev("{0}: ''{1}''", TRUST_PROXY_VERIFICATION, trustProxyVerification);
        this.certIsUrlEncoded = config.getBoolean(CERT_IS_URL_ENCODED, true);
        logger.tracev("{0}: ''{1}''", CERT_IS_URL_ENCODED, certIsUrlEncoded);
        this.isTruststoreLoaded = false;
    }

    @Override
    public X509ClientCertificateLookup create(KeycloakSession session) {
        recreateX509ClientCertificateLookupIfNecessary(session);
        return x509ClientCertificateLookup;
    }

    @Override
    public String getId() {
        return PROVIDER;
    }

    @Override
    public void truststoreReloaded(KeycloakSession session) {
        isTruststoreLoaded = false;
        if (x509ClientCertificateLookup.delegate != null) {
            recreateX509ClientCertificateLookupIfNecessary(session);
        }
    }

    /**  When necessary, loads truststore and creates {@link X509ClientCertificateLookup}.
     *
     * @param kcSession keycloak session
     */
    private void recreateX509ClientCertificateLookupIfNecessary(KeycloakSession kcSession) {

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

            final Set<X509Certificate> trustedRootCerts;
            final Set<X509Certificate> intermediateCerts;
            if (provider != null && provider.getTruststore() != null) {
                trustedRootCerts = provider.getRootCertificates().entrySet().stream().flatMap(t -> t.getValue().stream()).collect(Collectors.toUnmodifiableSet());
                intermediateCerts = provider.getIntermediateCertificates().entrySet().stream().flatMap(t -> t.getValue().stream()).collect(Collectors.toUnmodifiableSet());

                logger.debug("Keycloak truststore loaded for NGINX x509cert-lookup provider.");

                isTruststoreLoaded = true;
            } else {
                trustedRootCerts = Set.of();
                intermediateCerts = Set.of();
            }

            if (trustProxyVerification) {
                x509ClientCertificateLookup.delegate = new NginxProxyTrustedClientCertificateLookup(sslClientCertHttpHeader,
                        sslChainHttpHeaderPrefix, certificateChainLength, certIsUrlEncoded);
            } else {
                x509ClientCertificateLookup.delegate = new NginxProxySslClientCertificateLookup(sslClientCertHttpHeader,
                        sslChainHttpHeaderPrefix, certificateChainLength, intermediateCerts, trustedRootCerts,
                        isTruststoreLoaded, certIsUrlEncoded);
            }
        }
    }

    public static final class ReloadableX509ClientCertificateLookup implements X509ClientCertificateLookup {

        private volatile X509ClientCertificateLookup delegate = null;

        @Override
        public X509Certificate[] getCertificateChain(HttpRequest httpRequest) throws GeneralSecurityException {
            if (delegate != null) {
                return delegate.getCertificateChain(httpRequest);
            }
            return new X509Certificate[0];
        }

        @Override
        public void close() {
            if (delegate != null) {
                delegate.close();
            }
        }

        public Set<X509Certificate> getTrustedRootCerts() {
            if (delegate instanceof NginxProxySslClientCertificateLookup sslClientCertificateLookup) {
                return sslClientCertificateLookup.getTrustedRootCerts();
            }
            return Set.of();
        }

        public Set<X509Certificate> getIntermediateCerts() {
            if (delegate instanceof NginxProxySslClientCertificateLookup sslClientCertificateLookup) {
                return sslClientCertificateLookup.getIntermediateCerts();
            }
            return Set.of();
        }
    }
}
