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

import java.security.Key;
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
public interface PemUtilsProvider {

    public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    public static final String END_CERT = "-----END CERTIFICATE-----";


    /**
     * Decode a X509 Certificate from a PEM string
     *
     * @param cert
     * @return
     * @throws Exception
     */
    public X509Certificate decodeCertificate(String cert);


    /**
     * Decode a Public Key from a PEM string
     *
     * @param pem
     * @return
     * @throws Exception
     */
    public PublicKey decodePublicKey(String pem);

    /**
     * Decode a Public Key from a PEM string
     * @param pem The pem encoded pblic key
     * @param type The type of the key (RSA, EC,...)
     * @return The public key or null
     */
    public PublicKey decodePublicKey(String pem, String type);


    /**
     * Decode a Private Key from a PEM string
     *
     * @param pem
     * @return
     * @throws Exception
     */
    public PrivateKey decodePrivateKey(String pem);


    /**
     * Encode a Key to a PEM string
     *
     * @param key
     * @return
     * @throws Exception
     */
    public String encodeKey(Key key);

    /**
     * Encode a X509 Certificate to a PEM string
     *
     * @param certificate
     * @return
     */
    public String encodeCertificate(Certificate certificate);

    public byte[] pemToDer(String pem);

    public String removeBeginEnd(String pem);

    public String generateThumbprint(String[] certChain, String encoding) throws NoSuchAlgorithmException;

}
