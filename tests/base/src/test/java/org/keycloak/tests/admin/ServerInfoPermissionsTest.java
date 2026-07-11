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



import jakarta.ws.rs.ForbiddenException;

import org.keycloak.models.AdminRoles;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;

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

        // user in master with manage-realm permission can see the system info
        ServerInfoRepresentation serverInfo = clients.get("master-admin-" + AdminRoles.MANAGE_REALM).serverInfo().getInfo();
        Assert.assertNotNull(serverInfo.getSystemInfo());
        Assert.assertNotNull(serverInfo.getSystemInfo().getJavaVersion());
        Assert.assertNotNull(serverInfo.getCpuInfo());
        Assert.assertNotNull(serverInfo.getMemoryInfo());

        // server admin user can see the full system info
        serverInfo = clients.get("master-admin").serverInfo().getInfo();
        Assert.assertNotNull(serverInfo.getSystemInfo());
        Assert.assertNotNull(serverInfo.getSystemInfo().getJavaVersion());
        Assert.assertNotNull(serverInfo.getCpuInfo());
        Assert.assertNotNull(serverInfo.getMemoryInfo());

        // delegated admin in the master realm with view-realm role can not view full server info
        serverInfo = clients.get("master-admin-" + AdminRoles.VIEW_REALM).serverInfo().getInfo();
        Assert.assertNull(serverInfo.getSystemInfo());
        Assert.assertNull(serverInfo.getCpuInfo());
        Assert.assertNull(serverInfo.getMemoryInfo());

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
    }

    protected static class PermissionsTestRealm extends PermissionsTestRealmConfig1 {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            // configure with permissions enable to test view-system assignment
            return super.configure(realm).adminPermissionsEnabled(true);
        }
    }
}
