/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import java.net.URI;

import org.keycloak.OAuthErrorException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.saml.SamlClient;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientRegisteredContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdatedContext;
import org.keycloak.services.clientpolicy.context.SamlAuthnRequestContext;
import org.keycloak.services.clientpolicy.context.SamlLogoutRequestContext;

/**
 *
 * @author rmartinc
 */
public class SamlAvoidRedirectBindingExecutor implements ClientPolicyExecutorProvider<ClientPolicyExecutorConfigurationRepresentation> {

    public SamlAvoidRedirectBindingExecutor(KeycloakSession session) {
    }

    @Override
    public String getProviderId() {
        return SamlAvoidRedirectBindingExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case REGISTERED -> {
                confirmPostBindingIsForced(((AdminClientRegisteredContext)context).getTargetClient());
            }
            case UPDATED -> {
                confirmPostBindingIsForced(((AdminClientUpdatedContext)context).getTargetClient());
            }
            case SAML_AUTHN_REQUEST -> {
                confirmRedirectBindingIsNotUsed((SamlAuthnRequestContext) context);
            }
            case SAML_LOGOUT_REQUEST -> {
                confirmRedirectBindingIsNotUsed((SamlLogoutRequestContext) context);
            }
        }
    }

    private void confirmPostBindingIsForced(ClientModel client) throws ClientPolicyException {
        if (SamlProtocol.LOGIN_PROTOCOL.equals(client.getProtocol())) {
            SamlClient samlClient = new SamlClient(client);
            if (!samlClient.forcePostBinding()) {
                throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT_METADATA, "Force POST binding is not enabled");
            }
        }
    }

    private void confirmRedirectBindingIsNotUsed(SamlAuthnRequestContext context) throws ClientPolicyException {
        SamlClient samlClient = new SamlClient(context.getClient());
        if (samlClient.forcePostBinding()) {
            return;
        }
        URI requestedBinding = context.getRequest().getProtocolBinding();
        if (requestedBinding == null) {
            // no request binding explicitly requested so using the one used by the request
            if (context.getProtocolBinding().equals(SamlProtocol.SAML_REDIRECT_BINDING)) {
                throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "REDIRECT binding is used for the login request and it is not allowed.");
            }
        } else {
            // explicit request binding request check it's not redirect or artifact+redirect
            if (JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get().equals(requestedBinding.toString())
                    || (JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.get().equals(requestedBinding.toString())
                            && context.getProtocolBinding().equals(SamlProtocol.SAML_REDIRECT_BINDING))) {
                throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "REDIRECT binding is used for the login request and it is not allowed.");
            }
        }
    }

    private void confirmRedirectBindingIsNotUsed(SamlLogoutRequestContext context) throws ClientPolicyException {
        SamlClient samlClient = new SamlClient(context.getClient());
        if (samlClient.forcePostBinding()) {
            return;
        }
        if (context.getProtocolBinding().equals(SamlProtocol.SAML_REDIRECT_BINDING)) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "REDIRECT binding is used for the logout request and it is not allowed.");
        }
    }
}
