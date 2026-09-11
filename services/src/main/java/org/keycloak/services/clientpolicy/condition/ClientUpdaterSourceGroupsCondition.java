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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.OAuthErrorException;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.context.AdminClientRegisterContext;
import org.keycloak.services.clientpolicy.context.AdminClientRegisteredContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdateContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdatedContext;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;
import org.keycloak.services.clientpolicy.context.DynamicClientRegisterContext;
import org.keycloak.services.clientpolicy.context.DynamicClientRegisteredContext;
import org.keycloak.services.clientpolicy.context.DynamicClientUpdateContext;
import org.keycloak.services.clientpolicy.context.DynamicClientUpdatedContext;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientUpdaterSourceGroupsCondition extends AbstractClientPolicyConditionProvider<ClientUpdaterSourceGroupsCondition.Configuration> {

    private static final Logger logger = Logger.getLogger(ClientUpdaterSourceGroupsCondition.class);

    public ClientUpdaterSourceGroupsCondition(KeycloakSession session) {
        super(session);
    }

    @Override
    public Class<Configuration> getConditionConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyConditionConfigurationRepresentation {

        protected List<String> groups;

        public List<String> getGroups() {
            return groups;
        }

        public void setGroups(List<String> groups) {
            this.groups = groups;
        }
    }

    @Override
    public String getProviderId() {
        return ClientUpdaterSourceGroupsConditionFactory.PROVIDER_ID;
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
        case REGISTER:
        case REGISTERED:
            if (context instanceof AdminClientRegisterContext || context instanceof AdminClientRegisteredContext) {
                return getVoteForGroupsMatched(((ClientCRUDContext)context).getAuthenticatedUser());
            } else if (context instanceof DynamicClientRegisterContext || context instanceof DynamicClientRegisteredContext) {
                return getVoteForGroupsMatched(((ClientCRUDContext)context).getToken());
            } else {
                throw new ClientPolicyException(OAuthErrorException.SERVER_ERROR, "unexpected context type.");
            }
        case UPDATE:
        case UPDATED:
            if (context instanceof AdminClientUpdateContext || context instanceof AdminClientUpdatedContext) {
                return getVoteForGroupsMatched(((ClientCRUDContext)context).getAuthenticatedUser());
            } else if (context instanceof DynamicClientUpdateContext || context instanceof DynamicClientUpdatedContext) {
                return getVoteForGroupsMatched(((ClientCRUDContext)context).getToken());
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

        List<String> configuredGroups = configuration.getGroups();
        if (configuredGroups == null) return false;

        Set<String> expectedGroups = configuredGroups.stream().map(this::resolveGroup).filter(Objects::nonNull)
                .map(GroupModel::getId).collect(Collectors.toSet());

        Set<String> groups = user.getGroupsStream().map(GroupModel::getId).collect(Collectors.toSet());

        if (logger.isTraceEnabled()) {
            groups.forEach(i -> logger.tracev("user group id = {0}", i));
            expectedGroups.forEach(i -> logger.tracev("expected user group id = {0}", i));
        }

        return groups.stream().anyMatch(expectedGroups::contains);
    }

    private GroupModel resolveGroup(String group) {
        if (group == null) return null;

        RealmModel realm = session.getContext().getRealm();

        // the configured groups are resolved regardless of the groups the user performing the request is allowed to view
        return AdminPermissionsSchema.runWithoutAuthorization(session, () -> {
            // a value with a leading slash is the full path of the group, otherwise it is the name of a top level group
            if (group.startsWith("/")) {
                return KeycloakModelUtils.findGroupByPath(session, realm, group);
            }
            return session.groups().getGroupByName(realm, null, group);
        });
    }

}
