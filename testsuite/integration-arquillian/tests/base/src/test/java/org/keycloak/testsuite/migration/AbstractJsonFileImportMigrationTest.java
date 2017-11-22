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
package org.keycloak.testsuite.migration;

import org.junit.After;
import org.junit.Before;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractJsonFileImportMigrationTest extends AbstractMigrationTest {
    protected RealmRepresentation masterRep;
    protected String masterTestClientId;

    @Before
    public void beforeMigrationTest() {
        migrationRealm = adminClient.realms().realm(MIGRATION);
        migrationRealm2 = adminClient.realms().realm(MIGRATION2);
        migrationRealm3 = adminClient.realms().realm("authorization");
        masterRealm = adminClient.realms().realm(MASTER);
    }

    /*


        // hack to reuse AbstractMigrationTest  need to create a bunch of stuff in master realm for tests to work

        RoleRepresentation newRole = new RoleRepresentation();
        newRole.setName("master-test-realm-role");

        masterRealm.roles().create(newRole);
        ClientRepresentation newClient = new ClientRepresentation();
        newClient.setClientId("master-test-client");
        masterRealm.clients().create(newClient);
        newClient = masterRealm.clients().findByClientId("master-test-client").get(0);
        newRole.setName("master-test-client-role");
        masterTestClientId = newClient.getId();
        masterRealm.clients().get(masterTestClientId).roles().create(newRole);

        for (GroupRepresentation group : masterRep.getGroups()) {
            group.setId(null);
            masterRealm.groups().add(group);
        }
        for (UserRepresentation user : masterRep.getUsers()) {
            user.setId(null);
            if (!user.getUsername().equals("admin")) masterRealm.users().create(user);
        }
    }

    @After
    public void afterMigrationTest() {
        masterRealm.clients().get(masterTestClientId).remove();
        masterRealm.roles().get("master-test-realm-role").remove();
        GroupRepresentation group = masterRealm.getGroupByPath("/master-test-group");
        masterRealm.groups().group(group.getId()).remove();
        UserRepresentation user = masterRealm.users().search("master-test-user").get(0);
        masterRealm.users().get(user.getId()).remove();

    }
    */
}
