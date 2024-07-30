package org.keycloak.services.x509;

import java.io.UnsupportedEncodingException;
import java.security.cert.X509Certificate;

import org.jboss.logging.Logger;
import org.keycloak.common.util.PemException;
import org.keycloak.common.util.PemUtils;

/**
 * The AWS ALB Provider extracts the end user X.509 certificate send during TLS
 * mutual authentication and forwarded in an http header (without checking the
 * certificate chain). <br>
 * <br>
 * mTLS must be enabled in the AWS ALB configuration. <br>
 * For security reasons it should be in verify mode instead of passthrough mode.
 */
public class AwsAlbProxyTrustedClientCertificateLookup extends AbstractClientCertificateFromHttpHeadersLookup {

    public AwsAlbProxyTrustedClientCertificateLookup(String sslCientCertHttpHeader, String sslCertChainHttpHeaderPrefix,
            int certificateChainLength) {

        super(sslCientCertHttpHeader, sslCertChainHttpHeaderPrefix, certificateChainLength);

    }

    private static final Logger log = Logger.getLogger(AwsAlbProxyTrustedClientCertificateLookup.class);

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
