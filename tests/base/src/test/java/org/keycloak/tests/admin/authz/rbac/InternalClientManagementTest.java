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
package org.keycloak.tests.admin.authz.rbac;

import java.util.Set;

import org.keycloak.Config;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@KeycloakIntegrationTest
public class InternalClientManagementTest extends AbstractAdminRBACTest {

    private static final String TEMP_ROLE_NAME = "xyzzy-temp-role";

    @Test
    public void testManageClientsAdminCannotRenameRoleInRealmManagementClient() {
        createRealm(adminClient, "myrealm");

        grantMasterRealmManagementRole("myrealm", masterUser.getUsername(), AdminRoles.MANAGE_CLIENTS);

        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            ClientRepresentation realmMgmt = client.realm("myrealm").clients()
                    .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
            ClientResource realmMgmtResource = client.realm("myrealm").clients().get(realmMgmt.getId());
            RoleRepresentation realmAdminRole = realmMgmtResource.roles()
                    .get(AdminRoles.REALM_ADMIN).toRepresentation();

            assertForbidden("manage-clients admin must not rename realm-admin in realm-management",
                    () -> client.realm("myrealm").rolesById().updateRole(realmAdminRole.getId(), renamed(realmAdminRole)));
        });
    }

    @Test
    public void testMasterRealmManageClientsAdminCannotRenameRoleInRealmAdminContainer() {
        createRealm(adminClient, "myrealm");

        // Grant manage-clients from "master-realm" client — the admin container in master realm
        grantMasterRealmManagementRole(Config.getAdminRealm(), masterUser.getUsername(), AdminRoles.MANAGE_CLIENTS);

        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            // "myrealm-realm" is the admin container for "myrealm", living in the master realm
            ClientRepresentation realmAdminContainer = client.realm(masterRealm.getName()).clients()
                    .findByClientId("myrealm-realm").get(0);
            ClientResource realmAdminContainerResource = client.realm(masterRealm.getName()).clients()
                    .get(realmAdminContainer.getId());
            RoleRepresentation manageRealmRole = realmAdminContainerResource.roles()
                    .get(AdminRoles.MANAGE_REALM).toRepresentation();

            assertForbidden("master-realm manage-clients admin must not rename protected admin roles in myrealm-realm",
                    () -> client.realm(masterRealm.getName()).rolesById()
                            .updateRole(manageRealmRole.getId(), renamed(manageRealmRole)));
        });
    }

    @Test
    public void testMasterRealmManageRealmAdminCannotRenameAdminRealmRole() {
        // Grant manage-realm from "master-realm" client — the admin container in master realm
        grantMasterRealmManagementRole(Config.getAdminRealm(), masterUser.getUsername(), AdminRoles.MANAGE_REALM);

        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            for (String name : Set.of(AdminRoles.ADMIN, AdminRoles.CREATE_REALM)) {
                RoleRepresentation adminRole = client.realm(masterRealm.getName()).roles()
                        .get(name).toRepresentation();

                assertForbidden("master-realm manage-realm admin must not rename the '" + name + "' realm role",
                        () -> client.realm(masterRealm.getName()).rolesById()
                                .updateRole(adminRole.getId(), renamed(adminRole)));
            }
        });
    }

    @Test
    public void testMasterRealmManageRealmAdminCannotRenameAdminRealmRoleViaNameBasedEndpoint() {
        grantMasterRealmManagementRole(Config.getAdminRealm(), masterUser.getUsername(), AdminRoles.MANAGE_REALM);

        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            for (String name : Set.of(AdminRoles.ADMIN, AdminRoles.CREATE_REALM)) {
                RoleRepresentation adminRole = client.realm(masterRealm.getName()).roles()
                        .get(name).toRepresentation();
                assertForbidden("master-realm manage-realm admin must not rename the '" + name + "' realm role via name-based endpoint",
                        () -> client.realm(masterRealm.getName()).roles().get(name).update(renamed(adminRole)));
            }
        });
    }

    @Test
    public void testMasterRealmManageRealmAdminCannotDeleteAdminRealmRoleViaNameBasedEndpoint() {
        grantMasterRealmManagementRole(Config.getAdminRealm(), masterUser.getUsername(), AdminRoles.MANAGE_REALM);

        runAs(masterRealm.getName(), masterUser.getUsername(), client -> {
            for (String name : Set.of(AdminRoles.ADMIN, AdminRoles.CREATE_REALM)) {
                assertForbidden("master-realm manage-realm admin must not delete the '" + name + "' realm role via name-based endpoint",
                        () -> client.realm(masterRealm.getName()).roles().get(name).remove());

                // Verify the role still exists (it should not have been deleted)
                String roleName = client.realm(masterRealm.getName()).roles().get(name).toRepresentation().getName();
                assertNotEquals(null, roleName);
            }
        });
    }

    private RoleRepresentation renamed(RoleRepresentation original) {
        RoleRepresentation renamed = new RoleRepresentation();
        renamed.setId(original.getId());
        renamed.setName(TEMP_ROLE_NAME);
        renamed.setDescription(original.getDescription());
        return renamed;
    }
}
