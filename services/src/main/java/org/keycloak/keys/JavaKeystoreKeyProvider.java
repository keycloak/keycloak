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

import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JavaKeystoreKeyProvider extends AbstractRsaKeyProvider {

    public JavaKeystoreKeyProvider(RealmModel realm, ComponentModel model) {
        super(realm, model);
    }

    @Override
    protected KeyWrapper loadKey(RealmModel realm, ComponentModel model) {
        try (FileInputStream is = new FileInputStream(model.get(JavaKeystoreKeyProviderFactory.KEYSTORE_KEY))) {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(is, model.get(JavaKeystoreKeyProviderFactory.KEYSTORE_PASSWORD_KEY).toCharArray());

            String keyAlias = model.get(JavaKeystoreKeyProviderFactory.KEY_ALIAS_KEY);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, model.get(JavaKeystoreKeyProviderFactory.KEY_PASSWORD_KEY).toCharArray());
            PublicKey publicKey = KeyUtils.extractPublicKey(privateKey);

            KeyPair keyPair = new KeyPair(publicKey, privateKey);

            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(keyAlias);
            if (certificate == null) {
                certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, realm.getName());
            }

            KeyUse keyUse = KeyUse.valueOf(model.get(Attributes.KEY_USE, KeyUse.SIG.getSpecName()).toUpperCase());

            return createKeyWrapper(keyPair, certificate, loadCertificateChain(keyStore, keyAlias), keyUse);
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

    private List<X509Certificate> loadCertificateChain(KeyStore keyStore, String keyAlias) throws GeneralSecurityException {
        List<X509Certificate> chain = Optional.ofNullable(keyStore.getCertificateChain(keyAlias))
                .map(certificates -> Arrays.stream(certificates)
                        .map(X509Certificate.class::cast)
                        .collect(Collectors.toList()))
                .orElseGet(Collections::emptyList);

        validateCertificateChain(chain);

        return chain;
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
}
