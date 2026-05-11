/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.broker.trust;

import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.broker.provider.TrustMaterialIdentityProvider;
import org.keycloak.broker.provider.TrustMaterialRequest;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.keys.PublicKeyStorageUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.Strings;
import org.keycloak.utils.StringUtil;

public class DefaultTrustIdentityProvider implements TrustMaterialIdentityProvider<DefaultTrustIdentityProviderConfig> {

    private final KeycloakSession session;
    private final DefaultTrustIdentityProviderConfig config;

    public DefaultTrustIdentityProvider(KeycloakSession session, DefaultTrustIdentityProviderConfig config) {
        this.session = session;
        this.config = config;
    }

    @Override
    public DefaultTrustIdentityProviderConfig getConfig() {
        return config;
    }

    @Override
    public Stream<JWK> resolveKeys(TrustMaterialRequest request) {
        PublicKeyLoader loader = new DefaultTrustMaterialPublicKeyLoader(session, config);
        PublicKeyStorageProvider keyStorage = session.getProvider(PublicKeyStorageProvider.class);
        String modelKey = PublicKeyStorageUtils.getIdpModelCacheKey(session.getContext().getRealm().getId(), config.getInternalId());
        Stream<KeyWrapper> keys = Strings.isEmpty(request.getKid())
                ? keyStorage.getKeys(modelKey, loader).stream()
                : Stream.of(keyStorage.getPublicKey(modelKey, request.getKid(), request.getAlgorithm(), loader));

        return filterKeys(keys.map(this::toJwk), request);
    }

    @Override
    public boolean reloadKeys() {
        if (!config.isEnabled() || StringUtil.isBlank(config.getTrustedJwksUrl())) {
            return false;
        }

        PublicKeyStorageProvider keyStorage = session.getProvider(PublicKeyStorageProvider.class);
        String modelKey = PublicKeyStorageUtils.getIdpModelCacheKey(session.getContext().getRealm().getId(), config.getInternalId());
        return keyStorage.reloadKeys(modelKey, new DefaultTrustMaterialPublicKeyLoader(session, config));
    }

    @Override
    public void close() {
    }

    private Stream<JWK> filterKeys(Stream<JWK> keys, TrustMaterialRequest request) {
        return keys
                .filter(Objects::nonNull)
                .filter(key -> Strings.isEmpty(request.getKid()) || Objects.equals(request.getKid(), key.getKeyId()))
                .filter(key -> Strings.isEmpty(request.getAlgorithm()) || Strings.isEmpty(key.getAlgorithm())
                        || Objects.equals(request.getAlgorithm(), key.getAlgorithm()))
                .filter(key -> Strings.isEmpty(key.getPublicKeyUse()) || Objects.equals(JWK.Use.SIG.asString(), key.getPublicKeyUse()));
    }

    private JWK toJwk(KeyWrapper key) {
        if (key == null || key.getPublicKey() == null) {
            return null;
        }

        JWKBuilder builder = JWKBuilder.create()
                .kid(key.getKid())
                .algorithm(key.getAlgorithmOrDefault());
        List<X509Certificate> certificates = Optional.ofNullable(key.getCertificateChain())
                .filter(certs -> !certs.isEmpty())
                .orElseGet(() -> Optional.ofNullable(key.getCertificate())
                        .map(Collections::singletonList)
                        .orElseGet(Collections::emptyList));
        Key publicKey = key.getPublicKey();

        if (Objects.equals(key.getType(), KeyType.RSA)) {
            return builder.rsa(publicKey, certificates, key.getUse());
        } else if (Objects.equals(key.getType(), KeyType.EC)) {
            return builder.ec(publicKey, certificates, key.getUse());
        } else if (Objects.equals(key.getType(), KeyType.OKP)) {
            return builder.okp(publicKey, key.getUse());
        }

        return null;
    }
}
