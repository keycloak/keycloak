/*
 * Copyright 2018 Bosch Software Innovations GmbH
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

import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.representations.idm.*;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:leon.graser@bosch-si.com">Leon Graser</a>
 */
public class ManagementPermissionsTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    @Test
    public void updateGroupPermissions() {
        RealmResource realmResource = adminClient.realms().realm("test");
        GroupRepresentation group = new GroupRepresentation();
        group.setName("perm-group-test");
        Response response = realmResource.groups().add(group);
        String id = ApiUtil.getCreatedId(response);
        GroupResource groupResource = realmResource.groups().group(id);

        ManagementPermissionReference result = groupResource.setPermissions(new ManagementPermissionRepresentation(true));
        assertNotNull(result);
        assertTrue(result.isEnabled());
        result = groupResource.getPermissions();
        assertNotNull(result);
        assertTrue(result.isEnabled());

        result = groupResource.setPermissions(new ManagementPermissionRepresentation(false));
        assertNotNull(result);
        assertFalse(result.isEnabled());
        result = groupResource.getPermissions();
        assertNotNull(result);
        assertFalse(result.isEnabled());

        result = groupResource.setPermissions(new ManagementPermissionRepresentation(true));
        assertNotNull(result);
        assertTrue(result.isEnabled());
        result = groupResource.getPermissions();
        assertNotNull(result);
        assertTrue(result.isEnabled());

        result = groupResource.setPermissions(new ManagementPermissionRepresentation(true));
        assertNotNull(result);
        assertTrue(result.isEnabled());
        result = groupResource.getPermissions();
        assertNotNull(result);
        assertTrue(result.isEnabled());

        result = groupResource.setPermissions(new ManagementPermissionRepresentation(false));
        assertNotNull(result);
        assertFalse(result.isEnabled());
        result = groupResource.getPermissions();
        assertNotNull(result);
        assertFalse(result.isEnabled());

        result = groupResource.setPermissions(new ManagementPermissionRepresentation(false));
        assertNotNull(result);
        assertFalse(result.isEnabled());
        result = groupResource.getPermissions();
        assertNotNull(result);
        assertFalse(result.isEnabled());
    }

    @Test
    public void updateClientPermissions() {
        RealmResource realmResource = adminClient.realms().realm("test");
        ClientRepresentation clientRepresentation = new ClientRepresentation();
        clientRepresentation.setName("perm-client-test");
        Response response = realmResource.clients().create(clientRepresentation);
        String id = ApiUtil.getCreatedId(response);
        ClientResource clientResource = realmResource.clients().get(id);

        ManagementPermissionReference result = clientResource.setPermissions(new ManagementPermissionRepresentation(true));
        assertNotNull(result);
        assertTrue(result.isEnabled());
        result = clientResource.getPermissions();
        assertNotNull(result);
        assertTrue(result.isEnabled());

        result = clientResource.setPermissions(new ManagementPermissionRepresentation(false));
        assertNotNull(result);
        assertFalse(result.isEnabled());
        result = clientResource.getPermissions();
        assertNotNull(result);
        assertFalse(result.isEnabled());

        result = clientResource.setPermissions(new ManagementPermissionRepresentation(true));
        assertNotNull(result);
        assertTrue(result.isEnabled());
        result = clientResource.getPermissions();
        assertNotNull(result);
        assertTrue(result.isEnabled());

        result = clientResource.setPermissions(new ManagementPermissionRepresentation(true));
        assertNotNull(result);
        assertTrue(result.isEnabled());
        result = clientResource.getPermissions();
        assertNotNull(result);
        assertTrue(result.isEnabled());

        result = clientResource.setPermissions(new ManagementPermissionRepresentation(false));
        assertNotNull(result);
        assertFalse(result.isEnabled());
        result = clientResource.getPermissions();
        assertNotNull(result);
        assertFalse(result.isEnabled());

        result = clientResource.setPermissions(new ManagementPermissionRepresentation(false));
        assertNotNull(result);
        assertFalse(result.isEnabled());
        result = clientResource.getPermissions();
        assertNotNull(result);
        assertFalse(result.isEnabled());
    }

    @Test
    public void updateRealmRolePermissions() {
        RealmResource realmResource = adminClient.realms().realm("test");
        RoleRepresentation roleRepresentation = new RoleRepresentation();
        roleRepresentation.setName("perm-role-test");
        realmResource.roles().create(roleRepresentation);
        RoleResource roleResource = realmResource.roles().get("perm-role-test");

        ManagementPermissionReference result = roleResource.setPermissions(new ManagementPermissionRepresentation(true));
        assertNotNull(result);
        assertTrue(result.isEnabled());
        result = roleResource.getPermissions();
        assertNotNull(result);
        assertTrue(result.isEnabled());

        result = roleResource.setPermissions(new ManagementPermissionRepresentation(false));
        assertNotNull(result);
        assertFalse(result.isEnabled());
        result = roleResource.getPermissions();
        assertNotNull(result);
        assertFalse(result.isEnabled());

        result = roleResource.setPermissions(new ManagementPermissionRepresentation(true));
        assertNotNull(result);
        assertTrue(result.isEnabled());
        result = roleResource.getPermissions();
        assertNotNull(result);
        assertTrue(result.isEnabled());

        result = roleResource.setPermissions(new ManagementPermissionRepresentation(true));
        assertNotNull(result);
        assertTrue(result.isEnabled());
        result = roleResource.getPermissions();
        assertNotNull(result);
        assertTrue(result.isEnabled());

        result = roleResource.setPermissions(new ManagementPermissionRepresentation(false));
        assertNotNull(result);
        assertFalse(result.isEnabled());
        result = roleResource.getPermissions();
        assertNotNull(result);
        assertFalse(result.isEnabled());

        result = roleResource.setPermissions(new ManagementPermissionRepresentation(false));
        assertNotNull(result);
        assertFalse(result.isEnabled());
        result = roleResource.getPermissions();
        assertNotNull(result);
        assertFalse(result.isEnabled());
    }

    @Test
    public void updateClientRolePermissions() {
        RealmResource realmResource = adminClient.realms().realm("test");
        ClientRepresentation clientRepresentation = new ClientRepresentation();
        clientRepresentation.setName("perm-client-test");
        Response response = realmResource.clients().create(clientRepresentation);
        String id = ApiUtil.getCreatedId(response);
        ClientResource clientResource = realmResource.clients().get(id);
        RoleRepresentation roleRepresentation = new RoleRepresentation();
        roleRepresentation.setName("perm-client-role-test");
        clientResource.roles().create(roleRepresentation);
        RoleResource roleResource = clientResource.roles().get("perm-client-role-test");

        ManagementPermissionReference result = roleResource.setPermissions(new ManagementPermissionRepresentation(true));
        assertNotNull(result);
        assertTrue(result.isEnabled());
        result = roleResource.getPermissions();
        assertNotNull(result);
        assertTrue(result.isEnabled());

        result = roleResource.setPermissions(new ManagementPermissionRepresentation(false));
        assertNotNull(result);
        assertFalse(result.isEnabled());
        result = roleResource.getPermissions();
        assertNotNull(result);
        assertFalse(result.isEnabled());

        result = roleResource.setPermissions(new ManagementPermissionRepresentation(true));
        assertNotNull(result);
        assertTrue(result.isEnabled());
        result = roleResource.getPermissions();
        assertNotNull(result);
        assertTrue(result.isEnabled());

        result = roleResource.setPermissions(new ManagementPermissionRepresentation(true));
        assertNotNull(result);
        assertTrue(result.isEnabled());
        result = roleResource.getPermissions();
        assertNotNull(result);
        assertTrue(result.isEnabled());

        result = roleResource.setPermissions(new ManagementPermissionRepresentation(false));
        assertNotNull(result);
        assertFalse(result.isEnabled());
        result = roleResource.getPermissions();
        assertNotNull(result);
        assertFalse(result.isEnabled());

        result = roleResource.setPermissions(new ManagementPermissionRepresentation(false));
        assertNotNull(result);
        assertFalse(result.isEnabled());
        result = roleResource.getPermissions();
        assertNotNull(result);
        assertFalse(result.isEnabled());
    }
}
