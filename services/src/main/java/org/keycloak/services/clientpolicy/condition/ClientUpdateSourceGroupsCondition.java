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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.clientpolicy.AdminClientRegisterContext;
import org.keycloak.services.clientpolicy.AdminClientUpdateContext;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyLogger;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.ClientUpdateContext;
import org.keycloak.services.clientpolicy.DynamicClientRegisterContext;
import org.keycloak.services.clientpolicy.DynamicClientUpdateContext;

public class ClientUpdateSourceGroupsCondition implements ClientPolicyConditionProvider {

    private static final Logger logger = Logger.getLogger(ClientUpdateSourceGroupsCondition.class);

    private final KeycloakSession session;
    private final ComponentModel componentModel;

    public ClientUpdateSourceGroupsCondition(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
    }

    @Override
    public String getName() {
        return componentModel.getName();
    }

    @Override
    public String getProviderId() {
        return componentModel.getProviderId();
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
        case REGISTER:
            if (context instanceof AdminClientRegisterContext) {
                return getVoteForGroupsMatched(((ClientUpdateContext)context).getAuthenticatedUser());
            } else if (context instanceof DynamicClientRegisterContext) {
                return getVoteForGroupsMatched(((ClientUpdateContext)context).getToken());
            } else {
                throw new ClientPolicyException(OAuthErrorException.SERVER_ERROR, "unexpected context type.");
            }
        case UPDATE:
            if (context instanceof AdminClientUpdateContext) {
                return getVoteForGroupsMatched(((ClientUpdateContext)context).getAuthenticatedUser());
            } else if (context instanceof DynamicClientUpdateContext) {
                return getVoteForGroupsMatched(((ClientUpdateContext)context).getToken());
            } else {
                throw new ClientPolicyException(OAuthErrorException.SERVER_ERROR, "unexpected context type.");
            }
        default:
            return ClientPolicyVote.ABSTAIN;
        }
    }

    private ClientPolicyVote getVoteForGroupsMatched(UserModel user) {
        if (isGroupsMatched(user)) return ClientPolicyVote.YES;
        return ClientPolicyVote.NO;
    }

    private ClientPolicyVote getVoteForGroupsMatched(JsonWebToken token) {
        if (token == null) return ClientPolicyVote.NO;
        if(isGroupMatched(token.getSubject())) return ClientPolicyVote.YES;
        return ClientPolicyVote.NO;
    }

    private boolean isGroupMatched(String subjectId) {
        if (subjectId == null) return false;
        return isGroupsMatched(session.users().getUserById(session.getContext().getRealm(), subjectId));
    }

    private boolean isGroupsMatched(UserModel user) {
        if (user == null) return false;

        Set<String> expectedGroups = instantiateGroupsForMatching();
        if (expectedGroups == null) return false;

        // user.getGroupsStream() never returns null according to {@link UserModel.getGroupsStream}
        Set<String> groups = user.getGroupsStream().map(GroupModel::getName).collect(Collectors.toSet());

        if (logger.isTraceEnabled()) {
            groups.stream().forEach(i -> ClientPolicyLogger.log(logger, " user group = " + i));
            expectedGroups.stream().forEach(i -> ClientPolicyLogger.log(logger, "groups expected = " + i));
        }

        boolean isMatched = expectedGroups.removeAll(groups);
        if (isMatched) {
            ClientPolicyLogger.log(logger, "group matched.");
        } else {
            ClientPolicyLogger.log(logger, "group unmatched.");
        }
        return isMatched;
    }

    private Set<String> instantiateGroupsForMatching() {
        if (componentModel.getConfig() == null) return null;
        List<String> roles = componentModel.getConfig().get(ClientUpdateSourceGroupsConditionFactory.GROUPS);
        if (roles == null) return null;
        return new HashSet<>(roles);
    }
}
