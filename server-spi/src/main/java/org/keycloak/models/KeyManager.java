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

import org.keycloak.keys.HmacKeyMetadata;
import org.keycloak.keys.RsaKeyMetadata;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface KeyManager {

    ActiveRsaKey getActiveRsaKey(RealmModel realm);

    PublicKey getRsaPublicKey(RealmModel realm, String kid);

    Certificate getRsaCertificate(RealmModel realm, String kid);

    List<RsaKeyMetadata> getRsaKeys(RealmModel realm, boolean includeDisabled);

    ActiveHmacKey getActiveHmacKey(RealmModel realm);

    SecretKey getHmacSecretKey(RealmModel realm, String kid);

    List<HmacKeyMetadata> getHmacKeys(RealmModel realm, boolean includeDisabled);

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

}
