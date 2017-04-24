/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.admin;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.DecisionEffect;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyEvaluationRequest;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Ignore
public class FineGrainAdminLocalTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setId(TEST);
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealms.add(testRealmRep);
    }

    public static void setupDefaults(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);

        ClientModel client = realm.getClientByClientId("realm-management");

        AuthorizationProvider authz = session.getProvider(AuthorizationProvider.class);
        ResourceServer resourceServer = authz.getStoreFactory().getResourceServerStore().create(client.getId());
        Scope mapRoleScope = authz.getStoreFactory().getScopeStore().create("map-role", resourceServer);
        Scope manageScope = authz.getStoreFactory().getScopeStore().create("manage", resourceServer);

        Policy manageUsersPolicy = null;
        Policy manageClientsPolicy = null;
        for (RoleModel role : client.getRoles()) {
            Policy policy = createRolePolicy(authz, resourceServer, role);
            if (role.getName().equals(AdminRoles.MANAGE_USERS)) {
                manageUsersPolicy = policy;
            } else if (role.getName().equals(AdminRoles.MANAGE_CLIENTS)) {
                manageClientsPolicy = policy;
            }
            Resource resource = createRoleResource(authz, resourceServer, role);
            Set<Scope> scopeset = new HashSet<>();
            scopeset.add(mapRoleScope);
            resource.updateScopes(scopeset);


            String name = "map.role.permission." + client.getClientId() + "." + role.getName();
            Policy permission = addScopePermission(authz, resourceServer, name, resource, mapRoleScope, policy);

        }
        Resource usersResource = authz.getStoreFactory().getResourceStore().create("Users", resourceServer, resourceServer.getClientId());
        Set<Scope> scopeset = new HashSet<>();
        scopeset.add(manageScope);
        usersResource.updateScopes(scopeset);
        addScopePermission(authz, resourceServer, "Users.manage.permission", usersResource, manageScope, manageUsersPolicy);
    }

    private static Policy addScopePermission(AuthorizationProvider authz, ResourceServer resourceServer, String name, Resource resource, Scope scope, Policy policy) {
        ScopePermissionRepresentation representation = new ScopePermissionRepresentation();

        representation.setName(name);
        representation.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
        representation.setLogic(Logic.POSITIVE);
        representation.addResource(resource.getName());
        representation.addScope(scope.getName());
        representation.addPolicy(policy.getName());

        return authz.getStoreFactory().getPolicyStore().create(representation, resourceServer);
    }

    private static Resource createRoleResource(AuthorizationProvider authz, ResourceServer resourceServer, RoleModel role) {
        String roleName = getRoleResourceName(role);
        Resource resource =  authz.getStoreFactory().getResourceStore().create(roleName, resourceServer, resourceServer.getClientId());
        resource.setType("Role");
        return resource;
    }

    private static String getRoleResourceName(RoleModel role) {
        String roleName = "realm";
        if (role.getContainer() instanceof ClientModel) {
            ClientModel client = (ClientModel)role.getContainer();
            roleName = client.getClientId();
        }
        roleName = "role.resource." + roleName + "." + role.getName();
        return roleName;
    }


    private static Policy createRolePolicy(AuthorizationProvider authz, ResourceServer resourceServer, RoleModel role) {
        String roleName = "realm";
        if (role.getContainer() instanceof ClientModel) {
            ClientModel client = (ClientModel) role.getContainer();
            roleName = client.getClientId() ;
        }
        roleName = "role.policy." + roleName + "." + role.getName();
        PolicyRepresentation representation = new PolicyRepresentation();

        representation.setName(roleName);
        representation.setType("role");
        representation.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
        representation.setLogic(Logic.POSITIVE);
        String roleValues = "[{\"id\":\"" + role.getId() + "\",\"required\": true}]";
        Map<String, String> config = new HashMap<>();
        config.put("roles", roleValues);
        representation.setConfig(config);

        return authz.getStoreFactory().getPolicyStore().create(representation, resourceServer);
    }

    public static void setupUsers(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        ClientModel client = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
        UserModel admin = session.users().addUser(realm, "admin");
        admin.grantRole(client.getRole(AdminRoles.REALM_ADMIN));
        UserModel manageUserOnlyUser = session.users().addUser(realm, "manage-user");
        RoleModel manageUsersRole = client.getRole(AdminRoles.MANAGE_USERS);
        manageUserOnlyUser.grantRole(manageUsersRole);
        UserModel manageRealmUser = session.users().addUser(realm, "manage-realm");
        manageRealmUser.grantRole(manageUsersRole);
        RoleModel manageRealmRole = client.getRole(AdminRoles.MANAGE_REALM);
        manageRealmUser.grantRole(manageRealmRole);

    }

    @Test
    public void testUI() throws Exception {
        testingClient.server().run(FineGrainAdminLocalTest::setupDefaults);
        testingClient.server().run(FineGrainAdminLocalTest::setupUsers);
        //Thread.sleep(1000000000);
    }

    public static void evaluateAdminHasManageRealmPermissions(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        UserModel admin = session.users().getUserByUsername("admin", realm);

        AuthorizationProvider authz = session.getProvider(AuthorizationProvider.class);
        ClientModel client = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
        ResourceServer resourceServer = authz.getStoreFactory().getResourceServerStore().findByClient(client.getId());

        RoleModel manageRealmRole = client.getRole(AdminRoles.MANAGE_REALM);
        Resource roleResource = authz.getStoreFactory().getResourceStore().findByName(getRoleResourceName(manageRealmRole), resourceServer.getId());



    }

    @Test
    public void testEvaluation() throws Exception {
        testingClient.server().run(FineGrainAdminLocalTest::setupDefaults);
        testingClient.server().run(FineGrainAdminLocalTest::setupUsers);

        RealmResource realm = adminClient.realm(TEST);
        String resourceServerId = realm.clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();
        UserRepresentation admin = realm.users().search("admin").get(0);
        UserRepresentation manageUser = realm.users().search("manage-user").get(0);
        UserRepresentation manageRealm = realm.users().search("manage-realm").get(0);

        PolicyEvaluationRequest request = new PolicyEvaluationRequest();
        request.setUserId(admin.getId());
        request.setClientId(resourceServerId);
        request.addResource("role.resource." + Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.MANAGE_REALM,
                "map-role");
        PolicyEvaluationResponse result = realm.clients().get(resourceServerId).authorization().policies().evaluate(request);
        Assert.assertEquals(result.getStatus(), DecisionEffect.PERMIT);
    }

}
