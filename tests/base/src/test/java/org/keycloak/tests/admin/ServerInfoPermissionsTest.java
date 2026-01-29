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
package org.keycloak.tests.admin;

import java.util.Collections;

import jakarta.ws.rs.ForbiddenException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfigBuilder;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest
public class ServerInfoPermissionsTest extends AbstractPermissionsTest {

    @InjectRealm(config = PermissionsTestRealm.class, ref = "realm1")
    ManagedRealm managedRealm1;

    @Test
    public void testServerInfo() throws Exception {
        // user in master with no permission => forbidden
        Assert.assertThrows(ForbiddenException.class, () -> clients.get("master-none").serverInfo().getInfo());
        // user in master with any permission can see the system info
        ServerInfoRepresentation serverInfo = clients.get("master-view-realm").serverInfo().getInfo();
        Assert.assertNotNull(serverInfo.getSystemInfo());
        Assert.assertNotNull(serverInfo.getSystemInfo().getJavaVersion());
        Assert.assertNotNull(serverInfo.getCpuInfo());
        Assert.assertNotNull(serverInfo.getMemoryInfo());

        // user in test realm with no permission => forbidden
        Assert.assertThrows(ForbiddenException.class, () -> clients.get("none").serverInfo().getInfo());
        // user in test realm with any permission cannot see the system info
        serverInfo = clients.get("view-realm").serverInfo().getInfo();
        Assert.assertNull(serverInfo.getSystemInfo());
        Assert.assertNull(serverInfo.getCpuInfo());
        Assert.assertNull(serverInfo.getMemoryInfo());
        serverInfo = clients.get("manage-users").serverInfo().getInfo();
        Assert.assertNull(serverInfo.getSystemInfo());
        Assert.assertNull(serverInfo.getCpuInfo());
        Assert.assertNull(serverInfo.getMemoryInfo());
        // user with manage realm can only see the version
        serverInfo = clients.get("manage-realm").serverInfo().getInfo();
        Assert.assertNotNull(serverInfo.getSystemInfo());
        Assert.assertNotNull(serverInfo.getSystemInfo().getVersion());
        Assert.assertNull(serverInfo.getSystemInfo().getJavaVersion());
        Assert.assertNull(serverInfo.getSystemInfo().getOsName());
        Assert.assertNull(serverInfo.getSystemInfo().getServerTime());
        Assert.assertNull(serverInfo.getCpuInfo());
        Assert.assertNull(serverInfo.getMemoryInfo());

        // assign the view-system permission to a test realm user and check the fallback works
        ClientRepresentation realmMgtRep = adminClient.realm(REALM_NAME).clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        ClientResource realmMgtRes = adminClient.realm(REALM_NAME).clients().get(realmMgtRep.getId());
        RoleRepresentation createViewSystem = new RoleRepresentation();
        createViewSystem.setName(AdminRoles.VIEW_SYSTEM);
        realmMgtRes.roles().create(createViewSystem);
        final RoleRepresentation viewSystem = realmMgtRes.roles().get(AdminRoles.VIEW_SYSTEM).toRepresentation();
        UserRepresentation userRep = adminClient.realm(REALM_NAME).users().search("view-realm", Boolean.TRUE).get(0);
        // view-system cannot be assigned by admin in the permissions realm
        Assert.assertThrows(ForbiddenException.class, () -> clients.get("realm-admin")
                .realm(REALM_NAME).users().get(userRep.getId()).roles().clientLevel(realmMgtRep.getId())
                .add(Collections.singletonList(viewSystem)));
        // view-system can be assigned by a master realm-admin using FGAP
        UserResource userRes = adminClient.realm(REALM_NAME).users().get(userRep.getId());
        userRes.roles().clientLevel(realmMgtRep.getId())
                .add(Collections.singletonList(viewSystem));
        try (Keycloak keycloak = adminClientFactory.create().realm(REALM_NAME)
                .username(userRep.getUsername()).password("password").clientId("test-client")
                .build()) {
            serverInfo = keycloak.serverInfo().getInfo();
            Assert.assertNotNull(serverInfo.getSystemInfo());
            Assert.assertNotNull(serverInfo.getSystemInfo().getJavaVersion());
            Assert.assertNotNull(serverInfo.getCpuInfo());
            Assert.assertNotNull(serverInfo.getMemoryInfo());
        } finally {
            userRes.roles().clientLevel(realmMgtRep.getId()).remove(Collections.singletonList(viewSystem));
            realmMgtRes.roles().get(AdminRoles.VIEW_SYSTEM).remove();
        }
    }

    protected static class PermissionsTestRealm extends PermissionsTestRealmConfig1 {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            // configure with permissions enable to test view-system assignment
            return super.configure(realm).adminPermissionsEnabled(true);
        }
    }
}
