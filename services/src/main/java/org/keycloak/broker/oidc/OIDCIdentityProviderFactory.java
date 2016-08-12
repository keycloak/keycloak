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
package org.keycloak.broker.oidc;

import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.protocol.oidc.utils.JWKSUtils;
import org.keycloak.services.ServicesLogger;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.util.Map;

/**
 * @author Pedro Igor
 */
public class OIDCIdentityProviderFactory extends AbstractIdentityProviderFactory<OIDCIdentityProvider> {

    private static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    public static final String PROVIDER_ID = "oidc";

    @Override
    public String getName() {
        return "OpenID Connect v1.0";
    }

    @Override
    public OIDCIdentityProvider create(IdentityProviderModel model) {
        return new OIDCIdentityProvider(new OIDCIdentityProviderConfig(model));
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Map<String, String> parseConfig(InputStream inputStream) {
        return parseOIDCConfig(inputStream);
    }

    protected static Map<String, String> parseOIDCConfig(InputStream inputStream) {
        OIDCConfigurationRepresentation rep;
        try {
            rep = JsonSerialization.readValue(inputStream, OIDCConfigurationRepresentation.class);
        } catch (IOException e) {
            throw new RuntimeException("failed to load openid connect metadata", e);
        }
        OIDCIdentityProviderConfig config = new OIDCIdentityProviderConfig(new IdentityProviderModel());
        config.setIssuer(rep.getIssuer());
        config.setLogoutUrl(rep.getLogoutEndpoint());
        config.setAuthorizationUrl(rep.getAuthorizationEndpoint());
        config.setTokenUrl(rep.getTokenEndpoint());
        config.setUserInfoUrl(rep.getUserinfoEndpoint());
        if (rep.getJwksUri() != null) {
            sendJwksRequest(rep, config);
        }
        return config.getConfig();
    }

    protected static void sendJwksRequest(OIDCConfigurationRepresentation rep, OIDCIdentityProviderConfig config) {
        try {
            JSONWebKeySet keySet = JWKSUtils.sendJwksRequest(rep.getJwksUri());
            PublicKey key = JWKSUtils.getKeyForUse(keySet, JWK.Use.SIG);
            if (key == null) {
                logger.supportedJwkNotFound(JWK.Use.SIG.asString());
            } else {
                config.setPublicKeySignatureVerifier(KeycloakModelUtils.getPemFromKey(key));
                config.setValidateSignature(true);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to query JWKSet from: " + rep.getJwksUri(), e);
        }
    }

}
