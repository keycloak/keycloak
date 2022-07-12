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

package org.keycloak.crypto.def;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.DerUtils;
import org.keycloak.common.util.PemException;
import org.keycloak.common.crypto.PemUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;



/**
 * Utility classes to extract PublicKey, PrivateKey, and X509Certificate from openssl generated PEM files
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PemUtilsBC implements PemUtils {

    // public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    // public static final String END_CERT = "-----END CERTIFICATE-----";

    /**
     * Decode a X509 Certificate from a PEM string
     *
     * @param cert
     * @return
     * @throws Exception
     */
    @Override
    public X509Certificate decodeCertificate(String cert) {
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
    @Override
    public PublicKey decodePublicKey(String pem) {
        return decodePublicKey(pem, "RSA");
    }

    /**
     * Decode a Public Key from a PEM string
     * @param pem The pem encoded pblic key
     * @param type The type of the key (RSA, EC,...)
     * @return The public key or null
     */
    @Override
    public PublicKey decodePublicKey(String pem, String type) {
        if (pem == null) {
            return null;
        }

        try {
            byte[] der = pemToDer(pem);
            return DerUtils.decodePublicKey(der, type);
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
    @Override
    public PrivateKey decodePrivateKey(String pem) {
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
    @Override
    public String encodeKey(Key key) {
        return encode(key);
    }

    /**
     * Encode a X509 Certificate to a PEM string
     *
     * @param certificate
     * @return
     */
    @Override
    public String encodeCertificate(Certificate certificate) {
        return encode(certificate);
    }

    private String encode(Object obj) {
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
            return removeBeginEnd(s);
        } catch (Exception e) {
            throw new PemException(e);
        }
    }

    @Override
    public byte[] pemToDer(String pem) {
        try {
            pem = removeBeginEnd(pem);
            return Base64.decode(pem);
        } catch (IOException ioe) {
            throw new PemException(ioe);
        }
    }

    public String removeBeginEnd(String pem) {
        pem = pem.replaceAll("-----BEGIN (.*)-----", "");
        pem = pem.replaceAll("-----END (.*)----", "");
        pem = pem.replaceAll("\r\n", "");
        pem = pem.replaceAll("\n", "");
        return pem.trim();
    }

    @Override
    public String generateThumbprint(String[] certChain, String encoding) throws NoSuchAlgorithmException {
        return Base64Url.encode(generateThumbprintBytes(certChain, encoding));
    }

    private byte[] generateThumbprintBytes(String[] certChain, String encoding) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(encoding).digest(pemToDer(certChain[0]));
    }

}
