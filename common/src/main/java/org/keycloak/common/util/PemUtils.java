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

package org.keycloak.common.util;


import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * Utility classes to extract PublicKey, PrivateKey, and X509Certificate from openssl generated PEM files
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public final class PemUtils {

    public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    public static final String END_CERT = "-----END CERTIFICATE-----";

    static {
        BouncyIntegration.init();
    }

    private PemUtils() {
    }

    /**
     * Decode a X509 Certificate from a PEM string
     *
     * @param cert
     * @return
     * @throws Exception
     */
    public static X509Certificate decodeCertificate(String cert) {
        if (cert == null) {
            return null;
        }

        try {
            byte[] der = pemToDer(cert);
            ByteArrayInputStream bis = new ByteArrayInputStream(der);
            return DerUtils.decodeCertificate(bis);
        } catch (Exception e) {
            throw new PemException(e);
        }
    }

    /**
     * Decode a Public Key from a PEM string
     *
     * @param pem
     * @return
     * @throws Exception
     */
    public static PublicKey decodePublicKey(String pem) {
        if (pem == null) {
            return null;
        }

        try {
            byte[] der = pemToDer(pem);
            return DerUtils.decodePublicKey(der);
        } catch (Exception e) {
            throw new PemException(e);
        }
    }

    /**
     * Decode a Private Key from a PEM string
     *
     * @param pem
     * @return
     * @throws Exception
     */
    public static PrivateKey decodePrivateKey(String pem) {
        if (pem == null) {
            return null;
        }

        try {
            byte[] der = pemToDer(pem);
            return DerUtils.decodePrivateKey(der);
        } catch (Exception e) {
            throw new PemException(e);
        }
    }

    /**
     * Encode a Key to a PEM string
     *
     * @param key
     * @return
     * @throws Exception
     */
    public static String encodeKey(Key key) {
        return encode(key);
    }

    /**
     * Encode a X509 Certificate to a PEM string
     *
     * @param certificate
     * @return
     */
    public static String encodeCertificate(Certificate certificate) {
        return encode(certificate);
    }

    private static String encode(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            StringWriter writer = new StringWriter();
            JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
            pemWriter.writeObject(obj);
            pemWriter.flush();
            pemWriter.close();
            String s = writer.toString();
            return PemUtils.removeBeginEnd(s);
        } catch (Exception e) {
            throw new PemException(e);
        }
    }

    public static byte[] pemToDer(String pem) {
        try {
            pem = removeBeginEnd(pem);
            return Base64.decode(pem);
        } catch (IOException ioe) {
            throw new PemException(ioe);
        }
    }

    public static String removeBeginEnd(String pem) {
        pem = pem.replaceAll("-----BEGIN (.*)-----", "");
        pem = pem.replaceAll("-----END (.*)----", "");
        pem = pem.replaceAll("\r\n", "");
        pem = pem.replaceAll("\n", "");
        return pem.trim();
    }

    public static String generateThumbprint(String[] certChain, String encoding) throws NoSuchAlgorithmException {
        return Base64Url.encode(generateThumbprintBytes(certChain, encoding));
    }

    static byte[] generateThumbprintBytes(String[] certChain, String encoding) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(encoding).digest(pemToDer(certChain[0]));
    }

}
