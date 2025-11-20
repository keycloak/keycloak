/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.broker.oidc;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.broker.oidc.KeycloakOIDCIdentityProvider;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;

public class TestKeycloakOidcIdentityProviderFactory extends KeycloakOIDCIdentityProviderFactory {

    public static final String ID = "test-keycloak-oidc";
    public static final String IGNORE_MAX_AGE_PARAM = "ignore-max-age-param";
    public static final String USE_SINGLE_REFRESH_TOKEN = "use-single-refresh-token";
    public static final String PREFERRED_USERNAME = "preferred-username";

    public static void setIgnoreMaxAgeParam(IdentityProviderRepresentation rep) {
        rep.getConfig().put(IGNORE_MAX_AGE_PARAM, Boolean.TRUE.toString());
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public KeycloakOIDCIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new KeycloakOIDCIdentityProvider(session, new OIDCIdentityProviderConfig(model)) {

            private static final Set<String> usernames = new HashSet<>();

            @Override
            public BrokeredIdentityContext getFederatedIdentity(String response) {
                BrokeredIdentityContext context = super.getFederatedIdentity(response);
                String preferredUsername = getPreferredUsername();

                if (preferredUsername != null) {
                    context.setUsername(preferredUsername);
                }
                if (Boolean.valueOf(model.getConfig().get(USE_SINGLE_REFRESH_TOKEN))) {
                    // refresh token will be available only in the first login.
                    if (!usernames.add(context.getUsername())) {
                        try {
                            AccessTokenResponse tokenResponse = JsonSerialization.readValue(context.getToken(), AccessTokenResponse.class);
                            tokenResponse.setRefreshToken(null);
                            context.setToken(JsonSerialization.writeValueAsString(tokenResponse));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                return context;
            }

            @Override
            protected UriBuilder createAuthorizationUrl(AuthenticationRequest request) {
                AuthenticationSessionModel authSession = request.getAuthenticationSession();
                String maxAge = authSession.getClientNote(OIDCLoginProtocol.MAX_AGE_PARAM);

                try {
                    if (isIgnoreMaxAgeParam()) {
                        authSession.removeClientNote(OIDCLoginProtocol.MAX_AGE_PARAM);
                    }
                    return super.createAuthorizationUrl(request);
                } finally {
                    authSession.setClientNote(OIDCLoginProtocol.MAX_AGE_PARAM, maxAge);
                }
            }

            private boolean isIgnoreMaxAgeParam() {
                return Boolean.parseBoolean(model.getConfig().getOrDefault(IGNORE_MAX_AGE_PARAM, Boolean.FALSE.toString()));
            }

            private String getPreferredUsername() {
                return model.getConfig().get(PREFERRED_USERNAME);
            }
        };
    }

    @Override
    public OIDCIdentityProviderConfig createConfig() {
        return new OIDCIdentityProviderConfig(super.createConfig()) {
            @Override
            public String getDisplayIconClasses() {
                return "my-custom-idp-icon";
            }
        };
    }
}
