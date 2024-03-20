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

import org.keycloak.OAuthErrorException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.saml.SamlClient;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
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
public class SamlSignatureEnforcerExecutor implements ClientPolicyExecutorProvider<ClientPolicyExecutorConfigurationRepresentation> {

    public SamlSignatureEnforcerExecutor(KeycloakSession session) {
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case REGISTERED -> {
                confirmSignaturesAreForcedRegister(((AdminClientRegisteredContext)context).getTargetClient());
            }
            case UPDATED -> {
                confirmSignaturesAreForcedRegister(((AdminClientUpdatedContext)context).getTargetClient());
            }
            case SAML_AUTHN_REQUEST -> {
                confirmSignaturesAreForced(((SamlAuthnRequestContext) context).getClient(), OAuthErrorException.INVALID_REQUEST);
            }
            case SAML_LOGOUT_REQUEST -> {
                confirmSignaturesAreForced(((SamlLogoutRequestContext) context).getClient(), OAuthErrorException.INVALID_REQUEST);
            }
        }
    }

    @Override
    public String getProviderId() {
        return SamlSignatureEnforcerExecutorFactory.PROVIDER_ID;
    }

    private boolean signaturesAreForced(boolean clientSignature, boolean serverSignature, boolean assertionSignature) {
        // ensure client is signed and server or asertion is signed
        return clientSignature && (serverSignature || assertionSignature);
    }

    private void confirmSignaturesAreForcedRegister(ClientModel client) throws ClientPolicyException {
        if (SamlProtocol.LOGIN_PROTOCOL.equals(client.getProtocol())) {
            confirmSignaturesAreForced(client, OAuthErrorException.INVALID_CLIENT_METADATA);
        }
    }

    private void confirmSignaturesAreForced(ClientModel client, String error) throws ClientPolicyException {
        SamlClient samlClient = new SamlClient(client);
        if (!signaturesAreForced(samlClient.requiresClientSignature(), samlClient.requiresRealmSignature(),
                samlClient.requiresAssertionSignature())) {
            throw new ClientPolicyException(error,
                    "Signatures not ensured for the client. Ensure Client signature required and Sign documents or Sign assertions are ON");
        }
    }
}
