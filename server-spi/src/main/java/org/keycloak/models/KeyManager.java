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

import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.enums.AuthProtocol;
import org.keycloak.keys.SecretKeyMetadata;
import org.keycloak.keys.RsaKeyMetadata;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface KeyManager {

    KeyWrapper getActiveKey(RealmModel realm, KeyUse use, String algorithm, AuthProtocol authProtocol);

    KeyWrapper getKey(RealmModel realm, String kid);

    /**
     * Returns all {@code KeyWrapper} for the given realm.
     *
     * @param realm {@code RealmModel}.
     * @return List of all {@code KeyWrapper} in the realm.
     * @deprecated Use {@link #getKeysStream(RealmModel) getKeysStream} instead.
     */
    @Deprecated
    default List<KeyWrapper> getKeys(RealmModel realm) {
        return getKeysStream(realm).collect(Collectors.toList());
    }

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
     * @param authProtocol {@code AuthProtocol}.
     * @return List of all {@code KeyWrapper} in the realm.
     * @deprecated Use {@link #getKeysStream(RealmModel, KeyUse, String, AuthProtocol) getKeysStream} instead.
     */
    @Deprecated
    default List<KeyWrapper> getKeys(RealmModel realm, KeyUse use, String algorithm, AuthProtocol authProtocol) {
        return getKeysStream(realm, use, algorithm, authProtocol).collect(Collectors.toList());
    }

    /**
     * Returns all {@code KeyWrapper} for the given realm that match given criteria.
     * @param realm {@code RealmModel}.
     * @param use {@code KeyUse}.
     * @param algorithm {@code String}.
     * @param authProtocol {@code AuthProtocol}.
     * @return Stream of all {@code KeyWrapper} in the realm. Never returns {@code null}.
     */
    Stream<KeyWrapper> getKeysStream(RealmModel realm, KeyUse use, String algorithm, AuthProtocol authProtocol);


}
