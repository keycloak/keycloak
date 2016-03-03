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

import java.util.List;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.migration.Migration;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class MigrationTest extends AbstractKeycloakTest {
    
    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        log.info("Adding no test realms for migration test. Test realm should be migrated from previous vesrion.");
    }
    
    @Test
    @Migration(versionFrom = "1.6.1.Final")
    public void migration16Test() {
        RealmResource realmResource = adminClient.realms().realm("Migration");
        RealmRepresentation realmRep = realmResource.toRepresentation();
        assertEquals("Migration", realmRep.getRealm());
        
        List<RoleRepresentation> realmRoles = realmResource.roles().list();
        assertEquals(1, realmRoles.size());
        assertEquals("offline_access", realmRoles.get(0).getName());
        
        for (ClientRepresentation client : realmResource.clients().findAll()) {
            final String clientId = client.getClientId();
            switch (clientId) {
                case "realm-management":
                    assertEquals(13, realmResource.clients().get(client.getId()).roles().list().size());
                    break;
                case "security-admin-console":
                    assertEquals(0, realmResource.clients().get(client.getId()).roles().list().size());
                    break;
                case "broker":
                    assertEquals(1, realmResource.clients().get(client.getId()).roles().list().size());
                    break;
                case "account":
                    assertEquals(2, realmResource.clients().get(client.getId()).roles().list().size());
                    break;
                default:
                    fail("Migrated realm contains unexpected client " + clientId);
                    break;
            }
        }
    }
    
    @Test
    @Migration(versionFrom = "1.5.1.Final")
    @Ignore
    public void migration15Test() {
        for (RealmRepresentation realm : adminClient.realms().findAll()) {
            System.out.println(realm.getRealm());
        }
        
        //TODO
    }

}
