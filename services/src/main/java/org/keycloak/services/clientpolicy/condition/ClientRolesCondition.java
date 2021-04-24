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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyVote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientRolesCondition implements ClientPolicyConditionProvider<ClientRolesCondition.Configuration> {

    private static final Logger logger = Logger.getLogger(ClientRolesCondition.class);

    // to avoid null configuration, use vacant new instance to indicate that there is no configuration set up.
    private Configuration configuration = new Configuration();
    private final KeycloakSession session;

    public ClientRolesCondition(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(Configuration config) {
        this.configuration = config;
    }

    @Override
    public Class<Configuration> getConditionConfigurationClass() {
        return Configuration.class;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Configuration extends ClientPolicyConditionConfiguration {
        @JsonProperty("is-negative-logic")
        protected Boolean negativeLogic;

        public Boolean isNegativeLogic() {
            return negativeLogic;
        }

        public void setNegativeLogic(Boolean negativeLogic) {
            this.negativeLogic = negativeLogic;
        }

        protected List<String> roles;

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }

    @Override
    public boolean isNegativeLogic() {
        return Optional.ofNullable(this.configuration.isNegativeLogic()).orElse(Boolean.FALSE).booleanValue();
    }

    @Override
    public String getProviderId() {
        return ClientRolesConditionFactory.PROVIDER_ID;
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case AUTHORIZATION_REQUEST:
            case TOKEN_REQUEST:
            case TOKEN_REFRESH:
            case TOKEN_REVOKE:
            case TOKEN_INTROSPECT:
            case USERINFO_REQUEST:
            case LOGOUT_REQUEST:
                if (isRolesMatched(session.getContext().getClient())) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            default:
                return ClientPolicyVote.ABSTAIN;
        }
    }

    private boolean isRolesMatched(ClientModel client) {
        if (client == null) return false;

        Set<String> rolesForMatching = getRolesForMatching();
        if (rolesForMatching == null) return false;

        // client.getRolesStream() never returns null according to {@link RoleProvider.getClientRolesStream}
        Set<String> clientRoles = client.getRolesStream().map(RoleModel::getName).collect(Collectors.toSet());

        if (logger.isTraceEnabled()) {
            clientRoles.forEach(i -> logger.tracev("client role assigned = {0}", i));
            rolesForMatching.forEach(i -> logger.tracev("client role for matching = {0}", i));
        }

        return rolesForMatching.removeAll(clientRoles);  // may change rolesForMatching so that it has needed to be instantiated.
    }

    private Set<String> getRolesForMatching() {
        if (configuration.getRoles() == null) return null;
        return new HashSet<>(configuration.getRoles());
    }

}
