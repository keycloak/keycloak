/*
 * Copyright 2026 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.testsuite.exportimport;

import java.util.List;

import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testsuite.AbstractKeycloakTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Reproduces the duplicate KEYCLOAK_ROLE primary key violation reported in issue #49731: when several users
 * reference the same client role that is not declared in {@code roles.client}, the role must be auto-created
 * only once during the batch-mode user import instead of being created for every user that references it.
 */
public class RealmImportSharedUndefinedClientRoleTest extends AbstractKeycloakTest {

    private static final String REALM_NAME = "shared-undefined-client-role";
    private static final String CLIENT_ID = "shared-role-client";
    private static final String ROLE_NAME = "CALL_ACCESS";
    private static final String USER_1 = "user-one";
    private static final String USER_2 = "user-two";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        // The realm under test is imported and removed within the test itself.
    }

    @Test
    public void importRealmWithSharedUndefinedClientRole() {
        // The client declares no roles, yet both users reference the same client role. The import must
        // auto-create the role exactly once instead of failing with a duplicate primary key violation.
        RealmRepresentation realmRep = RealmBuilder.create()
                .name(REALM_NAME)
                .clients(ClientBuilder.create().clientId(CLIENT_ID).enabled(true))
                .users(
                        UserBuilder.create().username(USER_1).enabled(true).clientRoles(CLIENT_ID, ROLE_NAME),
                        UserBuilder.create().username(USER_2).enabled(true).clientRoles(CLIENT_ID, ROLE_NAME))
                .build();

        try {
            // (a) The import must succeed without a duplicate-key / PK violation.
            adminClient.realms().create(realmRep);

            RealmResource realm = adminClient.realm(REALM_NAME);
            String clientUuid = realm.clients().findByClientId(CLIENT_ID).get(0).getId();

            // (b) Exactly one such client role exists on the client after import.
            long matching = realm.clients().get(clientUuid).roles().list().stream()
                    .filter(role -> ROLE_NAME.equals(role.getName()))
                    .count();
            assertEquals("The shared client role must be created exactly once", 1, matching);

            // (c) Both users end up with the shared client-role mapping.
            for (String username : List.of(USER_1, USER_2)) {
                String userId = realm.users().search(username).get(0).getId();
                List<RoleRepresentation> mappings = realm.users().get(userId).roles().clientLevel(clientUuid).listAll();
                assertTrue("User " + username + " must have the shared client role mapped",
                        mappings.stream().anyMatch(role -> ROLE_NAME.equals(role.getName())));
            }
        } finally {
            removeRealm(REALM_NAME);
        }
    }
}
