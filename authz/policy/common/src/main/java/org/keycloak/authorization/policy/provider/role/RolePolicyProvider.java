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

import java.util.Set;
import java.util.function.BiFunction;

import org.jboss.logging.Logger;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.attribute.Attributes.Entry;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class RolePolicyProvider implements PolicyProvider {

    private final BiFunction<Policy, AuthorizationProvider, RolePolicyRepresentation> representationFunction;

    private static final Logger logger = Logger.getLogger(RolePolicyProvider.class);

    public RolePolicyProvider(BiFunction<Policy, AuthorizationProvider, RolePolicyRepresentation> representationFunction) {
        this.representationFunction = representationFunction;
    }

    @Override
    public void evaluate(Evaluation evaluation) {
        Policy policy = evaluation.getPolicy();
        RolePolicyRepresentation policyRep = representationFunction.apply(policy, evaluation.getAuthorizationProvider());
        Set<RolePolicyRepresentation.RoleDefinition> roleIds = policyRep.getRoles();
        AuthorizationProvider authorizationProvider = evaluation.getAuthorizationProvider();
        RealmModel realm = authorizationProvider.getKeycloakSession().getContext().getRealm();
        Identity identity = evaluation.getContext().getIdentity();

        for (RolePolicyRepresentation.RoleDefinition roleDefinition : roleIds) {
            RoleModel role = realm.getRoleById(roleDefinition.getId());

            if (role != null) {
                boolean hasRole = hasRole(identity, role, realm, authorizationProvider, policyRep.isFetchRoles());

                if (!hasRole && roleDefinition.isRequired()) {
                    evaluation.deny();
                    return;
                } else if (hasRole) {
                    evaluation.grant();
                }
            }
        }
        logger.debugv("policy {} evaluated with status {} on identity {}", policy.getName(), evaluation.getEffect(), identity.getId());
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
}
