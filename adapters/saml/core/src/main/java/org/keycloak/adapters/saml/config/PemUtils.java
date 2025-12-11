/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.adapters.saml.config;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.keycloak.common.crypto.CryptoConstants;
import org.keycloak.common.util.PemException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.logging.Logger;

/**
 * Fork of the PemUtils from common module to avoid dependency on keycloak-crypto-default
 */
public class PemUtils {

    private static final Logger log = Logger.getLogger(PemUtils.class);

    static {
        Provider existingBc = Security.getProvider(CryptoConstants.BC_PROVIDER_ID);
        Provider bcProvider = existingBc == null ? new BouncyCastleProvider() : existingBc;

        if (existingBc == null) {
            Security.addProvider(bcProvider);
            log.debugv("Loaded {0} security provider", bcProvider.getClass().getName());
        } else {
            log.debugv("Security provider {0} already loaded", bcProvider.getClass().getName());
        }
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
            return decodeCertificate(bis);
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
            return decodePublicKey(der, "RSA");
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
    public static PrivateKey decodePrivateKey(String pem){
        if (pem == null) {
            return null;
        }

        try {
            byte[] der = pemToDer(pem);
            return decodePrivateKey(der);
        } catch (Exception e) {
            throw new PemException(e);
        }
    }

    private static byte[] pemToDer(String pem) {
        try {
            pem = removeBeginEnd(pem);
            return Base64.getDecoder().decode(pem);
        } catch (IllegalArgumentException e) {
            throw new PemException(e);
        }
    }

    private static String removeBeginEnd(String pem) {
        pem = pem.replaceAll("-----BEGIN (.*)-----", "");
        pem = pem.replaceAll("-----END (.*)----", "");
        pem = pem.replaceAll("\r\n", "");
        pem = pem.replaceAll("\n", "");
        return pem.trim();
    }

    private static PrivateKey decodePrivateKey(byte[] der) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        PKCS8EncodedKeySpec spec =
                new PKCS8EncodedKeySpec(der);
        KeyFactory kf = KeyFactory.getInstance("RSA", CryptoConstants.BC_PROVIDER_ID);
        return kf.generatePrivate(spec);
    }

    private static X509Certificate decodeCertificate(InputStream is) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509", CryptoConstants.BC_PROVIDER_ID);
        X509Certificate cert = (X509Certificate) cf.generateCertificate(is);
        is.close();
        return cert;
    }

    private static PublicKey decodePublicKey(byte[] der, String type) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        X509EncodedKeySpec spec =
                new X509EncodedKeySpec(der);
        KeyFactory kf = KeyFactory.getInstance("RSA", CryptoConstants.BC_PROVIDER_ID);
        return kf.generatePublic(spec);
    }
}
