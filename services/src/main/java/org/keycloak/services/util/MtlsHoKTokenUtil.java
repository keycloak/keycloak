package org.keycloak.services.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import org.keycloak.common.util.Base64Url;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.x509.X509ClientCertificateLookup;

import org.jboss.logging.Logger;

public class MtlsHoKTokenUtil {
    // KEYCLOAK-6771 Certificate Bound Token
    // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3.1

    protected static final Logger logger = Logger.getLogger(MtlsHoKTokenUtil.class);

    private static final String DIGEST_ALG = "SHA-256";

    public static final String CERT_VERIFY_ERROR_DESC = "Client certificate missing, or its thumbprint and one in the refresh token did NOT match";


    public static AccessToken.Confirmation bindTokenWithClientCertificate(HttpRequest request, KeycloakSession session) {
        X509Certificate[] certs = getCertificateChain(request, session);

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

        AccessToken.Confirmation confirmation = new AccessToken.Confirmation();
        confirmation.setCertThumbprint(DERX509Base64UrlEncoded);
        return confirmation;
    }

    public static boolean verifyTokenBindingWithClientCertificate(AccessToken token, HttpRequest request, KeycloakSession session) {
        if (token == null) {
            logger.warnf("token is null");
            return false;
        }

        // Bearer Token, not MTLS HoK Token
        if (token.getConfirmation() == null) {
            logger.warnf("bearer token received instead of hok token.");
            return false;
        }

        X509Certificate[] certs = getCertificateChain(request, session);

        // HoK Token, but no Client Certificate available
        if (certs == null || certs.length < 1) {
            logger.warnf("missing client certificate.");
            return false;
        }

        String DERX509Base64UrlEncoded = null;
        String x5ts256 = token.getConfirmation().getCertThumbprint();
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

    private static X509Certificate[] getCertificateChain(HttpRequest request, KeycloakSession session) {
        try {
               // Get a x509 client certificate
            X509ClientCertificateLookup provider = session.getProvider(X509ClientCertificateLookup.class);
            if (provider == null) {
                logger.errorv("\"{0}\" Spi is not available, did you forget to update the configuration?", X509ClientCertificateLookup.class);
            return null;
            }
            X509Certificate[] certs = provider.getCertificateChain(request);
            return certs;
        } catch (GeneralSecurityException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
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
