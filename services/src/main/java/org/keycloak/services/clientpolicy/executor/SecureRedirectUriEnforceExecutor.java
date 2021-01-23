/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientpolicy.AdminClientRegisterContext;
import org.keycloak.services.clientpolicy.AdminClientUpdateContext;
import org.keycloak.services.clientpolicy.AuthorizationRequestContext;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyLogger;
import org.keycloak.services.clientpolicy.ClientUpdateContext;
import org.keycloak.services.clientpolicy.DynamicClientRegisterContext;
import org.keycloak.services.clientpolicy.DynamicClientUpdateContext;

public class SecureRedirectUriEnforceExecutor implements ClientPolicyExecutorProvider {

    private static final Logger logger = Logger.getLogger(SecureRedirectUriEnforceExecutor.class);

    private final KeycloakSession session;
    private final ComponentModel componentModel;

    public SecureRedirectUriEnforceExecutor(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
    }

    @Override
    public String getName() {
        return componentModel.getName();
    }

    @Override
    public String getProviderId() {
        return componentModel.getProviderId();
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case REGISTER:
                if (context instanceof AdminClientRegisterContext || context instanceof DynamicClientRegisterContext) {
                    confirmSecureRedirectUris(((ClientUpdateContext)context).getProposedClientRepresentation().getRedirectUris());
                } else {
                    throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not allowed input format.");
                }
                return;
            case UPDATE:
                if (context instanceof AdminClientUpdateContext || context instanceof DynamicClientUpdateContext) {
                    confirmSecureRedirectUris(((ClientUpdateContext)context).getProposedClientRepresentation().getRedirectUris());
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
            ClientPolicyLogger.log(logger, "Redirect URI = " + redirectUri);
            if (redirectUri.startsWith("http://") || redirectUri.contains("*")) {
                throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT_METADATA, "Invalid client metadata: redirect_uris");
            }
        }
    }
}
