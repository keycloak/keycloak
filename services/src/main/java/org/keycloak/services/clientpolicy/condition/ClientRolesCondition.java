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

package org.keycloak.services.clientpolicy.condition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyLogger;
import org.keycloak.services.clientpolicy.ClientPolicyVote;

public class ClientRolesCondition implements ClientPolicyConditionProvider {
    private static final Logger logger = Logger.getLogger(ClientRolesCondition.class);

    private final KeycloakSession session;
    private final ComponentModel componentModel;

    public ClientRolesCondition(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
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
            clientRoles.stream().forEach(i -> ClientPolicyLogger.log(logger, "client role assigned = " + i));
            rolesForMatching.stream().forEach(i -> ClientPolicyLogger.log(logger, "client role for matching = " + i));
        }

        boolean isMatched = rolesForMatching.removeAll(clientRoles);
        if (isMatched) {
            ClientPolicyLogger.log(logger, "role matched.");
        } else {
            ClientPolicyLogger.log(logger, "role unmatched.");
        }

        return isMatched;
    }

    private Set<String> getRolesForMatching() {
        if (componentModel.getConfig() == null) return null;
        List<String> roles = componentModel.getConfig().get(ClientRolesConditionFactory.ROLES);
        if (roles == null) return null;
        return new HashSet<>(roles);
    }

    @Override
    public String getName() {
        return componentModel.getName();
    }

    @Override
    public String getProviderId() {
        return componentModel.getProviderId();
    }

}
