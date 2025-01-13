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

package org.keycloak.test.admin.authz.fgap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ScopePermissionsResource;
import org.keycloak.authorization.AdminPermissionsSchema;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.test.framework.annotations.InjectAdminClient;
import org.keycloak.test.framework.annotations.InjectUser;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.realm.ManagedUser;

@KeycloakIntegrationTest(config = KeycloakAdminPermissionsServerConfig.class)
public class UserResourceTypeEvaluationTest extends AbstractPermissionTest {

    @InjectUser(ref = "alice")
    ManagedUser userAlice;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @AfterEach
    public void onAfter() {
        ScopePermissionsResource permissions = getScopePermissionsResource();

        for (ScopePermissionRepresentation permission : permissions.findAll(null, null, null, -1, -1)) {
            permissions.findById(permission.getId()).remove();
        }
    }

    @Test
    public void testManageAllPermission() {
        UserPolicyRepresentation policy = new UserPolicyRepresentation();
        policy.setName("Only My Admin User Policy");
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        policy.addUser(myadmin.getId());
        client.admin().authorization().policies().user().create(policy).close();
        ScopePermissionRepresentation permission = createAllUserPermission(policy);
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertFalse(search.isEmpty());

        permission = client.admin().authorization().permissions().scope().findByName(permission.getName());
        permission.setPolicies(Set.of());
        client.admin().authorization().permissions().scope().findById(permission.getId()).update(permission);
        search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertTrue(search.isEmpty());
    }

    @Test
    public void testManageUserPermission() {
        UserPolicyRepresentation allowMyAdminPermission = new UserPolicyRepresentation();
        allowMyAdminPermission.setName("Only My Admin User Policy");
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        allowMyAdminPermission.addUser(myadmin.getId());
        client.admin().authorization().policies().user().create(allowMyAdminPermission).close();
        createAllUserPermission(allowMyAdminPermission);
        UserPolicyRepresentation denyMyAdminAccessingHisAccountPermission = new UserPolicyRepresentation();
        denyMyAdminAccessingHisAccountPermission.setName("Not My Admin User Policy");
        denyMyAdminAccessingHisAccountPermission.addUser(myadmin.getId());
        denyMyAdminAccessingHisAccountPermission.setLogic(Logic.NEGATIVE);
        client.admin().authorization().policies().user().create(denyMyAdminAccessingHisAccountPermission).close();
        createUserPermission(myadmin, denyMyAdminAccessingHisAccountPermission);
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertEquals(1, search.size());
        assertEquals(userAlice.getUsername(), search.get(0).getUsername());
    }

    @Test
    public void testManageUserPermissionDenyByDefault() {
        UserPolicyRepresentation disallowMyAdmin = new UserPolicyRepresentation();
        disallowMyAdmin.setName("Not Only My Admin User Policy");
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        disallowMyAdmin.addUser(myadmin.getId());
        disallowMyAdmin.setLogic(Logic.NEGATIVE);
        client.admin().authorization().policies().user().create(disallowMyAdmin).close();
        createAllUserPermission(disallowMyAdmin);
        UserPolicyRepresentation allowAliceOnlyForMyAdmin = new UserPolicyRepresentation();
        allowAliceOnlyForMyAdmin.setName("My Admin User Policy");
        allowAliceOnlyForMyAdmin.addUser(myadmin.getId());
        client.admin().authorization().policies().user().create(allowAliceOnlyForMyAdmin).close();
        createUserPermission(userAlice.admin().toRepresentation(), allowAliceOnlyForMyAdmin);
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertEquals(1, search.size());
        assertEquals(userAlice.getUsername(), search.get(0).getUsername());
    }

    private ScopePermissionRepresentation createAllUserPermission(UserPolicyRepresentation policy) {
        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();

        permission.setName(KeycloakModelUtils.generateId());
        permission.setResourceType(AdminPermissionsSchema.USERS.getType());
        permission.setScopes(AdminPermissionsSchema.USERS.getScopes());
        permission.addPolicy(policy.getName());

        createPermission(permission);

        return permission;
    }

    private ScopePermissionRepresentation createUserPermission(UserRepresentation user, UserPolicyRepresentation... policies) {
        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();

        permission.setName(KeycloakModelUtils.generateId());
        permission.setResourceType(AdminPermissionsSchema.USERS.getType());
        permission.addResource(user.getId());
        permission.setScopes(AdminPermissionsSchema.USERS.getScopes());

        for (UserPolicyRepresentation policy : policies) {
            permission.addPolicy(policy.getName());
        }

        createPermission(permission);

        return permission;
    }
}
