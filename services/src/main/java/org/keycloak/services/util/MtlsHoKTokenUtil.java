package org.keycloak.services.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.util.Base64Url;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;

public class MtlsHoKTokenUtil {
    // KEYCLOAK-6771 Certificate Bound Token
    // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3.1

    // retrieve client certificate exchanged in TLS handshake
    // https://docs.oracle.com/javaee/6/api/javax/servlet/ServletRequest.html#getAttribute(java.lang.String)
    private static final String JAVAX_SERVLET_REQUEST_X509_CERTIFICATE = "javax.servlet.request.X509Certificate";

    protected static final Logger logger = Logger.getLogger(MtlsHoKTokenUtil.class);

    private static final String DIGEST_ALG = "SHA-256";

    public static final String CERT_VERIFY_ERROR_DESC = "Client certificate missing, or its thumbprint and one in the refresh token did NOT match";


    public static AccessToken.CertConf bindTokenWithClientCertificate(HttpRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute(JAVAX_SERVLET_REQUEST_X509_CERTIFICATE);

        if (certs == null || certs.length < 1) {
            logger.warnf("no client certificate available.");
            return null;
        }

        String DERX509Base64UrlEncoded = null;
        try {
            // On Certificate Chain, first entry is considered to be client certificate.
            DERX509Base64UrlEncoded = getCertificateThumbprintInSHA256DERX509Base64UrlEncoded(certs[0]);
            if (logger.isTraceEnabled()) dumpCertInfo(certs);
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            // give up issuing MTLS HoK Token
            logger.warnf("give up issuing hok token. %s", e);
            return null;
        }

        AccessToken.CertConf certConf = new AccessToken.CertConf();
        certConf.setCertThumbprint(DERX509Base64UrlEncoded);
        return certConf;
    }

    public static boolean verifyTokenBindingWithClientCertificate(AccessToken token, HttpRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute(JAVAX_SERVLET_REQUEST_X509_CERTIFICATE);

        if (token == null) {
            logger.warnf("token is null");
            return false;
        }

        // Bearer Token, not MTLS HoK Token
        if (token.getCertConf() == null) {
            logger.warnf("bearer token received instead of hok token.");
            return false;
        }

        // HoK Token, but no Client Certificate available
        if (certs == null || certs.length < 1) {
            logger.warnf("missing client certificate.");
            return false;
        }

        String DERX509Base64UrlEncoded = null;
        String x5ts256 = token.getCertConf().getCertThumbprint();
        logger.tracef("hok token cnf-x5t#s256 = %s", x5ts256);

        try {
            // On Certificate Chain, first entry is considered to be client certificate.
            DERX509Base64UrlEncoded = getCertificateThumbprintInSHA256DERX509Base64UrlEncoded(certs[0]);
            if (logger.isTraceEnabled()) dumpCertInfo(certs);
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            logger.warnf("client certificate exception. %s", e);
            return false;
        }

        if (!MessageDigest.isEqual(x5ts256.getBytes(), DERX509Base64UrlEncoded.getBytes())) {
            logger.warnf("certificate's thumbprint and one in the refresh token did not match.");
            return false;
        }

        return true;
    }

    public static boolean verifyTokenBindingWithClientCertificate(String refreshToken, HttpRequest request) {
        JWSInput jws = null;
        RefreshToken rt = null;

        try {
            jws = new JWSInput(refreshToken);
            rt = jws.readJsonContent(RefreshToken.class);
        } catch (JWSInputException e) {
            logger.warnf("refresh token JWS Input Exception. %s", e);
            return false;
        }

        return verifyTokenBindingWithClientCertificate(rt, request);
    }

    private static String getCertificateThumbprintInSHA256DERX509Base64UrlEncoded (X509Certificate cert) throws NoSuchAlgorithmException, CertificateEncodingException {
        // need to calculate over DER encoding of the X.509 certificate
        //   https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3.1
        // in order to do that, call getEncoded()
        //   https://docs.oracle.com/javase/8/docs/api/java/security/cert/Certificate.html#getEncoded--
        byte[] DERX509Hash = cert.getEncoded();
        MessageDigest md = MessageDigest.getInstance(DIGEST_ALG);
        md.update(DERX509Hash);
        String DERX509Base64UrlEncoded = Base64Url.encode(md.digest());
        return DERX509Base64UrlEncoded;
    }

    private static void dumpCertInfo(X509Certificate[] certs) throws CertificateEncodingException  {
        logger.tracef(":: Try Holder of Key Token");
        logger.tracef(":: # of x509 Client Certificate in Certificate Chain = %d", certs.length);
        for (int i = 0; i < certs.length; i++) {
            logger.tracef(":: certs[%d] Raw Bytes Counts of first x509 Client Certificate in Certificate Chain = %d", i, certs[i].toString().length());
            logger.tracef(":: certs[%d] Raw Bytes String of first x509 Client Certificate in Certificate Chain = %s", i, certs[i].toString());
            logger.tracef(":: certs[%d] DER Dump Bytes of first x509 Client Certificate in Certificate Chain = %d", i, certs[i].getEncoded().length);
            String DERX509Base64UrlEncoded = null;
            try {
                DERX509Base64UrlEncoded = getCertificateThumbprintInSHA256DERX509Base64UrlEncoded(certs[i]);
            } catch (Exception e) {}
            logger.tracef(":: certs[%d] Base64URL Encoded SHA-256 Hash of DER formatted first x509 Client Certificate in Certificate Chain = %s", i, DERX509Base64UrlEncoded);
            logger.tracef(":: certs[%d] DER Dump Bytes of first x509 Client Certificate TBScertificate in Certificate Chain = %d", i, certs[i].getTBSCertificate().length);
            logger.tracef(":: certs[%d] Signature Algorithm of first x509 Client Certificate in Certificate Chain = %s", i, certs[i].getSigAlgName());
            logger.tracef(":: certs[%d] Certfication Type of first x509 Client Certificate in Certificate Chain = %s", i, certs[i].getType());
            logger.tracef(":: certs[%d] Issuer DN of first x509 Client Certificate in Certificate Chain = %s", i, certs[i].getIssuerDN().getName());
            logger.tracef(":: certs[%d] Subject DN of first x509 Client Certificate in Certificate Chain = %s", i, certs[i].getSubjectDN().getName());
        }
    }
}
