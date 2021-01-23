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

package org.keycloak.authentication.authenticators.client;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.util.BasicAuthHelper;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates client based on "client_id" and "client_secret" sent either in request parameters or in "Authorization: Basic" header .
 *
 * See org.keycloak.adapters.authentication.ClientIdAndSecretAuthenticator for the adapter
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientIdAndSecretAuthenticator extends AbstractClientAuthenticator {

    public static final String PROVIDER_ID = "client-secret";

    @Override
    public void authenticateClient(ClientAuthenticationFlowContext context) {
        String client_id = null;
        String clientSecret = null;

        String authorizationHeader = context.getHttpRequest().getHttpHeaders().getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        MediaType mediaType = context.getHttpRequest().getHttpHeaders().getMediaType();
        boolean hasFormData = mediaType != null && mediaType.isCompatible(MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        MultivaluedMap<String, String> formData = hasFormData ? context.getHttpRequest().getDecodedFormParameters() : null;

        if (authorizationHeader != null) {
            String[] usernameSecret = BasicAuthHelper.parseHeader(authorizationHeader);
            if (usernameSecret != null) {
                client_id = usernameSecret[0];
                clientSecret = usernameSecret[1];
            } else {

                // Don't send 401 if client_id parameter was sent in request. For example IE may automatically send "Authorization: Negotiate" in XHR requests even for public clients
                if (formData != null && !formData.containsKey(OAuth2Constants.CLIENT_ID)) {
                    Response challengeResponse = Response.status(Response.Status.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"" + context.getRealm().getName() + "\"").build();
                    context.challenge(challengeResponse);
                    return;
                }
            }
        }

        if (formData != null) {
            // even if basic challenge response exist, we check if client id was explicitly set in the request as a form param,
            // so we can also support clients overriding flows and using challenges (e.g: basic) to authenticate their users
            if (formData.containsKey(OAuth2Constants.CLIENT_ID)) {
                client_id = formData.getFirst(OAuth2Constants.CLIENT_ID);
            }
            if (formData.containsKey(OAuth2Constants.CLIENT_SECRET)) {
                clientSecret = formData.getFirst(OAuth2Constants.CLIENT_SECRET);
            }
        }

        if (client_id == null) {
            client_id = context.getSession().getAttribute("client_id", String.class);
        }

        if (client_id == null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "Missing client_id parameter");
            context.challenge(challengeResponse);
            return;
        }

        context.getEvent().client(client_id);

        ClientModel client = context.getSession().clients().getClientByClientId(context.getRealm(), client_id);
        if (client == null) {
            context.failure(AuthenticationFlowError.CLIENT_NOT_FOUND, null);
            return;
        }

        context.setClient(client);

        if (!client.isEnabled()) {
            context.failure(AuthenticationFlowError.CLIENT_DISABLED, null);
            return;
        }

        // Skip client_secret validation for public client
        if (client.isPublicClient()) {
            context.success();
            return;
        }

        if (clientSecret == null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), "unauthorized_client", "Client secret not provided in request");
            context.challenge(challengeResponse);
            return;
        }

        if (client.getSecret() == null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), "unauthorized_client", "Invalid client secret");
            context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, challengeResponse);
            return;
        }

        if (!client.validateSecret(clientSecret)) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), "unauthorized_client", "Invalid client secret");
            context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, challengeResponse);
            return;
        }

        context.success();
    }

    @Override
    public String getDisplayType() {
        return "Client Id and Secret";
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
        return "Validates client based on 'client_id' and 'client_secret' sent either in request parameters or in 'Authorization: Basic' header";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new LinkedList<>();
    }

    @Override
    public List<ProviderConfigProperty> getConfigPropertiesPerClient() {
        // This impl doesn't use generic screen in admin console, but has its own screen. So no need to return anything here
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getAdapterConfiguration(ClientModel client) {
        Map<String, Object> result = new HashMap<>();
        result.put(CredentialRepresentation.SECRET, client.getSecret());
        return result;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Set<String> getProtocolAuthenticatorMethods(String loginProtocol) {
        if (loginProtocol.equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) {
            Set<String> results = new LinkedHashSet<>();
            results.add(OIDCLoginProtocol.CLIENT_SECRET_BASIC);
            results.add(OIDCLoginProtocol.CLIENT_SECRET_POST);
            return results;
        } else {
            return Collections.emptySet();
        }
    }
}
