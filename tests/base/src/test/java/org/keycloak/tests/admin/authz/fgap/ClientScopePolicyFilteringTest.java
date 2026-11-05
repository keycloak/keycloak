/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.admin.authz.fgap;

import java.util.List;

import jakarta.ws.rs.ForbiddenException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ClientScopePolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientScopeBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakUrls;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest
public class ClientScopePolicyFilteringTest {

    @InjectRealm(config = ClientScopeRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void testRemovingRequiredClientScopeDeniesPartialEvaluation() {
        RealmResource realm = managedRealm.admin();

        var marta = realm.users().search("marta").get(0);
        var management = realm.clients().get(realm.clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId());
        realm.users().get(marta.getId()).roles().clientLevel(management.toRepresentation().getId())
                .add(List.of(management.roles().get(AdminRoles.QUERY_USERS).toRepresentation()));

        var adminPermissions = realm.clients().findByClientId(Constants.ADMIN_PERMISSIONS_CLIENT_ID).get(0);
        AuthorizationResource authorization = realm.clients().get(adminPermissions.getId()).authorization();
        UserPolicyRepresentation userPolicy = new UserPolicyRepresentation();
        userPolicy.setName("Allow Marta");
        userPolicy.addUser(marta.getId());
        authorization.policies().user().create(userPolicy).close();

        ClientScopePolicyRepresentation scopePolicy = new ClientScopePolicyRepresentation();
        scopePolicy.setName("Required bar");
        scopePolicy.addClientScope("bar", true);
        authorization.policies().clientScope().create(scopePolicy).close();

        AggregatePolicyRepresentation inner = new AggregatePolicyRepresentation();
        inner.setName("Mixed aggregate");
        inner.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        inner.addPolicy(userPolicy.getName(), scopePolicy.getName());
        authorization.policies().aggregate().create(inner).close();

        AggregatePolicyRepresentation outer = new AggregatePolicyRepresentation();
        outer.setName("Outer mixed aggregate");
        outer.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        outer.addPolicy(userPolicy.getName(), inner.getName());
        authorization.policies().aggregate().create(outer).close();

        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
        permission.setName("View users");
        permission.setResourceType(AdminPermissionsSchema.USERS_RESOURCE_TYPE);
        permission.addScope(AdminPermissionsSchema.VIEW);
        permission.addPolicy(outer.getName());
        authorization.permissions().scope().create(permission).close();

        try (Keycloak martaClient = KeycloakBuilder.builder()
                .serverUrl(keycloakUrls.getBase())
                .realm(managedRealm.getName()).clientId("resource-server-test").clientSecret("secret")
                .username("marta").password("password").scope("bar").build()) {
            var users = martaClient.realm(managedRealm.getName()).users();
            assertEquals(1, users.search("marta").size());
            assertEquals(marta.getId(), users.get(marta.getId()).toRepresentation().getId());

            ClientScopeRepresentation requiredScope = realm.clientScopes().findAll().stream()
                    .filter(scope -> scope.getName().equals("bar")).findFirst().orElseThrow();
            realm.clientScopes().get(requiredScope.getId()).remove();

            assertThrows(ForbiddenException.class, () -> users.get(marta.getId()).toRepresentation());
            assertEquals(0, users.search("marta").size(),
                    "Partial evaluation must deny the same invalid aggregate as full evaluation");

            outer = authorization.policies().aggregate().findByName(outer.getName());
            for (Logic logic : Logic.values()) {
                for (DecisionStrategy strategy : DecisionStrategy.values()) {
                    outer.setLogic(logic);
                    outer.setDecisionStrategy(strategy);
                    authorization.policies().aggregate().findById(outer.getId()).update(outer);
                    assertThrows(ForbiddenException.class, () -> users.get(marta.getId()).toRepresentation());
                    assertEquals(0, users.search("marta").size(),
                            "Invalid nested aggregates must deny with " + logic + " logic and " + strategy + " strategy");
                }
            }

            // Also check a permission that references the client-scope policy directly.
            permission = authorization.permissions().scope().findByName(permission.getName());
            permission.addScope(AdminPermissionsSchema.VIEW);
            permission.addPolicy(userPolicy.getName(), scopePolicy.getName());
            authorization.permissions().scope().findById(permission.getId()).update(permission);
            assertThrows(ForbiddenException.class, () -> users.get(marta.getId()).toRepresentation());
            assertEquals(0, users.search("marta").size());
        }
    }

    public static class ClientScopeRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.adminPermissionsEnabled(true)
                    .users(UserBuilder.create("marta").name("Marta", "Test").email("marta@example.org").password("password"))
                    .clientScopes(ClientScopeBuilder.create().name("bar").protocol("openid-connect"))
                    .clients(ClientBuilder.create().clientId("resource-server-test").secret("secret")
                            .optionalClientScopes("bar").directAccessGrantsEnabled());
        }
    }
}
