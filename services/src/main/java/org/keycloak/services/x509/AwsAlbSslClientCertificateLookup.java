package org.keycloak.services.x509;

import java.io.UnsupportedEncodingException;
import java.security.cert.X509Certificate;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.common.util.PemException;
import org.keycloak.common.util.PemUtils;

/**
 * The AWS ALB Provider extracts the end user X.509 certificate send during TLS
 * mutual authentication and forwarded in an http header (with checking the
 * certificate chain). <br>
 * <br>
 * mTLS must be enabled in the AWS ALB configuration. <br>
 * It can be in verify or passthrough mode.
 */

public class AwsAlbSslClientCertificateLookup extends AbstractLeafClientCertificateFromHttpHeadersLookup {

    private static final Logger log = Logger.getLogger(AwsAlbSslClientCertificateLookup.class);

    public AwsAlbSslClientCertificateLookup(String sslCientCertHttpHeader, String sslCertChainHttpHeaderPrefix,
            int certificateChainLength, Set<X509Certificate> intermediateCerts, Set<X509Certificate> trustedRootCerts,
            boolean isTruststoreLoaded) {

        super(sslCientCertHttpHeader, sslCertChainHttpHeaderPrefix, certificateChainLength, intermediateCerts,
                trustedRootCerts, isTruststoreLoaded);

    }

    @Override
    protected X509Certificate decodeCertificateFromPem(String pem) throws PemException {

        if (pem == null) {
            log.warn("End user TLS Certificate is NULL! ");
            return null;
        }

        pem = pem.replace("+", "%2B");

        try {
            pem = java.net.URLDecoder.decode(pem, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("Cannot URL decode the end user TLS Certificate : " + pem, e);
        }

        return PemUtils.decodeCertificate(pem);

    }

}
