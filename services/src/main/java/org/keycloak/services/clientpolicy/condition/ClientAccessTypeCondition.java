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

package org.keycloak.services.clientpolicy.condition;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientAccessTypeCondition extends AbstractClientPolicyConditionProvider<ClientAccessTypeCondition.Configuration> {

    private static final Logger logger = Logger.getLogger(ClientAccessTypeCondition.class);

    public ClientAccessTypeCondition(KeycloakSession session) {
        super(session);
    }

    @Override
    public Class<Configuration> getConditionConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyConditionConfigurationRepresentation {

        protected List<String> type;

        public List<String> getType() {
            return type;
        }

        public void setType(List<String> type) {
            this.type = type;
        }
    }

    @Override
    public String getProviderId() {
        return ClientAccessTypeConditionFactory.PROVIDER_ID;
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
                if (isClientAccessTypeMatched()) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            case REGISTER:
                if (isProposedClientAccessTypeMatched((ClientCRUDContext)context)) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            default:
                return ClientPolicyVote.ABSTAIN;
        }
    }

    private String getClientAccessType() {
        ClientModel client = session.getContext().getClient();
        if (client == null) return null;
        return getClientAccessType(client.isPublicClient(), client.isBearerOnly());
    }

    private String getProposedClientAccessType(ClientCRUDContext context) {
        ClientRepresentation clientRep = context.getProposedClientRepresentation();
        if (clientRep == null) return null;
        return getClientAccessType(Optional.ofNullable(clientRep.isPublicClient()).orElse(Boolean.FALSE).booleanValue(),
                Optional.ofNullable(clientRep.isBearerOnly()).orElse(Boolean.FALSE).booleanValue());
    }

    private String getClientAccessType(boolean isPublicClient, boolean isBearerOnly) {
        if (isPublicClient) return ClientAccessTypeConditionFactory.TYPE_PUBLIC;
        if (isBearerOnly) return ClientAccessTypeConditionFactory.TYPE_BEARERONLY;
        else return ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL;
    }

    private boolean isClientAccessTypeMatched() {
        return isClientAccessTypeMatched(getClientAccessType());
    }

    private boolean isProposedClientAccessTypeMatched(ClientCRUDContext context) {
        return isClientAccessTypeMatched(getProposedClientAccessType(context));
    }

    private boolean isClientAccessTypeMatched(String accessType) {
        if (accessType == null) return false;

        List<String> expectedAccessTypes = Optional.ofNullable(configuration.getType()).orElse(Collections.emptyList());

        if (logger.isTraceEnabled()) {
            logger.tracev("accessType = {0}", accessType);
            expectedAccessTypes.stream().forEach(i -> logger.tracev("expected accessType = {0}", i));
        }

        return expectedAccessTypes.stream().anyMatch(i -> i.equals(accessType));
    }

}
