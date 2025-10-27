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

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.keycloak.common.crypto.CryptoIntegration;

/**
 * Utility classes to extract PublicKey, PrivateKey, and X509Certificate from openssl generated PEM files
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PemUtils {

    public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    public static final String END_CERT = "-----END CERTIFICATE-----";

    public static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    public static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";
    public static final String BEGIN_RSA_PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----";
    public static final String END_RSA_PRIVATE_KEY = "-----END RSA PRIVATE KEY-----";

    /**
     * Decode a X509 Certificate from a PEM string
     *
     * @param cert
     * @return
     * @throws Exception
     */
    public static X509Certificate decodeCertificate(String cert) {
        return CryptoIntegration.getProvider().getPemUtils().decodeCertificate(cert);
    }

    /**
     * Decode one or more X509 Certificates from a PEM string (certificate bundle)
     *
     * @param certs
     * @return
     * @throws Exception
     */
    public static X509Certificate[] decodeCertificates(String certs) {
        return Arrays.stream(certs.split(END_CERT))
                .map(String::trim)
                .filter(pemBlock -> !pemBlock.isEmpty())
                .map(pemBlock -> PemUtils.decodeCertificate(pemBlock + END_CERT))
                .toArray(X509Certificate[]::new);
    }

    /**
     * Decode a Public Key from a PEM string
     *
     * @param pem
     * @return
     * @throws Exception
     */
    public static PublicKey decodePublicKey(String pem) {
        return CryptoIntegration.getProvider().getPemUtils().decodePublicKey(pem);
    }

    /**
     * Decode a Public Key from a PEM string
     * @param pem The pem encoded pblic key
     * @param type The type of the key (RSA, EC,...)
     * @return The public key or null
     */
    public static PublicKey decodePublicKey(String pem, String type){
        return CryptoIntegration.getProvider().getPemUtils().decodePublicKey(pem, type);
    }


    /**
     * Decode a Private Key from a PEM string
     *
     * @param pem
     * @return
     * @throws Exception
     */
    public static PrivateKey decodePrivateKey(String pem){
        return CryptoIntegration.getProvider().getPemUtils().decodePrivateKey(pem);
    }


    /**
     * Encode a Key to a PEM string
     *
     * @param key
     * @return
     * @throws Exception
     */
    public static String encodeKey(Key key){
        return CryptoIntegration.getProvider().getPemUtils().encodeKey(key);
    }

    /**
     * Encode a X509 Certificate to a PEM string
     *
     * @param certificate
     * @return
     */
    public static String encodeCertificate(Certificate certificate){
        return CryptoIntegration.getProvider().getPemUtils().encodeCertificate(certificate);
    }

    public static byte[] pemToDer(String pem){
        return CryptoIntegration.getProvider().getPemUtils().pemToDer(pem);
    }

    public static String removeBeginEnd(String pem){
        return CryptoIntegration.getProvider().getPemUtils().removeBeginEnd(pem);
    }

    public static String addPrivateKeyBeginEnd(String privateKeyPem) {
        return new StringBuilder(PemUtils.BEGIN_PRIVATE_KEY + "\n")
                .append(privateKeyPem)
                .append("\n" + PemUtils.END_PRIVATE_KEY)
                .toString();
    }

    public static String addCertificateBeginEnd(String certificate) {
        return new StringBuilder(BEGIN_CERT + "\n")
            .append(certificate)
            .append("\n" + END_CERT)
            .toString();
    }

    public static String addRsaPrivateKeyBeginEnd(String privateKeyPem) {
        return new StringBuilder(PemUtils.BEGIN_RSA_PRIVATE_KEY + "\n")
                .append(privateKeyPem)
                .append("\n" + PemUtils.END_RSA_PRIVATE_KEY)
                .toString();
    }

    public static String generateThumbprint(String[] certChain, String encoding) throws NoSuchAlgorithmException{
        return CryptoIntegration.getProvider().getPemUtils().generateThumbprint(certChain, encoding);
    }

}
