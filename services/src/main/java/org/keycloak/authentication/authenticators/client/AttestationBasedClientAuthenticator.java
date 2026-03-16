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

package org.keycloak.authentication.authenticators.client;


import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.common.Profile;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;

import static jakarta.ws.rs.core.Response.Status.NOT_IMPLEMENTED;

/**
 * Attestation-Based Client Authentication based on Client Attestation JWT and PoP.
 * See <a href="https://datatracker.ietf.org/doc/draft-ietf-oauth-attestation-based-client-auth">specs</a> for more details.
 *
 * @author <a href="mailto:tdiesler@proton.me">Thomas Diesler</a>
 */
public class AttestationBasedClientAuthenticator extends AbstractClientAuthenticator implements EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "client-attestation";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void authenticateClient(ClientAuthenticationFlowContext context) {
        Response errorResponse = ClientAuthUtil.errorResponse(NOT_IMPLEMENTED.getStatusCode(), OAuthErrorException.UNAUTHORIZED_CLIENT,
                "Attestation-Based Client Authentication not (yet) supported");
        context.failure(AuthenticationFlowError.UNAUTHORIZED_CLIENT, errorResponse);
    }

    @Override
    public String getDisplayType() {
        return "Attestation-Based";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getHelpText() {
        return "Validates client based on a Client Attestation JWT and a PoP JWT which proves possession of the private key";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public List<ProviderConfigProperty> getConfigPropertiesPerClient() {
        return List.of();
    }

    @Override
    public Map<String, Object> getAdapterConfiguration(KeycloakSession session, ClientModel client) {
        return Map.of();
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.CLIENT_AUTH_ABCA);
    }

    @Override
    public Set<String> getProtocolAuthenticatorMethods(String loginProtocol) {
        if (loginProtocol.equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) {
            return Set.of(OIDCLoginProtocol.ATTEST_JWT_CLIENT_AUTH);
        } else {
            return Set.of();
        }
    }
}
