package org.keycloak.services.x509;

import java.io.UnsupportedEncodingException;
import java.security.cert.X509Certificate;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.common.util.PemException;
import org.keycloak.common.util.PemUtils;

/**
 * The NGINX Provider extract end user X.509 certificate send during TLS mutual authentication,
 * and forwarded in an http header.
 *
 * NGINX configuration must have : 
 * <code>
 * server {
 *    ...
 *    ssl_client_certificate                  path-to-my-trustyed-cas-for-client-auth.pem;
 *    ssl_verify_client                       on|optional_no_ca;
 *    ssl_verify_depth                        2;
 *    ...
 *    location / {
 *    ...
 *      proxy_set_header ssl-client-cert        $ssl_client_escaped_cert;
 *    ...
 *  }
 * </code>
 *
 * Note that $ssl_client_cert is deprecated, use only $ssl_client_escaped_cert with this implementation
 *
 * @author <a href="mailto:arnault.michel@toad-consulting.com">Arnault MICHEL</a>
 * @version $Revision: 1 $
 * @since 10/09/2018
 */

public class NginxProxySslClientCertificateLookup extends AbstractLeafClientCertificateFromHttpHeadersLookup {

    private static final Logger log = Logger.getLogger(NginxProxySslClientCertificateLookup.class);

    public NginxProxySslClientCertificateLookup(String sslClientCertHttpHeader,
                                                String sslCertChainHttpHeaderPrefix,
                                                int certificateChainLength,
                                                Set<X509Certificate> intermediateCerts,
                                                Set<X509Certificate> trustedRootCerts,
                                                boolean isTruststoreLoaded
                                                ) {

        super(sslClientCertHttpHeader, sslCertChainHttpHeaderPrefix, certificateChainLength, intermediateCerts, trustedRootCerts, isTruststoreLoaded);

    }

    /**
     * Removing PEM Headers and end of lines
     *
     * @param pem
     * @return
     */
    private static String removeBeginEnd(String pem) {
        pem = pem.replace(PemUtils.BEGIN_CERT, "");
        pem = pem.replace(PemUtils.END_CERT, "");
        pem = pem.replace("\r\n", "");
        pem = pem.replace("\n", "");
        return pem.trim();
    }

    /**
     * Decoding end user certificate, including URL decodeding due to ssl_client_escaped_cert nginx variable.
     */
    @Override
    protected X509Certificate decodeCertificateFromPem(String pem) throws PemException {

        if (pem == null) {
            log.warn("End user TLS Certificate is NULL! ");
            return null;
        }
        try {
            pem = java.net.URLDecoder.decode(pem, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("Cannot URL decode the end user TLS Certificate : " + pem,e);
        }

        if (pem.startsWith(PemUtils.BEGIN_CERT)) {
            pem = removeBeginEnd(pem);
        }

        return PemUtils.decodeCertificate(pem);
    }

}
