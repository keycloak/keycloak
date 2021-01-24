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
import org.keycloak.OAuthErrorException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyLogger;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.context.AdminClientRegisterContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdateContext;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;
import org.keycloak.services.clientpolicy.context.DynamicClientRegisterContext;
import org.keycloak.services.clientpolicy.context.DynamicClientUpdateContext;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class ClientUpdateSourceRolesCondition extends AbstractClientCondition {

    private static final Logger logger = Logger.getLogger(ClientUpdateSourceRolesCondition.class);

    // to avoid null configuration, use vacant new instance to indicate that there is no configuration set up.
    private Configuration configuration = new Configuration();

    public ClientUpdateSourceRolesCondition(KeycloakSession session) {
        super(session);
    }

    @Override
    protected <T extends AbstractClientCondition.Configuration> T getConfiguration(Class<T> clazz) {
        return (T) configuration;
    }
 
    @Override
    public void setupConfiguration(Object config) {
        // to avoid null configuration, use vacant new instance to indicate that there is no configuration set up.
        configuration = Optional.ofNullable(getConvertedConfiguration(config, Configuration.class)).orElse(new Configuration());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Configuration extends AbstractClientCondition.Configuration {
        protected List<String> roles;

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }

    @Override
    public String getProviderId() {
        return ClientUpdateSourceRolesConditionFactory.PROVIDER_ID;
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
        case REGISTER:
            if (context instanceof AdminClientRegisterContext) {
                return getVoteForRolesMatched(((ClientCRUDContext)context).getAuthenticatedUser());
            } else if (context instanceof DynamicClientRegisterContext) {
                return getVoteForRolesMatched(((ClientCRUDContext)context).getToken());
            } else {
                throw new ClientPolicyException(OAuthErrorException.SERVER_ERROR, "unexpected context type.");
            }
        case UPDATE:
            if (context instanceof AdminClientUpdateContext) {
                return getVoteForRolesMatched(((ClientCRUDContext)context).getAuthenticatedUser());
            } else if (context instanceof DynamicClientUpdateContext) {
                return getVoteForRolesMatched(((ClientCRUDContext)context).getToken());
            } else {
                throw new ClientPolicyException(OAuthErrorException.SERVER_ERROR, "unexpected context type.");
            }
        default:
            return ClientPolicyVote.ABSTAIN;
        }
    }

    private ClientPolicyVote getVoteForRolesMatched(UserModel user) {
        if (isRolesMatched(user)) return ClientPolicyVote.YES;
        return ClientPolicyVote.NO;
    }

    private ClientPolicyVote getVoteForRolesMatched(JsonWebToken token) {
        if (token == null) return ClientPolicyVote.NO;
        if(isRoleMatched(token.getSubject())) return ClientPolicyVote.YES;
        return ClientPolicyVote.NO;
    }

    private boolean isRoleMatched(String subjectId) {
        if (subjectId == null) return false;
        return isRolesMatched(session.users().getUserById(session.getContext().getRealm(), subjectId));
    }

    private boolean isRolesMatched(UserModel user) {
        if (user == null) return false;

        Set<String> expectedRoles = instantiateRolesForMatching();
        if (expectedRoles == null) return false;

        // user.getRoleMappingsStream() never returns null according to {@link UserModel.getRoleMappingsStream}
        Set<String> roles = user.getRoleMappingsStream().map(RoleModel::getName).collect(Collectors.toSet());

        if (logger.isTraceEnabled()) {
            roles.forEach(i -> ClientPolicyLogger.logv(logger, "{0} :: user role = {1}", logMsgPrefix(), i));
            expectedRoles.forEach(i -> ClientPolicyLogger.logv(logger, "{0} :: roles expected = {1}", logMsgPrefix(), i));
        }

        RealmModel realm = session.getContext().getRealm();
        boolean isMatched = expectedRoles.stream().anyMatch(i->{
            if (realm.getRole(i) != null && user.hasRole(realm.getRole(i))) {
                return true;
            }
            return realm.getClientsStream().anyMatch(j->{
                if (j.getRole(i) != null && user.hasRole(j.getRole(i))) {
                    return true;
                }
                return false;
            });
        });

        return isMatched;
    }

    private Set<String> instantiateRolesForMatching() {
        List<String> roles = configuration.getRoles();
        if (roles == null) return null;
        return new HashSet<>(roles);
    }

}
