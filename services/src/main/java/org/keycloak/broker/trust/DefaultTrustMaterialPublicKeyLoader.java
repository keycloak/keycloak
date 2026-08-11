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

import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.utils.JWKSHttpUtils;
import org.keycloak.util.JWKSUtils;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

public class DefaultTrustMaterialPublicKeyLoader implements PublicKeyLoader {

    private final KeycloakSession session;
    private final DefaultTrustIdentityProviderConfig config;

    public DefaultTrustMaterialPublicKeyLoader(KeycloakSession session, DefaultTrustIdentityProviderConfig config) {
        this.session = session;
        this.config = config;
    }

    @Override
    public PublicKeysWrapper loadKeys() throws Exception {
        if (config.isUseJwksUrl() && StringUtil.isNotBlank(config.getTrustedJwksUrl())) {
            JSONWebKeySet jwks = JWKSHttpUtils.sendJwksRequest(session, config.getTrustedJwksUrl());
            return JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.SIG, true);
        }

        if (!config.isUseJwksUrl() && StringUtil.isNotBlank(config.getTrustedJwks())) {
            JSONWebKeySet jwks = JsonSerialization.readValue(config.getTrustedJwks(), JSONWebKeySet.class);
            return JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.SIG, true);
        }

        return PublicKeysWrapper.EMPTY;
    }
}
