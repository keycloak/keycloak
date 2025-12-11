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

import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;
import org.keycloak.services.clientregistration.ClientRegistrationTokenUtils;
import org.keycloak.util.TokenUtil;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientUpdaterContextCondition extends AbstractClientPolicyConditionProvider<ClientUpdaterContextCondition.Configuration> {

    private static final Logger logger = Logger.getLogger(ClientUpdaterContextCondition.class);

    public ClientUpdaterContextCondition(KeycloakSession session) {
        super(session);
    }

    @Override
    public Class<Configuration> getConditionConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyConditionConfigurationRepresentation {

        @JsonProperty("update-client-source")
        protected List<String> updateClientSource;

        public List<String> getUpdateClientSource() {
            return updateClientSource;
        }

        public void setUpdateClientSource(List<String> updateClientSource) {
            this.updateClientSource = updateClientSource;
        }
    }

    @Override
    public String getProviderId() {
        return ClientUpdaterContextConditionFactory.PROVIDER_ID;
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
        case REGISTER:
        case UPDATE:
        case REGISTERED:
        case UPDATED:
            if (isAuthMethodMatched((ClientCRUDContext)context)) return ClientPolicyVote.YES;
            return ClientPolicyVote.NO;
        default:
            return ClientPolicyVote.ABSTAIN;
        }
    }

    private boolean isAuthMethodMatched(String authMethod) {
        if (authMethod == null) return false;

        List<String> expectedAuthMethods = configuration.getUpdateClientSource();
        if (expectedAuthMethods == null) expectedAuthMethods = Collections.emptyList();

        if (logger.isTraceEnabled()) {
            logger.tracev("auth method = {0}", authMethod);
            expectedAuthMethods.stream().forEach(i -> logger.tracev("auth method expected = {0}", i));
        }

        return expectedAuthMethods.stream().anyMatch(i -> i.equals(authMethod));
    }

    private boolean isAuthMethodMatched(ClientCRUDContext context) {
        String authMethod = null;

        if (context.getToken() == null) {
            authMethod = ClientUpdaterContextConditionFactory.BY_ANONYMOUS;
        } else if (isInitialAccessToken(context.getToken())) {
            authMethod = ClientUpdaterContextConditionFactory.BY_INITIAL_ACCESS_TOKEN;
        } else if (isRegistrationAccessToken(context.getToken())) {
            authMethod = ClientUpdaterContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN;
        } else if (isBearerToken(context.getToken())) {
            if (context.getAuthenticatedUser() != null || context.getAuthenticatedClient() != null) {
                authMethod = ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER;
            } else {
                authMethod = ClientUpdaterContextConditionFactory.BY_ANONYMOUS;
            }
        }

        return isAuthMethodMatched(authMethod);
    }
 
    private boolean isInitialAccessToken(JsonWebToken jwt) {
        return jwt != null && ClientRegistrationTokenUtils.TYPE_INITIAL_ACCESS_TOKEN.equals(jwt.getType());
    }

    private boolean isRegistrationAccessToken(JsonWebToken jwt) {
        return jwt != null && ClientRegistrationTokenUtils.TYPE_REGISTRATION_ACCESS_TOKEN.equals(jwt.getType());
    }

    private boolean isBearerToken(JsonWebToken jwt) {
        return jwt != null && TokenUtil.TOKEN_TYPE_BEARER.equals(jwt.getType());
    }

}
