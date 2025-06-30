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

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.models.RealmModel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JavaKeystoreKeyProvider implements KeyProvider {

    private final KeyStatus status;

    private final ComponentModel model;

    private final KeyWrapper key;

    private final String algorithm;

    public JavaKeystoreKeyProvider(RealmModel realm, ComponentModel model) {
        this.model = model;
        this.status = KeyStatus.from(model.get(Attributes.ACTIVE_KEY, true), model.get(Attributes.ENABLED_KEY, true));

        String defaultAlgorithmKey = KeyUse.ENC.name().equals(model.get(Attributes.KEY_USE)) ? JWEConstants.RSA_OAEP : Algorithm.RS256;
        this.algorithm = model.get(Attributes.ALGORITHM_KEY, defaultAlgorithmKey);

        if (model.hasNote(KeyWrapper.class.getName())) {
            key = model.getNote(KeyWrapper.class.getName());
        } else {
            key = loadKey(realm, model);
            model.setNote(KeyWrapper.class.getName(), key);
        }
    }

    protected KeyWrapper loadKey(RealmModel realm, ComponentModel model) {
        String keystorePath = model.get(JavaKeystoreKeyProviderFactory.KEYSTORE_KEY);
        try (FileInputStream is = new FileInputStream(keystorePath)) {
            KeyStore keyStore = loadKeyStore(is, keystorePath);
            String keyAlias = model.get(JavaKeystoreKeyProviderFactory.KEY_ALIAS_KEY);

            return switch (algorithm) {
                case Algorithm.PS256, Algorithm.PS384, Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512,
                        Algorithm.RSA_OAEP, Algorithm.RSA1_5, Algorithm.RSA_OAEP_256 ->
                        loadRSAKey(realm, model, keyStore, keyAlias);
                case Algorithm.ES256, Algorithm.ES384, Algorithm.ES512 -> loadECKey(realm, model, keyStore, keyAlias);
                default ->
                        throw new RuntimeException(String.format("Keys for algorithm %s are not supported.", algorithm));
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
            throw new RuntimeException("Keystore on server can not be recovered. " + uke.getMessage(), uke);
        } catch (GeneralSecurityException gse) {
            throw new RuntimeException("Invalid certificate chain. Check the order of certificates.", gse);
        }
    }

    private KeyStore loadKeyStore(FileInputStream inputStream, String keystorePath) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        // Use "JKS" as default type for backwards compatibility
        String keystoreType = KeystoreUtil.getKeystoreType(model.get(JavaKeystoreKeyProviderFactory.KEYSTORE_TYPE_KEY), keystorePath, "JKS");
        KeyStore keyStore = KeyStore.getInstance(keystoreType);
        keyStore.load(inputStream, model.get(JavaKeystoreKeyProviderFactory.KEYSTORE_PASSWORD_KEY).toCharArray());
        return keyStore;
    }


    private KeyWrapper loadECKey(RealmModel realm, ComponentModel model, KeyStore keyStore, String keyAlias) throws GeneralSecurityException {
        ECPrivateKey privateKey = (ECPrivateKey) keyStore.getKey(keyAlias, model.get(JavaKeystoreKeyProviderFactory.KEY_PASSWORD_KEY).toCharArray());
        String curve = AbstractEcdsaKeyProviderFactory.convertECDomainParmNistRepToSecRep(AbstractEcdsaKeyProviderFactory.convertAlgorithmToECDomainParmNistRep(algorithm));

        PublicKey publicKey = CryptoIntegration.getProvider().getEcdsaCryptoProvider().getPublicFromPrivate(privateKey);

        KeyPair keyPair = new KeyPair(publicKey, privateKey);

        return createKeyWrapper(keyPair, getCertificate(keyStore, keyPair, keyAlias, realm.getName()), loadCertificateChain(keyStore, keyAlias), KeyType.EC);

    }

    private X509Certificate getCertificate(KeyStore keyStore, KeyPair keyPair, String keyAlias, String realmName) throws KeyStoreException {
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate(keyAlias);
        if (certificate == null) {
            certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, realmName);
        }
        return certificate;
    }

    private KeyWrapper loadRSAKey(RealmModel realm, ComponentModel model, KeyStore keyStore, String keyAlias) throws GeneralSecurityException {
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, model.get(JavaKeystoreKeyProviderFactory.KEY_PASSWORD_KEY).toCharArray());
        PublicKey publicKey = KeyUtils.extractPublicKey(privateKey);

        KeyPair keyPair = new KeyPair(publicKey, privateKey);

        return createKeyWrapper(keyPair, getCertificate(keyStore, keyPair, keyAlias, realm.getName()), loadCertificateChain(keyStore, keyAlias), KeyType.RSA);
    }

    private List<X509Certificate> loadCertificateChain(KeyStore keyStore, String keyAlias) throws GeneralSecurityException {
        List<X509Certificate> chain = Optional.ofNullable(keyStore.getCertificateChain(keyAlias))
                .map(certificates -> Arrays.stream(certificates)
                        .map(X509Certificate.class::cast)
                        .collect(Collectors.toList()))
                .orElseGet(Collections::emptyList);

        validateCertificateChain(chain);

        return chain;
    }

    private KeyWrapper createKeyWrapper(KeyPair keyPair, X509Certificate certificate, List<X509Certificate> certificateChain, String type) {
        KeyUse keyUse = KeyUse.valueOf(model.get(Attributes.KEY_USE, KeyUse.SIG.getSpecName()).toUpperCase());

        KeyWrapper key = new KeyWrapper();

        key.setProviderId(model.getId());
        key.setProviderPriority(model.get("priority", 0L));

        key.setKid(model.get(Attributes.KID_KEY) != null ? model.get(Attributes.KID_KEY) : KeyUtils.createKeyId(keyPair.getPublic()));
        key.setUse(keyUse);
        key.setType(type);
        key.setAlgorithm(algorithm);
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

    /**
     * <p>Validates the giving certificate chain represented by {@code certificates}. If the list of certificates is empty
     * or does not have at least 2 certificates (end-user certificate plus intermediary/root CAs) this method does nothing.
     *
     * <p>It should not be possible to import to keystores invalid chains though. So this is just an additional check
     * that we can reuse later for other purposes when the cert chain is also provided manually, in PEM.
     *
     * @param certificates
     */
    private void validateCertificateChain(List<X509Certificate> certificates) throws GeneralSecurityException {
        if (certificates == null || certificates.isEmpty()) {
            return;
        }

        Set<TrustAnchor> anchors = new HashSet<>();

        // consider the last certificate in the chain as the most trusted cert
        anchors.add(new TrustAnchor(certificates.get(certificates.size() - 1), null));

        PKIXParameters params = new PKIXParameters(anchors);

        params.setRevocationEnabled(false);

        CertPath certPath = CertificateFactory.getInstance("X.509").generateCertPath(certificates);
        CertPathValidator validator = CertPathValidator.getInstance(CertPathValidator.getDefaultType());

        validator.validate(certPath, params);
    }


    @Override
    public Stream<KeyWrapper> getKeysStream() {
        return Stream.of(key);
    }
}