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

package org.keycloak.testsuite.services.clientpolicy.condition;

import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyLogger;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider;

public class TestClientRolesCondition implements ClientPolicyConditionProvider {

    private static final Logger logger = Logger.getLogger(TestClientRolesCondition.class);

    private final KeycloakSession session;
    private final ComponentModel componentModel;

    public TestClientRolesCondition(KeycloakSession session, ComponentModel componentModel) {
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

        List<String> rolesForMatching = getRolesForMatching();
        if (rolesForMatching == null) return false;

        client.getRoles().stream().forEach(i -> ClientPolicyLogger.log(logger, "client role = " + i.getName()));
        rolesForMatching.stream().forEach(i -> ClientPolicyLogger.log(logger, "roles expected = " + i));

        boolean isMatched = rolesForMatching.stream().anyMatch(i->client.getRoles().stream().anyMatch(j->j.getName().equals(i)));
        if (isMatched) {
            ClientPolicyLogger.log(logger, "role matched.");
        } else {
            ClientPolicyLogger.log(logger, "role unmatched.");
        }
        return isMatched;
    }

    private List<String> getRolesForMatching() {
        return componentModel.getConfig().get(TestClientRolesConditionFactory.ROLES);
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
