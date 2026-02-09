package org.keycloak.services.x509;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import org.jboss.logging.Logger;

/**
 * The factory and the corresponding providers extract a client certificate
 * from a reverse proxy that is compliant with RFC 9440.
 *
 * @author <a href="mailto:seiferma.dev+kc@gmail.com">Stephan Seifermann</a>
 * @version $Revision: 1 $
 * @since 12/30/2024
 */
public class Rfc9440ClientCertificateLookupFactory implements X509ClientCertificateLookupFactory {

    private final static Logger logger = Logger.getLogger(Rfc9440ClientCertificateLookupFactory.class);
    private final static String PROVIDER = "rfc9440";

    protected final static String HTTP_HEADER_CLIENT_CERT = "sslClientCert";
    protected final static String HTTP_HEADER_CLIENT_CERT_DEFAULT = "Client-Cert";
    protected final static String HTTP_HEADER_CERT_CHAIN = "sslCertChain";
    protected final static String HTTP_HEADER_CERT_CHAIN_DEFAULT = "Client-Cert-Chain";
    protected final static String HTTP_HEADER_CERT_CHAIN_LENGTH = "certificateChainLength";
    protected final static int HTTP_HEADER_CERT_CHAIN_LENGTH_DEFAULT = 1;

    protected String sslClientCertHttpHeader;
    protected String sslChainHttpHeader;
    protected int certificateChainLength;

    @Override
    public void init(Config.Scope config) {
        certificateChainLength = config.getInt(HTTP_HEADER_CERT_CHAIN_LENGTH, HTTP_HEADER_CERT_CHAIN_LENGTH_DEFAULT);
        sslClientCertHttpHeader = config.get(HTTP_HEADER_CLIENT_CERT, HTTP_HEADER_CLIENT_CERT_DEFAULT);
        sslChainHttpHeader = config.get(HTTP_HEADER_CERT_CHAIN, HTTP_HEADER_CERT_CHAIN_DEFAULT);

        logger.tracev("{0}:   ''{1}''", HTTP_HEADER_CLIENT_CERT, sslClientCertHttpHeader);
        logger.tracev("{0}:   ''{1}''", HTTP_HEADER_CERT_CHAIN, sslChainHttpHeader);
        logger.tracev("{0}:   ''{1}''", HTTP_HEADER_CERT_CHAIN_LENGTH, certificateChainLength);
    }

    @Override
    public X509ClientCertificateLookup create(KeycloakSession session) {
        return new Rfc9440ClientCertificateLookup(sslClientCertHttpHeader, sslChainHttpHeader, certificateChainLength);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // intentionally left blank
    }

    @Override
    public void close() {
        // intentionally left blank
    }

    @Override
    public String getId() {
        return PROVIDER;
    }
}
