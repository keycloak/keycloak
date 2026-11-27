/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.authorization.policy.provider.role;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision.Effect;
import org.keycloak.authorization.attribute.Attributes.Entry;
import org.keycloak.authorization.fgap.evaluation.partial.PartialEvaluationPolicyProvider;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.identity.UserModelIdentity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ResourceType;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation.RoleDefinition;

import org.jboss.logging.Logger;

import static org.keycloak.models.utils.RoleUtils.getDeepUserRoleMappings;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class RolePolicyProvider implements PolicyProvider, PartialEvaluationPolicyProvider {

    private final BiFunction<Policy, AuthorizationProvider, RolePolicyRepresentation> representationFunction;

    private static final Logger logger = Logger.getLogger(RolePolicyProvider.class);

    public RolePolicyProvider(BiFunction<Policy, AuthorizationProvider, RolePolicyRepresentation> representationFunction) {
        this.representationFunction = representationFunction;
    }

    @Override
    public boolean isValid(Policy policy, AuthorizationProvider authorization) {
        boolean negative = Logic.NEGATIVE.equals(policy.getLogic());
        Set<RoleDefinition> roles = representationFunction.apply(policy, authorization).getRoles();
        RealmModel realm = authorization.getRealm();
        boolean hasResolvedRole = false;
        for (RoleDefinition role : roles) {
            boolean mustResolve = negative || Boolean.TRUE.equals(role.isRequired());
            if (hasResolvedRole && !mustResolve) {
                continue;
            }
            if (realm.getRoleById(role.getId()) != null) {
                hasResolvedRole = true;
            } else if (mustResolve) {
                return false;
            }
        }
        return hasResolvedRole;
    }

    @Override
    public void evaluate(Evaluation evaluation) {
        Policy policy = evaluation.getPolicy();
        AuthorizationProvider authorizationProvider = evaluation.getAuthorizationProvider();
        RealmModel realm = authorizationProvider.getKeycloakSession().getContext().getRealm();
        Identity identity = evaluation.getContext().getIdentity();

        Outcome outcome = evaluateRoles(policy, realm, authorizationProvider, identity);
        if (outcome == Outcome.INVALID) {
            evaluation.setEffect(Effect.INVALID);
        } else if (outcome == Outcome.GRANT) {
            evaluation.grant();
        }

        logger.debugf("policy %s evaluated with status %s on identity %s", policy.getName(), evaluation.getEffect(), identity.getId());
    }

    private Outcome evaluateRoles(Policy policy, RealmModel realm, AuthorizationProvider authorizationProvider, Identity identity) {
        RolePolicyRepresentation policyRep = representationFunction.apply(policy, authorizationProvider);
        Set<RoleDefinition> roleIds = policyRep.getRoles();
        boolean negative = Logic.NEGATIVE.equals(policy.getLogic());
        boolean fetchRoles = Boolean.TRUE.equals(policyRep.isFetchRoles());
        boolean granted = false;
        boolean missingRequiredMembership = false;
        boolean hasResolvedRole = false;

        for (RoleDefinition roleDefinition : roleIds) {
            boolean required = Boolean.TRUE.equals(roleDefinition.isRequired());
            if (!negative && !required && (granted || missingRequiredMembership)) {
                continue;
            }
            RoleModel role = realm.getRoleById(roleDefinition.getId());

            if (role == null) {
                if (required || negative) {
                    return Outcome.INVALID;
                }
                continue;
            }
            hasResolvedRole = true;

            if (missingRequiredMembership || (granted && !required)) {
                continue;
            }
            if (hasRole(identity, role, realm, authorizationProvider, fetchRoles)) {
                granted = true;
            } else if (required) {
                missingRequiredMembership = true;
            }
        }

        if (!hasResolvedRole) {
            return Outcome.INVALID;
        }
        return granted && !missingRequiredMembership ? Outcome.GRANT : Outcome.DENY;
    }

    private boolean hasRole(Identity identity, RoleModel role, RealmModel realm, AuthorizationProvider authorizationProvider, boolean fetchRoles) {
        if (fetchRoles) {
            UserModel subject = getSubject(identity, realm, authorizationProvider);
            return subject != null && subject.hasRole(role);
        }
        String roleName = role.getName();
        if (role.isClientRole()) {
            ClientModel clientModel = realm.getClientById(role.getContainerId());
            return identity.hasClientRole(clientModel.getClientId(), roleName);
        }
        return identity.hasRealmRole(roleName);
    }

    private UserModel getSubject(Identity identity, RealmModel realm, AuthorizationProvider authorizationProvider) {
        KeycloakSession session = authorizationProvider.getKeycloakSession();
        UserProvider users = session.users();
        UserModel user = users.getUserById(realm, identity.getId());

        if (user == null) {
            Entry sub = identity.getAttributes().getValue(JsonWebToken.SUBJECT);

            if (sub == null || sub.isEmpty()) {
                return null;
            }

            return users.getUserById(realm, sub.asString(0));
        }

        return user;
    }

    @Override
    public void close() {

    }

    @Override
    public Stream<Policy> getPermissions(KeycloakSession session, ResourceType resourceType, ResourceType groupResourceType, UserModel subject) {
        AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);
        RealmModel realm = session.getContext().getRealm();
        ClientModel adminPermissionsClient = realm.getAdminPermissionsClient();
        StoreFactory storeFactory = provider.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findByClient(adminPermissionsClient);
        PolicyStore policyStore = storeFactory.getPolicyStore();
        List<String> roleIds = getDeepUserRoleMappings(subject).stream().map(RoleModel::getId).toList();
        Stream<Policy> policies = Stream.of();

        return Stream.concat(policies, policyStore.findDependentPolicies(resourceServer, resourceType.getType(), groupResourceType == null ? null : groupResourceType.getType(), RolePolicyProviderFactory.ID, "roles", roleIds));
    }

    @Override
    public boolean evaluate(KeycloakSession session, Policy policy, UserModel adminUser) {
        return evaluateOutcome(session, policy, adminUser) == Outcome.GRANT;
    }

    @Override
    public Outcome evaluateOutcome(KeycloakSession session, Policy policy, UserModel adminUser) {
        RealmModel realm = session.getContext().getRealm();
        AuthorizationProvider authorizationProvider = session.getProvider(AuthorizationProvider.class);
        return evaluateRoles(policy, realm, authorizationProvider, new UserModelIdentity(realm, adminUser));
    }

    @Override
    public boolean supports(Policy policy) {
        return RolePolicyProviderFactory.ID.equals(policy.getType());
    }
}
