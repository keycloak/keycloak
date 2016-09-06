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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.migration.Migration;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class MigrationTest extends AbstractKeycloakTest {

    private RealmResource realmResource;
    private RealmRepresentation realmRep;
        
    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        log.info("Adding no test realms for migration test. Test realm should be migrated from previous vesrion.");
    }
    
    @Before
    public void beforeMigrationTest() {
        realmResource = adminClient.realms().realm("Migration");
        realmRep = realmResource.toRepresentation();
    }
    
    @Test
    @Migration(versionFrom = "1.9.8.Final")
    public void migration198Test() {
        Assert.assertNames(realmResource.roles().list(), "offline_access", "uma_authorization");
        Assert.assertNames(realmResource.clients().findAll(), "admin-cli", "realm-management", "security-admin-console", "broker", "account");
        
        //TODO
    }
    
    /**
     * Assumed that there is only one migration test for each version and *remove*
     * 'Migration' realm from Keycloak after test to be able to run the rest 
     * of the testsuite isolated afterward.
     */
    @After
    public void afterMigrationTest() {
        log.info("removing '" + realmRep.getRealm() + "' realm");
        removeRealm(realmRep);
    }
    
}
