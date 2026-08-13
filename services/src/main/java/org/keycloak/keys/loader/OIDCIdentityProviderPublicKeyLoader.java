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

import org.keycloak.broker.jwtauthorizationgrant.JWTAuthorizationGrantConfig;
import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.utils.JWKSHttpUtils;
import org.keycloak.util.JWKSUtils;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCIdentityProviderPublicKeyLoader implements PublicKeyLoader {

    private static final Logger logger = Logger.getLogger(OIDCIdentityProviderPublicKeyLoader.class);

    private final KeycloakSession session;
    private final JWTAuthorizationGrantConfig config;

    public OIDCIdentityProviderPublicKeyLoader(KeycloakSession session, JWTAuthorizationGrantConfig config) {
        this.session = session;
        this.config = config;
    }

    @Override
    public PublicKeysWrapper loadKeys() throws Exception {
        if (config.isUseJwksUrl()) {
            String jwksUrl = config.getJwksUrl();
            JSONWebKeySet jwks = JWKSHttpUtils.sendJwksRequest(session, jwksUrl);
            return JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.SIG, true);
        } else {
            String publicKeySignatureVerifier = config.getPublicKeySignatureVerifier();
            if (StringUtil.isBlank(publicKeySignatureVerifier)) {
                return PublicKeysWrapper.EMPTY;
            }
            try {
                // only load jwks, direct pem public key needs to load a hardcoded key locator
                JSONWebKeySet jwks = JsonSerialization.readValue(publicKeySignatureVerifier, JSONWebKeySet.class);
                return JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.SIG);
            } catch (Exception e) {
                logger.warnf(e, "Unable to retrieve publicKey for verify signature of identityProvider '%s'.", config.getAlias());
                return PublicKeysWrapper.EMPTY;
            }
        }
    }
}
