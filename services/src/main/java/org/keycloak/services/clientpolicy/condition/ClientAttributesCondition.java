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

import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.context.PreAuthorizationRequestContext;

import java.util.Map;

/**
 * @author <a href="mailto:yoshiyuki.tabata.jy@hitachi.com">Yoshiyuki Tabata</a>
 */
public class ClientAttributesCondition extends AbstractClientPolicyConditionProvider<ClientAttributesCondition.Configuration> {

    private static final Logger logger = Logger.getLogger(ClientAttributesCondition.class);

    public ClientAttributesCondition(KeycloakSession session) {
        super(session);
    }

    @Override
    public Class<Configuration> getConditionConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyConditionConfigurationRepresentation {

        protected Map<String, String> attributes;

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
        }
    }

    @Override
    public String getProviderId() {
        return ClientAttributesConditionFactory.PROVIDER_ID;
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case PRE_AUTHORIZATION_REQUEST:
                PreAuthorizationRequestContext paContext = (PreAuthorizationRequestContext) context;
                ClientModel client = session.getContext().getRealm().getClientByClientId(paContext.getClientId());
                if (isAttributesMatched(client)) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
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
            case BACKCHANNEL_AUTHENTICATION_REQUEST:
            case BACKCHANNEL_TOKEN_REQUEST:
            case BACKCHANNEL_TOKEN_RESPONSE:
            case PUSHED_AUTHORIZATION_REQUEST:
            case REGISTERED:
            case UPDATE:
            case UPDATED:
            case SAML_AUTHN_REQUEST:
            case SAML_LOGOUT_REQUEST:
                if (isAttributesMatched(session.getContext().getClient())) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            default:
                return ClientPolicyVote.ABSTAIN;
        }
    }

    private boolean isAttributesMatched(ClientModel client) {
        if (client == null) return false;

        Map<String, String> attributesForMatching = getAttributesForMatching();
        if (attributesForMatching == null) return false;

        Map<String, String> clientAttributes = client.getAttributes();

        if (logger.isTraceEnabled()) {
            clientAttributes.forEach((i, j) -> logger.tracev("client attribute assigned = {0}: {1}", i, j));
            attributesForMatching.forEach((i, j) -> logger.tracev("client attribute for matching = {0}: {1}", i, j));
        }

        return attributesForMatching.entrySet().stream()
                .allMatch(entry -> clientAttributes.containsKey(entry.getKey()) && clientAttributes.get(entry.getKey()).equals(entry.getValue()));
    }

    private Map<String, String> getAttributesForMatching() {
        if (configuration.getAttributes() == null) return null;
        return configuration.getAttributes();
    }

}
