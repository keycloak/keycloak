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

import java.io.DataInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.keycloak.common.crypto.CryptoIntegration;

/**
 * Extract PrivateKey, PublicKey, and X509Certificate from a DER encoded byte array or file.  Usually
 * generated from openssl
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public final class DerUtils {

    private DerUtils() {
    }

    public static PrivateKey decodePrivateKey(InputStream is)
            throws Exception {

        DataInputStream dis = new DataInputStream(is);
        byte[] keyBytes = new byte[dis.available()];
        dis.readFully(keyBytes);
        dis.close();

        return decodePrivateKey(keyBytes);
    }

    public static PublicKey decodePublicKey(byte[] der) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        return decodePublicKey(der, "RSA");
    }

    public static PublicKey decodePublicKey(byte[] der, String type) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        X509EncodedKeySpec spec =
                new X509EncodedKeySpec(der);
        KeyFactory kf = CryptoIntegration.getProvider().getKeyFactory(type);
        return kf.generatePublic(spec);
    }

    public static X509Certificate decodeCertificate(InputStream is) throws Exception {
        CertificateFactory cf = CryptoIntegration.getProvider().getX509CertFactory();
        X509Certificate cert = (X509Certificate) cf.generateCertificate(is);
        is.close();
        return cert;
    }

    public static PrivateKey decodePrivateKey(byte[] der) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        PKCS8EncodedKeySpec spec =
                new PKCS8EncodedKeySpec(der);
        String[] algorithms = { "RSA", "EC" };
        for (String algorithm : algorithms) {
            try {
                return CryptoIntegration.getProvider().getKeyFactory(algorithm).generatePrivate(spec);
            } catch (InvalidKeySpecException e) {
                // Ignore and try the next algorithm.
            }
        }
        throw new InvalidKeySpecException("Unable to decode the private key with supported algorithms: " + String.join(", ", algorithms));
   }
}
