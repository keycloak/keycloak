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

package org.keycloak.common.crypto;

import java.io.ByteArrayInputStream;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.DerUtils;
import org.keycloak.common.util.PemException;

/**
 * Utility classes to extract PublicKey, PrivateKey, and X509Certificate from openssl generated PEM files
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class PemUtilsProvider {


    /**
     * Decode a X509 Certificate from a PEM string
     *
     * @param cert
     * @return
     * @throws Exception
     */
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
    public PublicKey decodePublicKey(String pem) {
        return decodePublicKey(pem, "RSA");
    }

    /**
     * Decode a Public Key from a PEM string
     * @param pem The pem encoded pblic key
     * @param type The type of the key (RSA, EC,...)
     * @return The public key or null
     */
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
    public abstract PrivateKey decodePrivateKey(String pem);


    /**
     * Encode a Key to a PEM string
     *
     * @param key
     * @return
     * @throws Exception
     */
    public String encodeKey(Key key) {
        return encode(key);
    }
    

    /**
     * Encode a X509 Certificate to a PEM string
     *
     * @param certificate
     * @return
     */
    public String encodeCertificate(Certificate certificate) {
        return encode(certificate);
    }

    public byte[] pemToDer(String pem) {
        try {
            pem = removeBeginEnd(pem);
            return Base64.getDecoder().decode(pem);
        } catch (IllegalArgumentException e) {
            throw new PemException(e);
        }
    }

    public String removeBeginEnd(String pem) {
        pem = pem.replaceAll("-----BEGIN (.*)-----", "");
        pem = pem.replaceAll("-----END (.*)----", "");
        pem = pem.replaceAll("\r\n", "");
        pem = pem.replaceAll("\n", "");
        return pem.trim();
    }

    public String generateThumbprint(String[] certChain, String encoding) throws NoSuchAlgorithmException{
        return Base64Url.encode(generateThumbprintBytes(certChain, encoding));
    }

    private byte[] generateThumbprintBytes(String[] certChain, String encoding) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(encoding).digest(pemToDer(certChain[0]));
    }

    protected abstract String encode(Object obj);

}
