/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
