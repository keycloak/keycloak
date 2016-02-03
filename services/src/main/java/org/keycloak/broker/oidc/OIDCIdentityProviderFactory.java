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

import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.representations.JSONWebKeySet;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.util.Map;

/**
 * @author Pedro Igor
 */
public class OIDCIdentityProviderFactory extends AbstractIdentityProviderFactory<OIDCIdentityProvider> {

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
        OIDCConfigurationRepresentation rep = null;
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
            String uri = rep.getJwksUri();
            String keySetString = null;
            try {
                keySetString = SimpleHttp.doGet(uri).asString();
                JSONWebKeySet keySet = JsonSerialization.readValue(keySetString, JSONWebKeySet.class);
                for (JWK jwk : keySet.getKeys()) {
                    JWKParser parse = JWKParser.create(jwk);
                    if (parse.getJwk().getPublicKeyUse().equals(JWK.SIG_USE) && keyTypeSupported(jwk.getKeyType())) {
                        PublicKey key = parse.toPublicKey();
                        config.setPublicKeySignatureVerifier(KeycloakModelUtils.getPemFromKey(key));
                        config.setValidateSignature(true);
                        break;
                    }

                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to query JWKSet from: " + uri, e);
            }

        }
        return config.getConfig();
    }

    protected static boolean keyTypeSupported(String type) {
        return type != null && type.equals("RSA");
    }

}
