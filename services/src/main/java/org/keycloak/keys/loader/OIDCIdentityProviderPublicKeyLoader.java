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

package org.keycloak.keys.loader;

import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.utils.JWKSHttpUtils;
import org.keycloak.util.JWKSUtils;

import java.security.PublicKey;
import java.util.Collections;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCIdentityProviderPublicKeyLoader implements PublicKeyLoader {

    private static final Logger logger = Logger.getLogger(OIDCIdentityProviderPublicKeyLoader.class);

    private final KeycloakSession session;
    private final OIDCIdentityProviderConfig config;

    public OIDCIdentityProviderPublicKeyLoader(KeycloakSession session, OIDCIdentityProviderConfig config) {
        this.session = session;
        this.config = config;
    }

    @Override
    public Map<String, PublicKey> loadKeys() throws Exception {
        if (config.isUseJwksUrl()) {
            String jwksUrl = config.getJwksUrl();
            JSONWebKeySet jwks = JWKSHttpUtils.sendJwksRequest(session, jwksUrl);
            return JWKSUtils.getKeysForUse(jwks, JWK.Use.SIG);
        } else {
            try {
                PublicKey publicKey = getSavedPublicKey();
                if (publicKey == null) {
                    return Collections.emptyMap();
                }

                String presetKeyId = config.getPublicKeySignatureVerifierKeyId();
                String kid = (presetKeyId == null || presetKeyId.trim().isEmpty())
                  ? KeyUtils.createKeyId(publicKey)
                  : presetKeyId;
                return Collections.singletonMap(kid, publicKey);
            } catch (Exception e) {
                logger.warnf(e, "Unable to retrieve publicKey for verify signature of identityProvider '%s' . Error details: %s", config.getAlias(), e.getMessage());
                return Collections.emptyMap();
            }
        }
    }

    protected PublicKey getSavedPublicKey() throws Exception {
        if (config.getPublicKeySignatureVerifier() != null && !config.getPublicKeySignatureVerifier().trim().equals("")) {
            return PemUtils.decodePublicKey(config.getPublicKeySignatureVerifier());
        } else {
            logger.warnf("No public key saved on identityProvider %s", config.getAlias());
            return null;
        }
    }
}
