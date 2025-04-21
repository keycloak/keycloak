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
package org.keycloak.services.clientpolicy.condition;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;

/**
 *
 * @author rmartinc
 */
public class ClientProtocolCondition extends AbstractClientPolicyConditionProvider<ClientProtocolCondition.Configuration> {

    public static class Configuration extends ClientPolicyConditionConfigurationRepresentation {

        protected String protocol;

        public Configuration() {
            protocol = null;
        }

        public Configuration(String protocol) {
            this.protocol = protocol;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }
    }

    public ClientProtocolCondition(KeycloakSession session) {
        super(session);
    }

    @Override
    public String getProviderId() {
        return ClientProtocolConditionFactory.PROVIDER_ID;
    }

    @Override
    public Class<Configuration> getConditionConfigurationClass() {
        return Configuration.class;
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case AUTHORIZATION_REQUEST:
            case TOKEN_REQUEST:
            case TOKEN_RESPONSE:
            case SERVICE_ACCOUNT_TOKEN_REQUEST:
            case SERVICE_ACCOUNT_TOKEN_RESPONSE:
            case TOKEN_REFRESH:
            case TOKEN_REFRESH_RESPONSE:
            case TOKEN_REVOKE:
            case TOKEN_INTROSPECT:
            case USERINFO_REQUEST:
            case LOGOUT_REQUEST:
            case UPDATE:
            case UPDATED:
            case REGISTERED:
            case SAML_AUTHN_REQUEST:
            case SAML_LOGOUT_REQUEST:
                if (isCorrectProtocolFromContext()) {
                    return ClientPolicyVote.YES;
                }
                return ClientPolicyVote.NO;
            case REGISTER:
                if (isCorrectProtocolFromRepresentation((ClientCRUDContext)context)) {
                    return ClientPolicyVote.YES;
                }
                return ClientPolicyVote.NO;
            default:
                return ClientPolicyVote.ABSTAIN;
        }
    }

    public boolean isCorrectProtocolFromContext() {
        ClientModel client = session.getContext().getClient();
        if (client != null) {
            String protocol = client.getProtocol();
            if (protocol != null) {
                return protocol.equals(configuration.getProtocol());
            }
        }
        return false;
    }

    public boolean isCorrectProtocolFromRepresentation(ClientCRUDContext context) {
        ClientRepresentation clientRep = context.getProposedClientRepresentation();
        if (clientRep != null) {
            String protocol = clientRep.getProtocol();
            if (protocol != null) {
                return protocol.equals(configuration.getProtocol());
            }
        }
        return false;
    }
}
