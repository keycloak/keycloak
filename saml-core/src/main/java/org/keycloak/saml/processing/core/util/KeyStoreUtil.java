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
package org.keycloak.saml.processing.core.util;

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

/**
 * Utility to handle Java Keystore
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jan 12, 2009
 */
public class KeyStoreUtil {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    /**
     * Get the KeyStore
     *
     * @param keyStoreFile
     * @param storePass
     *
     * @return
     *
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static KeyStore getKeyStore(File keyStoreFile, char[] storePass) throws GeneralSecurityException, IOException {
        FileInputStream fis = new FileInputStream(keyStoreFile);
        return getKeyStore(fis, storePass);
    }

    /**
     * Get the Keystore given the url to the keystore file as a string
     *
     * @param fileURL
     * @param storePass
     *
     * @return
     *
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static KeyStore getKeyStore(String fileURL, char[] storePass) throws GeneralSecurityException, IOException {
        if (fileURL == null)
            throw logger.nullArgumentError("fileURL");

        File file = new File(fileURL);
        FileInputStream fis = new FileInputStream(file);
        return getKeyStore(fis, storePass);
    }

    /**
     * Get the Keystore given the URL to the keystore
     *
     * @param url
     * @param storePass
     *
     * @return
     *
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static KeyStore getKeyStore(URL url, char[] storePass) throws GeneralSecurityException, IOException {
        if (url == null)
            throw logger.nullArgumentError("url");

        return getKeyStore(url.openStream(), storePass);
    }

    /**
     * Get the Key Store <b>Note:</b> This method wants the InputStream to be not null.
     *
     * @param ksStream
     * @param storePass
     *
     * @return
     *
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws IllegalArgumentException if ksStream is null
     */
    public static KeyStore getKeyStore(InputStream ksStream, char[] storePass) throws GeneralSecurityException, IOException {
        if (ksStream == null)
            throw logger.nullArgumentError("InputStream for the KeyStore");
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(ksStream, storePass);
        return ks;
    }

    /**
     * Get the Public Key from the keystore
     *
     * @param ks
     * @param alias
     * @param password
     *
     * @return
     *
     * @throws GeneralSecurityException
     */
    public static PublicKey getPublicKey(KeyStore ks, String alias, char[] password) throws GeneralSecurityException {
        PublicKey publicKey = null;

        // Get private key
        Key key = ks.getKey(alias, password);
        if (key instanceof PrivateKey) {
            // Get certificate of public key
            Certificate cert = ks.getCertificate(alias);

            // Get public key
            publicKey = cert.getPublicKey();
        }
        // if alias is a certificate alias, get the public key from the certificate.
        if (publicKey == null) {
            Certificate cert = ks.getCertificate(alias);
            if (cert != null)
                publicKey = cert.getPublicKey();
        }
        return publicKey;
    }

    /**
     * Add a certificate to the KeyStore
     *
     * @param keystoreFile
     * @param storePass
     * @param alias
     * @param cert
     *
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static void addCertificate(File keystoreFile, char[] storePass, String alias, Certificate cert)
            throws GeneralSecurityException, IOException {
        KeyStore keystore = getKeyStore(keystoreFile, storePass);

        // Add the certificate
        keystore.setCertificateEntry(alias, cert);

        // Save the new keystore contents
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(keystoreFile);
            keystore.store(out, storePass);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ioe) {
                    // Ignore
                }
            }
        }
    }
}