/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.saml;

import java.security.Signature;
import java.util.HashMap;
import java.util.Map;

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

    private static final Map<String, SignatureAlgorithm> signatureMethodMap = new HashMap<>();
    private static final Map<String, SignatureAlgorithm> signatureDigestMethodMap = new HashMap<>();

    static {
        signatureMethodMap.put(RSA_SHA1.getXmlSignatureMethod(), RSA_SHA1);
        signatureMethodMap.put(RSA_SHA256.getXmlSignatureMethod(), RSA_SHA256);
        signatureMethodMap.put(RSA_SHA512.getXmlSignatureMethod(), RSA_SHA512);
        signatureMethodMap.put(DSA_SHA1.getXmlSignatureMethod(), DSA_SHA1);

        signatureDigestMethodMap.put(RSA_SHA1.getXmlSignatureDigestMethod(), RSA_SHA1);
        signatureDigestMethodMap.put(RSA_SHA256.getXmlSignatureDigestMethod(), RSA_SHA256);
        signatureDigestMethodMap.put(RSA_SHA512.getXmlSignatureDigestMethod(), RSA_SHA512);
        signatureDigestMethodMap.put(DSA_SHA1.getXmlSignatureDigestMethod(), DSA_SHA1);
    }

    public static SignatureAlgorithm getFromXmlMethod(String xml) {
        return signatureMethodMap.get(xml);
    }

    public static SignatureAlgorithm getFromXmlDigest(String xml) {
        return signatureDigestMethodMap.get(xml);
    }

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
