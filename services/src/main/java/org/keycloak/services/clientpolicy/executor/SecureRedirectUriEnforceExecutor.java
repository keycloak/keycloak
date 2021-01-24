/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.executor;

import java.util.Arrays;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyLogger;
import org.keycloak.services.clientpolicy.context.AdminClientRegisterContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdateContext;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;
import org.keycloak.services.clientpolicy.context.DynamicClientRegisterContext;
import org.keycloak.services.clientpolicy.context.DynamicClientUpdateContext;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class SecureRedirectUriEnforceExecutor implements ClientPolicyExecutorProvider {

    private static final Logger logger = Logger.getLogger(SecureRedirectUriEnforceExecutor.class);
    private static final String LOGMSG_PREFIX = "CLIENT-POLICY";
    private String logMsgPrefix() {
        return LOGMSG_PREFIX + "@" + session.hashCode() + " :: EXECUTOR";
    }

    private final KeycloakSession session;

    public SecureRedirectUriEnforceExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(Object config) {
        // no setup configuration item
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Configuration {
    }

    @Override
    public String getProviderId() {
        return SecureRedirectUriEnforceExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case REGISTER:
                if (context instanceof AdminClientRegisterContext || context instanceof DynamicClientRegisterContext) {
                    confirmSecureRedirectUris(((ClientCRUDContext)context).getProposedClientRepresentation().getRedirectUris());
                } else {
                    throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not allowed input format.");
                }
                return;
            case UPDATE:
                if (context instanceof AdminClientUpdateContext || context instanceof DynamicClientUpdateContext) {
                    confirmSecureRedirectUris(((ClientCRUDContext)context).getProposedClientRepresentation().getRedirectUris());
                } else {
                    throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not allowed input format.");
                }
                return;
            case AUTHORIZATION_REQUEST:
                confirmSecureRedirectUris(Arrays.asList(((AuthorizationRequestContext)context).getRedirectUri()));
                return;
            default:
                return;
        }
    }

    private void confirmSecureRedirectUris(List<String> redirectUris) throws ClientPolicyException {
        if (redirectUris == null || redirectUris.isEmpty()) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT_METADATA, "Invalid client metadata: redirect_uris");
        }

        for(String redirectUri : redirectUris) {
            ClientPolicyLogger.logv(logger, "{0} :: Redirect URI = {1}", logMsgPrefix(), redirectUri);
            if (redirectUri.startsWith("http://") || redirectUri.contains("*")) {
                throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT_METADATA, "Invalid client metadata: redirect_uris");
            }
        }
    }

}