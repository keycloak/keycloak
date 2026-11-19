/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.social.linkedin;

import java.io.IOException;
import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 * <p>Specific OIDC LinkedIn provider for <b>Sign In with LinkedIn using OpenID Connect</b>
 * product app. LinkedIn currently has two issues with default OIDC provider
 * implementation:</p>
 *
 * <ol>
 * <li>The jwks endpoint does not contain <em>use</em> claim for the signature key.</li>
 * <li>The nonce in the authentication request is not returned back in the ID Token.</li>
 * </ol>
 *
 * <p>This factory workarounds the default provider to overcome the issues.</p>
 *
 * @author rmartinc
 */
public class LinkedInOIDCIdentityProviderFactory extends AbstractIdentityProviderFactory<LinkedInOIDCIdentityProvider> implements SocialIdentityProviderFactory<LinkedInOIDCIdentityProvider> {

    public static final String PROVIDER_ID = "linkedin-openid-connect";
    public static final String WELL_KNOWN_URL = "https://www.linkedin.com/oauth/.well-known/openid-configuration";

    // well known oidc metadata is cached as static property
    private static OIDCConfigurationRepresentation metadata;

    @Override
    public String getName() {
        return "LinkedIn";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public LinkedInOIDCIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        OIDCConfigurationRepresentation local = metadata;
        if (local == null) {
            local = getWellKnownMetadata(session);
            if (local.getIssuer() == null || local.getTokenEndpoint() == null || local.getAuthorizationEndpoint()== null || local.getJwksUri() == null) {
                throw new RuntimeException("Invalid data in the OIDC LinkedIn well-known address.");
            }
            metadata = local;
        }
        OIDCIdentityProviderConfig config = new OIDCIdentityProviderConfig(model);
        config.setIssuer(local.getIssuer());
        config.setAuthorizationUrl(local.getAuthorizationEndpoint());
        config.setTokenUrl(local.getTokenEndpoint());
        if (local.getUserinfoEndpoint() != null) {
            config.setUserInfoUrl(local.getUserinfoEndpoint());
        }
        config.setUseJwksUrl(true);
        config.setJwksUrl(local.getJwksUri());
        config.setValidateSignature(true);
        config.setDisableNonce(true); // linkedin does not manage nonce correctly
        return new LinkedInOIDCIdentityProvider(session, config);
    }

    @Override
    public OIDCIdentityProviderConfig createConfig() {
        return new OIDCIdentityProviderConfig();
    }

    private static OIDCConfigurationRepresentation getWellKnownMetadata(KeycloakSession session) {
        try (SimpleHttpResponse response = SimpleHttp.create(session).doGet(WELL_KNOWN_URL)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .asResponse()) {
            if (Response.Status.fromStatusCode(response.getStatus()).getFamily() != Response.Status.Family.SUCCESSFUL) {
                throw new RuntimeException("Error calling the OIDC LinkedIn well-known address. Http status " + response.getStatus());
            }
            return response.asJson(OIDCConfigurationRepresentation.class);
        } catch (IOException e) {
            throw new RuntimeException("Error calling the OIDC LinkedIn well-known address.", e);
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        // we can add some common OIDC config parameters here if needed
        return ProviderConfigurationBuilder.create()
                .build();
    }
}
