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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.DeploymentTargetModifier;
import org.keycloak.testsuite.arquillian.migration.Migration;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;

import javax.ws.rs.NotFoundException;
import java.util.List;

import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class MigrationTest extends AbstractMigrationTest {

    @Deployment
    @TargetsContainer(DeploymentTargetModifier.AUTH_SERVER_CURRENT)
    public static WebArchive deploy() {
        return RunOnServerDeployment.create();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        log.info("Adding no test realms for migration test. Test realm should be migrated from previous vesrion.");
    }
    @Before
    public void beforeMigrationTest() {
        migrationRealm = adminClient.realms().realm(MIGRATION);
        migrationRealm2 = adminClient.realms().realm(MIGRATION2);
        migrationRealm3 = adminClient.realms().realm("authorization");
        masterRealm = adminClient.realms().realm(MASTER);
        //add migration realms to testRealmReps to make them removed after test
        addTestRealmToTestRealmReps(migrationRealm);
        addTestRealmToTestRealmReps(migrationRealm2);
        addTestRealmToTestRealmReps(migrationRealm3);
    }
    private void addTestRealmToTestRealmReps(RealmResource realm) {
        try {
            testRealmReps.add(realm.toRepresentation());
        } catch (NotFoundException ex) {
        }
    }
    @Test
    @Migration(versionFrom = "2.5.5.Final")
    public void migration2_5_5Test() {
        testMigratedData();
        testMigrationTo3_x_and_higher();
    }
    @Test
    @Migration(versionFrom = "1.9.8.Final")
    public void migration1_9_8Test() throws Exception {
        testMigratedData();
        testMigrationTo2_0_0();
        testMigrationTo2_1_0();
        testMigrationTo2_2_0();
        testMigrationTo2_3_0();
        testMigrationTo2_5_0();
        testMigrationTo2_5_1();
        testMigrationTo3_x_and_higher();
    }

    @Test
    @Migration(versionFrom = "2.2.1.Final")
    public void migrationInAuthorizationServicesTest() {
        testDroolsToRulesPolicyTypeMigration();
    }

}
