/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class RealmAdminAccessTest extends AbstractPermissionTest {

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @Test
    public void testRealmAdminAccess() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        ClientRepresentation realmManagement = realm.admin().clients().findByClientId("realm-management").get(0);
        RoleRepresentation realmAdminRole = realm.admin().clients().get(realmManagement.getId()).roles().get(AdminRoles.REALM_ADMIN).toRepresentation();
        realm.admin().users().get(myadmin.getId()).roles().clientLevel(realmManagement.getId()).add(List.of(realmAdminRole));

        assertThat(realmAdminClient.realm(realm.getName()).users().search("myadmin"), is(not(empty())));
        assertThat(realmAdminClient.realm(realm.getName()).clients().findAll(), is(not(empty())));
        RealmRepresentation realmRep = realmAdminClient.realm(realm.getName()).toRepresentation();

        realmRep.setAdminPermissionsEnabled(!realmRep.isAdminPermissionsEnabled());
        realmAdminClient.realm(realmRep.getRealm()).update(realmRep);
        realmRep.setAdminPermissionsEnabled(!realmRep.isAdminPermissionsEnabled());
        realmAdminClient.realm(realmRep.getRealm()).update(realmRep);

        try {
            assertThat(realmAdminClient.realm("master").clients().findAll(), is(not(empty())));
            fail("Should not have access to other realm");
        } catch (ForbiddenException ignore) {
        }

        RealmRepresentation myrealm = new RealmRepresentation();
        myrealm.setRealm("myrealm");
        myrealm.setEnabled(true);

        try {
            realmAdminClient.realms().create(myrealm);
            fail("Should not have access to create realms");
        } catch (ForbiddenException ignore) {
        }

        try (Keycloak client = adminClientFactory.create().realm("master")
                .username("admin").password("admin").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
            try {
                Assertions.assertNotNull(client.serverInfo().getInfo());
                client.realms().create(myrealm);

                assertThat(realmAdminClient.realms().findAll(), hasSize(1));
                assertThat(realmAdminClient.realms().findAll().get(0).getRealm(), is(realm.getName()));

                try {
                    realmAdminClient.realm(myrealm.getRealm()).remove();
                    fail("Should not have access to other realm");
                } catch (ForbiddenException ignore) {
                }

                try {
                    assertThat(realmAdminClient.realm(myrealm.getRealm()).users().search(null), is(not(empty())));
                    fail("Should not have access to other realm");
                } catch (ForbiddenException ignore) {
                }

                try {
                    assertThat(realmAdminClient.realm(myrealm.getRealm()).clients().findAll(), is(not(empty())));
                    fail("Should not have access to other realm");
                } catch (ForbiddenException ignore) {
                }
            } finally {
                client.realm(myrealm.getRealm()).remove();
            }
        }
    }
}
