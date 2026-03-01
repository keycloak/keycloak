/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2025 Red Hat, Inc., and individual contributors
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

package org.keycloak.authorization.fgap.evaluation.partial;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;

public final class UserResourceTypePolicyEvaluator implements ResourceTypePolicyEvaluator {

    @Override
    public void evaluate(ResourcePermission permission, AuthorizationProvider authorization, Consumer<Policy> policyConsumer) {
        UserModel user = resolveUser(permission, authorization);

        if (user != null) {
            evaluateGroupMembershipPermissions(permission, user, authorization, policyConsumer);
        }
    }

    private UserModel resolveUser(ResourcePermission permission, AuthorizationProvider authorization) {
        RealmModel realm = authorization.getRealm();
        KeycloakSession session = authorization.getKeycloakSession();
        String resourceType = permission.getResourceType();

        if (resourceType == null) {
            return null;
        }

        Resource resource = permission.getResource();

        if (resource == null) {
            return null;
        }

        ResourceServer resourceServer = resource.getResourceServer();
        String userName = AdminPermissionsSchema.SCHEMA.getResourceName(session, resourceServer, resourceType, resource.getName());

        if (userName == null) {
            return null;
        }

        return session.users().getUserByUsername(realm, userName);
    }

    private void evaluateGroupMembershipPermissions(ResourcePermission permission, UserModel user, AuthorizationProvider authorization, Consumer<Policy> policyConsumer) {
        StoreFactory storeFactory = authorization.getStoreFactory();
        PolicyStore policyStore = storeFactory.getPolicyStore();
        ResourceStore resourceStore = storeFactory.getResourceStore();
        ResourceServer resourceServer = permission.getResourceServer();

        evaluateHierarchy(user, group -> {
            Resource groupResource = resourceStore.findByName(resourceServer, group.getId());

            if (groupResource != null) {
                policyStore.findByResource(resourceServer, groupResource, policyConsumer);
            }
        });

        Stream<GroupModel> groups = user.getGroupsStream();

        if (groups.findAny().isPresent()) {
            KeycloakSession session = authorization.getKeycloakSession();
            Resource resourceTypeResource = AdminPermissionsSchema.SCHEMA.getResourceTypeResource(session, resourceServer, GROUPS_RESOURCE_TYPE);
            policyStore.findByResource(resourceServer, resourceTypeResource, policyConsumer);
        }
    }

    private void evaluateHierarchy(UserModel user, Consumer<GroupModel> eval) {
        user.getGroupsStream().forEach(group -> evaluateHierarchy(eval, group, new HashSet<>()));
    }

    private void evaluateHierarchy(Consumer<GroupModel> eval, GroupModel group, Set<GroupModel> visited) {
        if (visited.contains(group)) return;
        eval.accept(group);
        visited.add(group);
        if (group.getParent() == null) return;
        evaluateHierarchy(eval, group.getParent(), visited);
    }
}
