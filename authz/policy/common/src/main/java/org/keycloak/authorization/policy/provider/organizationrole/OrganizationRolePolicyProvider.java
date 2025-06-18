/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authorization.policy.provider.organizationrole;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.attribute.Attributes.Entry;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.identity.UserModelIdentity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.fgap.evaluation.partial.PartialEvaluationPolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationRoleModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.authorization.ResourceType;
import org.keycloak.representations.idm.authorization.OrganizationRolePolicyRepresentation;

/**
 * Policy provider for organization role-based authorization.
 * 
 * This provider evaluates authorization policies based on organization roles assigned to users.
 * It extends the standard role-based policy evaluation to include organization-scoped roles.
 * Organization roles provide fine-grained access control within organization boundaries.
 */
public class OrganizationRolePolicyProvider implements PolicyProvider, PartialEvaluationPolicyProvider {

    private final BiFunction<Policy, AuthorizationProvider, OrganizationRolePolicyRepresentation> representationFunction;

    private static final Logger logger = Logger.getLogger(OrganizationRolePolicyProvider.class);

    public OrganizationRolePolicyProvider(BiFunction<Policy, AuthorizationProvider, OrganizationRolePolicyRepresentation> representationFunction) {
        this.representationFunction = representationFunction;
    }

    @Override
    public void evaluate(Evaluation evaluation) {
        Policy policy = evaluation.getPolicy();
        OrganizationRolePolicyRepresentation policyRep = representationFunction.apply(policy, evaluation.getAuthorizationProvider());
        AuthorizationProvider authorizationProvider = evaluation.getAuthorizationProvider();
        RealmModel realm = authorizationProvider.getKeycloakSession().getContext().getRealm();
        Identity identity = evaluation.getContext().getIdentity();

        if (isGranted(realm, authorizationProvider, policyRep, identity)) {
            evaluation.grant();
        }

        if (logger.isDebugEnabled()) {
            logger.debugf("Organization role policy %s evaluated with status %s for identity %s", 
                    policy.getName(), evaluation.getEffect(), identity.getId());
        }
    }

    private boolean isGranted(RealmModel realm, AuthorizationProvider authorizationProvider, 
            OrganizationRolePolicyRepresentation policyRep, Identity identity) {
        
        Set<OrganizationRolePolicyRepresentation.OrganizationRoleDefinition> orgRoles = policyRep.getOrganizationRoles();
        boolean granted = false;
        
        UserModel user = getSubject(identity, realm, authorizationProvider);
        if (user == null) {
            return false;
        }

        OrganizationProvider orgProvider = authorizationProvider.getKeycloakSession().getProvider(OrganizationProvider.class);
        
        for (OrganizationRolePolicyRepresentation.OrganizationRoleDefinition roleDefinition : orgRoles) {
            String organizationId = roleDefinition.getOrganizationId();
            String roleId = roleDefinition.getRoleId();
            
            OrganizationModel organization = orgProvider.getById(organizationId);
            if (organization == null) {
                continue;
            }
            
            OrganizationRoleModel role = organization.getRoleById(roleId);
            if (role == null) {
                continue;
            }
            
            boolean hasRole = organization.hasRole(user, role);
            
            if (!hasRole && roleDefinition.isRequired() != null && roleDefinition.isRequired()) {
                return false;
            } else if (hasRole) {
                granted = true;
            }
        }

        return granted;
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
        // No resources to close
    }

    @Override
    public Stream<Policy> getPermissions(KeycloakSession session, ResourceType resourceType, UserModel subject) {
        AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);
        RealmModel realm = session.getContext().getRealm();
        StoreFactory storeFactory = provider.getStoreFactory();
        PolicyStore policyStore = storeFactory.getPolicyStore();
        
        // Get all organizations the user is a member of and their roles
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
        Stream<String> organizationRoleIds = orgProvider.getAllStream()
                .filter(org -> orgProvider.isMember(org, subject))
                .flatMap(org -> org.getUserRolesStream(subject))
                .map(OrganizationRoleModel::getId);
        
        List<String> roleIds = organizationRoleIds.toList();
        
        // Find policies that depend on these organization roles
        return policyStore.findDependentPolicies(
                storeFactory.getResourceServerStore().findByClient(realm.getAdminPermissionsClient()),
                resourceType.getType(), 
                OrganizationRolePolicyProviderFactory.ID, 
                "organizationRoles", 
                roleIds);
    }

    @Override
    public boolean evaluate(KeycloakSession session, Policy policy, UserModel adminUser) {
        RealmModel realm = session.getContext().getRealm();
        AuthorizationProvider authorizationProvider = session.getProvider(AuthorizationProvider.class);
        return isGranted(realm, authorizationProvider, 
                representationFunction.apply(policy, authorizationProvider), 
                new UserModelIdentity(realm, adminUser));
    }

    @Override
    public boolean supports(Policy policy) {
        return OrganizationRolePolicyProviderFactory.ID.equals(policy.getType());
    }
}
