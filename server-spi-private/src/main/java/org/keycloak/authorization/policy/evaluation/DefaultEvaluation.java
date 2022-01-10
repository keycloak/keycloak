/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.policy.evaluation;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision;
import org.keycloak.authorization.Decision.Effect;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.representations.idm.authorization.Logic;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DefaultEvaluation implements Evaluation {
    private final ResourcePermission permission;
    private final EvaluationContext executionContext;
    private final Decision decision;
    private Policy policy;
    private final Policy parentPolicy;
    private final AuthorizationProvider authorizationProvider;
    private final Map<Policy, Map<Object, Effect>> decisionCache;
    private final Realm realm;
    private Effect effect;

    public DefaultEvaluation(ResourcePermission permission, EvaluationContext executionContext, Policy parentPolicy, Decision decision, AuthorizationProvider authorizationProvider, Map<Policy, Map<Object, Decision.Effect>> decisionCache) {
        this(permission, executionContext, parentPolicy, null, decision, authorizationProvider, decisionCache);
    }

    public DefaultEvaluation(ResourcePermission permission, EvaluationContext executionContext, Decision decision, AuthorizationProvider authorizationProvider) {
        this(permission, executionContext, null, null, decision, authorizationProvider, Collections.emptyMap());
    }

    public DefaultEvaluation(ResourcePermission permission, EvaluationContext executionContext, Policy parentPolicy, Policy policy, Decision decision, AuthorizationProvider authorizationProvider, Map<Policy, Map<Object, Decision.Effect>> decisionCache) {
        this.permission = permission;
        this.executionContext = executionContext;
        this.parentPolicy = parentPolicy;
        this.policy = policy;
        this.decision = decision;
        this.authorizationProvider = authorizationProvider;
        this.decisionCache = decisionCache;
        this.realm = createRealm();
    }

    @Override
    public ResourcePermission getPermission() {
        return this.permission;
    }

    @Override
    public EvaluationContext getContext() {
        return this.executionContext;
    }

    @Override
    public void grant() {
        if (policy != null && Logic.NEGATIVE.equals(policy.getLogic())) {
            setEffect(Effect.DENY);
        } else {
            setEffect(Effect.PERMIT);
        }
    }

    @Override
    public void deny() {
        if (policy != null && Logic.NEGATIVE.equals(policy.getLogic())) {
            setEffect(Effect.PERMIT);
        } else {
            setEffect(Effect.DENY);
        }
    }

    @Override
    public Policy getPolicy() {
        if (policy == null) {
            return parentPolicy;
        }
        return this.policy;
    }

    @Override
    public Realm getRealm() {
        return realm;
    }

    @Override
    public AuthorizationProvider getAuthorizationProvider() {
        return authorizationProvider;
    }

    public Policy getParentPolicy() {
        return this.parentPolicy;
    }

    public Effect getEffect() {
        return effect;
    }

    public Map<Policy, Map<Object, Effect>> getDecisionCache() {
        return decisionCache;
    }

    @Override
    public void denyIfNoEffect() {
        if (this.effect == null) {
            deny();
        }
    }

    private Realm createRealm() {
        return new Realm() {

            @Override
            public boolean isUserInGroup(String id, String groupId, boolean checkParent) {
                KeycloakSession session = authorizationProvider.getKeycloakSession();
                UserModel user = getUser(id, session);

                if (Objects.isNull(user)) {
                    return false;
                }

                RealmModel realm = session.getContext().getRealm();
                GroupModel group = KeycloakModelUtils.findGroupByPath(realm, groupId);

                if (Objects.isNull(group)) {
                    return false;
                }

                if (checkParent) {
                    return RoleUtils.isMember(user.getGroupsStream(), group);
                }

                return user.isMemberOf(group);
            }

            private UserModel getUser(String id, KeycloakSession session) {
                RealmModel realm = session.getContext().getRealm();
                UserModel user = session.users().getUserById(realm, id);

                if (Objects.isNull(user)) {
                    user = session.users().getUserByUsername(realm ,id);
                }
                if (Objects.isNull(user)) {
                    user = session.users().getUserByEmail(realm, id);
                }
                if (Objects.isNull(user)) {
                    user = session.users().getServiceAccount(realm.getClientById(id));
                }

                return user;
            }

            @Override
            public boolean isUserInRealmRole(String id, String roleName) {
                KeycloakSession session = authorizationProvider.getKeycloakSession();
                UserModel user = getUser(id, session);

                if (Objects.isNull(user)) {
                    return false;
                }

                Stream<RoleModel> roleMappings = user.getRoleMappingsStream().filter(isNotClientRole);

                return RoleUtils.hasRole(roleMappings, session.getContext().getRealm().getRole(roleName));
            }

            @Override
            public boolean isUserInClientRole(String id, String clientId, String roleName) {
                KeycloakSession session = authorizationProvider.getKeycloakSession();
                RealmModel realm = session.getContext().getRealm();
                UserModel user = getUser(id, session);

                if (Objects.isNull(user)) {
                    return false;
                }

                Set<RoleModel> roleMappings = user.getRoleMappingsStream()
                        .filter(RoleModel::isClientRole)
                        .filter(role -> Objects.equals(((ClientModel) role.getContainer()).getClientId(), clientId))
                        .collect(Collectors.toSet());

                if (roleMappings.isEmpty()) {
                    return false;
                }

                RoleModel role = realm.getClientById(roleMappings.iterator().next().getContainer().getId()).getRole(roleName);

                if (Objects.isNull(role)) {
                    return false;
                }

                return RoleUtils.hasRole(roleMappings, role);
            }

            @Override
            public boolean isGroupInRole(String id, String role) {
                KeycloakSession session = authorizationProvider.getKeycloakSession();
                RealmModel realm = session.getContext().getRealm();
                GroupModel group = KeycloakModelUtils.findGroupByPath(realm, id);

                return RoleUtils.hasRoleFromGroup(group, realm.getRole(role), false);
            }

            @Override
            public List<String> getUserRealmRoles(String id) {
                return getUser(id, authorizationProvider.getKeycloakSession()).getRoleMappingsStream()
                        .filter(isNotClientRole)
                        .map(RoleModel::getName)
                        .collect(Collectors.toList());
            }

            @Override
            public List<String> getUserClientRoles(String id, String clientId) {
                return getUser(id, authorizationProvider.getKeycloakSession()).getRoleMappingsStream()
                        .filter(RoleModel::isClientRole)
                        .map(RoleModel::getName)
                        .collect(Collectors.toList());
            }

            @Override
            public List<String> getUserGroups(String id) {
                return getUser(id, authorizationProvider.getKeycloakSession()).getGroupsStream()
                        .map(ModelToRepresentation::buildGroupPath)
                        .collect(Collectors.toList());
            }

            @Override
            public Map<String, List<String>> getUserAttributes(String id) {
                return Collections.unmodifiableMap(getUser(id, authorizationProvider.getKeycloakSession()).getAttributes());
            }
        };
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
        this.effect = null;
    }

    public void setEffect(Effect effect) {
        this.effect = effect;
        this.decision.onDecision(this);
    }

    private Predicate<RoleModel> isNotClientRole = ((Predicate<RoleModel>) RoleModel::isClientRole).negate();
}
