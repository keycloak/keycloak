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

import java.util.List;
import java.util.Map;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.MapperTypeSerializer;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;
import org.keycloak.services.clientpolicy.context.ClientModelContext;
import org.keycloak.services.clientpolicy.context.PreAuthorizationRequestContext;

import org.jboss.logging.Logger;

import static org.keycloak.services.clientpolicy.ClientPolicyEvent.PRE_AUTHORIZATION_REQUEST;
import static org.keycloak.services.clientpolicy.ClientPolicyEvent.REGISTER;

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

        private String attributes;

        public String getAttributes() {
            return attributes;
        }

        public void setAttributes(String attributes) {
            this.attributes = attributes;
        }
    }

    @Override
    public String getProviderId() {
        return ClientAttributesConditionFactory.PROVIDER_ID;
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {
        if (context.getEvent() == PRE_AUTHORIZATION_REQUEST) {
            PreAuthorizationRequestContext parc = (PreAuthorizationRequestContext) context;
            ClientModel client = session.getContext().getRealm().getClientByClientId(parc.getClientId());
            if (isAttributesMatched(client)) return ClientPolicyVote.YES;
            return ClientPolicyVote.NO;
        } else if (context.getEvent() == REGISTER && context instanceof ClientCRUDContext clientCRUDContext) {
            // The client does not exist yet, so the attributes of the proposed client are the only ones available
            if (isProposedAttributesMatched(clientCRUDContext)) return ClientPolicyVote.YES;
            return ClientPolicyVote.NO;
        } else if (context instanceof ClientModelContext clientModelContext) {
            ClientModel client = clientModelContext.getClient();
            if (isAttributesMatched(client)) return ClientPolicyVote.YES;
            // In case that there is an attempt to update the client to the matching attributes, condition should be also evaluated to success
            if (context instanceof ClientCRUDContext clientCRUDContext && isProposedAttributesMatched(clientCRUDContext)) return ClientPolicyVote.YES;
            return ClientPolicyVote.NO;
        } else {
            return ClientPolicyVote.ABSTAIN;
        }
    }

    private boolean isAttributesMatched(ClientModel client) {
        if (client == null) return false;
        return isAttributesMatched(client.getAttributes());
    }

    private boolean isProposedAttributesMatched(ClientCRUDContext context) {
        ClientRepresentation clientRep = context.getProposedClientRepresentation();
        if (clientRep == null) return false;
        return isAttributesMatched(clientRep.getAttributes());
    }

    private boolean isAttributesMatched(Map<String, String> clientAttributes) {
        if (clientAttributes == null) return false;

        Map<String, List<String>> attributesForMatching = getAttributesForMatching();
        if (attributesForMatching == null) return false;

        if (logger.isTraceEnabled()) {
            clientAttributes.forEach((i, j) -> logger.tracev("client attribute assigned = {0}: {1}", i, j));
            attributesForMatching.forEach((i, j) -> logger.tracev("client attribute for matching = {0}: {1}", i, j));
        }

        return attributesForMatching.entrySet().stream()
                .allMatch(entry -> {
                    String key = entry.getKey();
                    if (key == null) {
                        logger.warnf("Empty key in configuration of client-attributes condition");
                        return false;
                    }
                    if (entry.getValue() == null || entry.getValue().isEmpty()) {
                        logger.warnf("Empty value in the configuration of client-attributes condition for the attribute %s. This cannot match any client", key);
                        return false;
                    }
                    if (entry.getValue().size() > 1) {
                        logger.warnf("More values in the configuration of client-attributes condition for the attribute %s. This cannot match any client", key);
                        return false;
                    }
                    String value = entry.getValue().get(0);
                    return value.equals(clientAttributes.get(key));
                });
    }

    private Map<String, List<String>> getAttributesForMatching() {
        if (configuration.getAttributes() == null) return null;
        return MapperTypeSerializer.deserialize(configuration.getAttributes());
    }

}
