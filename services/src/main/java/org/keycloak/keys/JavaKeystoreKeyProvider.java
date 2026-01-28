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

package org.keycloak.keys;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.crypto.SecretKey;

import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.vault.VaultTranscriber;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JavaKeystoreKeyProvider implements KeyProvider {

    private final KeyStatus status;

    private final ComponentModel model;

    private final VaultTranscriber vault;

    private final KeyWrapper key;

    private final String algorithm;

    public JavaKeystoreKeyProvider(RealmModel realm, ComponentModel model, VaultTranscriber vault) {
        this.model = model;
        this.vault = vault;
        this.status = KeyStatus.from(model.get(Attributes.ACTIVE_KEY, true), model.get(Attributes.ENABLED_KEY, true));

        String defaultAlgorithmKey = KeyUse.ENC.name().equalsIgnoreCase(model.get(Attributes.KEY_USE)) ? JWEConstants.RSA_OAEP : Algorithm.RS256;
        this.algorithm = model.get(Attributes.ALGORITHM_KEY, defaultAlgorithmKey);

        KeyWrapper tmpKey = KeyNoteUtils.retrieveKeyFromNotes(model, KeyWrapper.class.getName());
        if (tmpKey == null) {
            tmpKey = loadKey(realm, model);
            KeyNoteUtils.attachKeyNotes(model, KeyWrapper.class.getName(), tmpKey);
        }
        this.key = tmpKey;
    }

    protected KeyWrapper loadKey(RealmModel realm, ComponentModel model) {
        String keystorePath = model.get(JavaKeystoreKeyProviderFactory.KEYSTORE_KEY);
        try (FileInputStream is = new FileInputStream(keystorePath)) {
            KeyStore keyStore = loadKeyStore(is, keystorePath);
            String keyAlias = model.get(JavaKeystoreKeyProviderFactory.KEY_ALIAS_KEY);

            return switch (algorithm) {
                case Algorithm.PS256, Algorithm.PS384, Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512 ->
                    loadRSAKey(keyStore, keyAlias, KeyUse.SIG);
                case Algorithm.RSA_OAEP, Algorithm.RSA1_5, Algorithm.RSA_OAEP_256 ->
                    loadRSAKey(keyStore, keyAlias, KeyUse.ENC);
                case Algorithm.ES256, Algorithm.ES384, Algorithm.ES512 ->
                    loadECKey(keyStore, keyAlias, KeyUse.SIG);
                case Algorithm.ECDH_ES, Algorithm.ECDH_ES_A128KW, Algorithm.ECDH_ES_A192KW, Algorithm.ECDH_ES_A256KW ->
                    loadECKey(keyStore, keyAlias, KeyUse.ENC);
                case Algorithm.EdDSA ->
                    loadEdDSAKey(keyStore, keyAlias, KeyUse.SIG);
                case Algorithm.AES ->
                    loadOctKey(keyStore, keyAlias, JavaAlgorithm.getJavaAlgorithm(algorithm), KeyUse.ENC);
                case Algorithm.HS256, Algorithm.HS384, Algorithm.HS512 ->
                    loadOctKey(keyStore, keyAlias, JavaAlgorithm.getJavaAlgorithm(algorithm), KeyUse.SIG);
                default -> throw new RuntimeException(String.format("Keys for algorithm %s are not supported.", algorithm));
            };
        } catch (KeyStoreException kse) {
            throw new RuntimeException("KeyStore error on server. " + kse.getMessage(), kse);
        } catch (FileNotFoundException fnfe) {
            throw new RuntimeException("File not found on server. " + fnfe.getMessage(), fnfe);
        } catch (IOException ioe) {
            throw new RuntimeException("IO error on server. " + ioe.getMessage(), ioe);
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("Algorithm not available on server. " + nsae.getMessage(), nsae);
        } catch (CertificateException ce) {
            throw new RuntimeException("Certificate error on server. " + ce.getMessage(), ce);
        } catch (UnrecoverableKeyException uke) {
            throw new RuntimeException("Key in the keystore cannot be recovered. " + uke.getMessage(), uke);
        } catch (GeneralSecurityException gse) {
            throw new RuntimeException("Invalid certificate chain. Check the order of certificates.", gse);
        }
    }

    private KeyStore loadKeyStore(FileInputStream inputStream, String keystorePath) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        // Use "JKS" as default type for backwards compatibility
        String keystoreType = KeystoreUtil.getKeystoreType(model.get(JavaKeystoreKeyProviderFactory.KEYSTORE_TYPE_KEY), keystorePath, "JKS");
        KeyStore keyStore = KeyStore.getInstance(keystoreType);
        String keystorePwd = model.get(JavaKeystoreKeyProviderFactory.KEYSTORE_PASSWORD_KEY);
        keystorePwd = vault.getStringSecret(keystorePwd).get().orElse(keystorePwd);
        keyStore.load(inputStream, keystorePwd.toCharArray());
        return keyStore;
    }

    private void checkUsage(KeyUse keyUse) throws GeneralSecurityException {
        String use = model.get(Attributes.KEY_USE);
        if (use != null && !keyUse.name().equalsIgnoreCase(use)) {
            throw new UnrecoverableKeyException(String.format("Invalid use %s for algorithm %s.", use, algorithm));
        }
    }

    private X509Certificate checkCertificate(Certificate cert) throws GeneralSecurityException {
        if (cert instanceof X509Certificate x509Cert) {
            return x509Cert;
        }
        throw new UnrecoverableKeyException(String.format("Invalid %s certificate in the entry.", (cert != null? cert.getType() : null)));
    }

    private <K extends KeyStore.Entry> K checkKeyEntry(KeyStore keyStore, String keyAlias, Class<K> clazz, KeyUse use) throws GeneralSecurityException {
        checkUsage(use);
        String keyPwd = model.get(JavaKeystoreKeyProviderFactory.KEY_PASSWORD_KEY);
        keyPwd = vault.getStringSecret(keyPwd).get().orElse(keyPwd);
        KeyStore.Entry keyEntry = keyStore.getEntry(keyAlias, new KeyStore.PasswordProtection(keyPwd.toCharArray()));
        if (keyEntry == null) {
            throw new UnrecoverableKeyException(String.format("Alias %s does not exists in the keystore.", keyAlias));
        }
        if (!clazz.isInstance(keyEntry)) {
            throw new UnrecoverableKeyException(String.format("Invalid %s key for alias %s. Key is not %s.", algorithm, keyAlias, clazz.getSimpleName()));
        }
        return clazz.cast(keyEntry);
    }

    private <K extends Key> K checkKey(Key key, String keyAlias, Class<K> clazz, String javaAlgorithm) throws GeneralSecurityException {
        if (!clazz.isInstance(key) || (javaAlgorithm != null && !javaAlgorithm.equalsIgnoreCase(key.getAlgorithm()))) {
            throw new NoSuchAlgorithmException(String.format("Invalid %s key for alias %s. Algorithm is %s.", algorithm, keyAlias, key.getAlgorithm()));
        }
        return clazz.cast(key);
    }

    private KeyWrapper loadOctKey(KeyStore keyStore, String keyAlias, String javaAlgorithm, KeyUse keyUse) throws GeneralSecurityException {
        KeyStore.SecretKeyEntry secretKeyEntry = checkKeyEntry(keyStore, keyAlias, KeyStore.SecretKeyEntry.class, keyUse);
        SecretKey secretKey = checkKey(secretKeyEntry.getSecretKey(), keyAlias, SecretKey.class, javaAlgorithm);

        return createKeyWrapper(secretKey, keyUse);
    }

    private KeyWrapper loadEdDSAKey(KeyStore keyStore, String keyAlias, KeyUse keyUse) throws GeneralSecurityException {
        KeyStore.PrivateKeyEntry privateKeyEntry = checkKeyEntry(keyStore, keyAlias, KeyStore.PrivateKeyEntry.class, keyUse);
        EdECPrivateKey privateKey = checkKey(privateKeyEntry.getPrivateKey(), keyAlias, EdECPrivateKey.class, null);
        X509Certificate x509Cert = checkCertificate(privateKeyEntry.getCertificate());
        try {
           JavaAlgorithm.getJavaAlgorithmForHash(Algorithm.EdDSA, privateKey.getParams().getName());
        } catch (RuntimeException e) {
            throw new UnrecoverableKeyException(String.format("Invalid EdDSA curve for alias %s. Curve algorithm is %s.",
                    keyAlias, privateKey.getParams().getName()));
        }

        PublicKey publicKey = x509Cert.getPublicKey();
        KeyPair keyPair = new KeyPair(publicKey, privateKey);
        return createKeyWrapper(keyPair, x509Cert, loadCertificateChain(privateKeyEntry), KeyType.OKP, keyUse, privateKey.getParams().getName());
    }

    private KeyWrapper loadECKey(KeyStore keyStore, String keyAlias, KeyUse keyUse) throws GeneralSecurityException {
        KeyStore.PrivateKeyEntry privateKeyEntry = checkKeyEntry(keyStore, keyAlias, KeyStore.PrivateKeyEntry.class, keyUse);
        ECPrivateKey privateKey = checkKey(privateKeyEntry.getPrivateKey(), keyAlias, ECPrivateKey.class, null);
        X509Certificate x509Cert = checkCertificate(privateKeyEntry.getCertificate());
        PublicKey publicKey = x509Cert.getPublicKey();
        KeyPair keyPair = new KeyPair(publicKey, privateKey);
        return createKeyWrapper(keyPair, x509Cert, loadCertificateChain(privateKeyEntry), KeyType.EC, keyUse, null);
    }

    private KeyWrapper loadRSAKey(KeyStore keyStore, String keyAlias, KeyUse keyUse) throws GeneralSecurityException {
        KeyStore.PrivateKeyEntry privateKeyEntry = checkKeyEntry(keyStore, keyAlias, KeyStore.PrivateKeyEntry.class, keyUse);
        RSAPrivateCrtKey privateKey = checkKey(privateKeyEntry.getPrivateKey(), keyAlias, RSAPrivateCrtKey.class, null);
        X509Certificate x509Cert = checkCertificate(privateKeyEntry.getCertificate());
        PublicKey publicKey = x509Cert.getPublicKey();
        KeyPair keyPair = new KeyPair(publicKey, privateKey);
        return createKeyWrapper(keyPair, x509Cert, loadCertificateChain(privateKeyEntry), KeyType.RSA, keyUse, null);
    }

    private List<X509Certificate> loadCertificateChain(KeyStore.PrivateKeyEntry privateKeyEntry) throws GeneralSecurityException {
        return Optional.ofNullable(privateKeyEntry.getCertificateChain())
                .map(certificates -> Arrays.stream(certificates)
                        .map(X509Certificate.class::cast)
                        .collect(Collectors.toList()))
                .orElseGet(Collections::emptyList);
    }

    private KeyWrapper createKeyWrapper(KeyPair keyPair, X509Certificate certificate, List<X509Certificate> certificateChain,
            String type, KeyUse keyUse, String curve) {
        KeyWrapper key = new KeyWrapper();

        key.setProviderId(model.getId());
        key.setProviderPriority(model.get("priority", 0L));

        key.setKid(model.get(Attributes.KID_KEY) != null ? model.get(Attributes.KID_KEY) : KeyUtils.createKeyId(keyPair.getPublic()));
        key.setUse(keyUse);
        key.setType(type);
        key.setAlgorithm(algorithm);
        key.setCurve(curve);
        key.setStatus(status);
        key.setPrivateKey(keyPair.getPrivate());
        key.setPublicKey(keyPair.getPublic());
        key.setCertificate(certificate);

        if (!certificateChain.isEmpty()) {
            if (certificate != null && !certificate.equals(certificateChain.get(0))) {
                // just in case the chain does not contain the end-user certificate
                certificateChain.add(0, certificate);
            }
            key.setCertificateChain(certificateChain);
        }

        return key;
    }

    private KeyWrapper createKeyWrapper(SecretKey secretKey, KeyUse use) {
        KeyWrapper keyWrapper = new KeyWrapper();

        keyWrapper.setProviderId(model.getId());
        keyWrapper.setProviderPriority(model.get("priority", 0l));

        keyWrapper.setKid(model.get(Attributes.KID_KEY, KeycloakModelUtils.generateId()));
        keyWrapper.setUse(use);
        keyWrapper.setType(KeyType.OCT);
        keyWrapper.setAlgorithm(algorithm);
        keyWrapper.setStatus(status);
        keyWrapper.setSecretKey(secretKey);
        return keyWrapper;
    }

    @Override
    public Stream<KeyWrapper> getKeysStream() {
        return Stream.of(key);
    }
}