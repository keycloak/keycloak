package org.keycloak.protocol.saml;

import java.security.NoSuchAlgorithmException;
import java.security.Signature;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public enum SignatureAlgorithm {
    RSA_SHA1("http://www.w3.org/2000/09/xmldsig#rsa-sha1", "http://www.w3.org/2000/09/xmldsig#sha1", "SHA1withRSA"),
    RSA_SHA256("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", "http://www.w3.org/2001/04/xmlenc#sha256", "SHA256withRSA"),
    RSA_SHA512("http://www.w3.org/2001/04/xmldsig-more#rsa-sha512", "http://www.w3.org/2001/04/xmlenc#sha512", "SHA512withRSA"),
    DSA_SHA1("http://www.w3.org/2000/09/xmldsig#dsa-sha1", "http://www.w3.org/2000/09/xmldsig#sha1", "SHA1withDSA")
    ;
    private final String xmlSignatureMethod;
    private final String xmlSignatureDigestMethod;
    private final String javaSignatureAlgorithm;

    SignatureAlgorithm(String xmlSignatureMethod, String xmlSignatureDigestMethod, String javaSignatureAlgorithm) {
        this.xmlSignatureMethod = xmlSignatureMethod;
        this.xmlSignatureDigestMethod = xmlSignatureDigestMethod;
        this.javaSignatureAlgorithm = javaSignatureAlgorithm;
    }

    public String getXmlSignatureMethod() {
        return xmlSignatureMethod;
    }

    public String getXmlSignatureDigestMethod() {
        return xmlSignatureDigestMethod;
    }

    public String getJavaSignatureAlgorithm() {
        return javaSignatureAlgorithm;
    }

    public Signature createSignature() {
        try {
            return Signature.getInstance(javaSignatureAlgorithm);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
