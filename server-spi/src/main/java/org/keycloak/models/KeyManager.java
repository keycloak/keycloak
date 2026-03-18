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

package org.keycloak.models;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Stream;
import javax.crypto.SecretKey;

import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.keys.RsaKeyMetadata;
import org.keycloak.keys.SecretKeyMetadata;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface KeyManager {

    KeyWrapper getActiveKey(RealmModel realm, KeyUse use, String algorithm);

    KeyWrapper getKey(RealmModel realm, String kid, KeyUse use, String algorithm);

    /**
     * Returns all {@code KeyWrapper} for the given realm.
     * @param realm {@code RealmModel}.
     * @return Stream of all {@code KeyWrapper} in the realm. Never returns {@code null}.
     */
    Stream<KeyWrapper> getKeysStream(RealmModel realm);

    /**
     * Returns all {@code KeyWrapper} for the given realm that match given criteria.
     * @param realm {@code RealmModel}.
     * @param use {@code KeyUse}.
     * @param algorithm {@code String}.
     * @return Stream of all {@code KeyWrapper} in the realm. Never returns {@code null}.
     */
    Stream<KeyWrapper> getKeysStream(RealmModel realm, KeyUse use, String algorithm);

    @Deprecated
    ActiveRsaKey getActiveRsaKey(RealmModel realm);

    @Deprecated
    PublicKey getRsaPublicKey(RealmModel realm, String kid);

    @Deprecated
    Certificate getRsaCertificate(RealmModel realm, String kid);

    @Deprecated
    List<RsaKeyMetadata> getRsaKeys(RealmModel realm);

    @Deprecated
    ActiveHmacKey getActiveHmacKey(RealmModel realm);

    @Deprecated
    SecretKey getHmacSecretKey(RealmModel realm, String kid);

    @Deprecated
    List<SecretKeyMetadata> getHmacKeys(RealmModel realm);

    @Deprecated
    ActiveAesKey getActiveAesKey(RealmModel realm);

    @Deprecated
    SecretKey getAesSecretKey(RealmModel realm, String kid);

    @Deprecated
    List<SecretKeyMetadata> getAesKeys(RealmModel realm);

    class ActiveRsaKey {
        private final String kid;
        private final PrivateKey privateKey;
        private final PublicKey publicKey;
        private final X509Certificate certificate;

        public ActiveRsaKey(String kid, PrivateKey privateKey, PublicKey publicKey, X509Certificate certificate) {
            this.kid = kid;
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.certificate = certificate;
        }

        public ActiveRsaKey(KeyWrapper keyWrapper) {
            this(keyWrapper.getKid(), (PrivateKey) keyWrapper.getPrivateKey(), (PublicKey) keyWrapper.getPublicKey(), keyWrapper.getCertificate());
        }

        public String getKid() {
            return kid;
        }

        public PrivateKey getPrivateKey() {
            return privateKey;
        }

        public PublicKey getPublicKey() {
            return publicKey;
        }

        public X509Certificate getCertificate() {
            return certificate;
        }
    }

    class ActiveHmacKey {
        private final String kid;
        private final SecretKey secretKey;

        public ActiveHmacKey(String kid, SecretKey secretKey) {
            this.kid = kid;
            this.secretKey = secretKey;
        }

        public String getKid() {
            return kid;
        }

        public SecretKey getSecretKey() {
            return secretKey;
        }
    }

    class ActiveAesKey {
        private final String kid;
        private final SecretKey secretKey;

        public ActiveAesKey(String kid, SecretKey secretKey) {
            this.kid = kid;
            this.secretKey = secretKey;
        }

        public String getKid() {
            return kid;
        }

        public SecretKey getSecretKey() {
            return secretKey;
        }
    }


}
