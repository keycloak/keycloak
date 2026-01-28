/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.migration;

import java.util.List;

import org.keycloak.common.Version;
import org.keycloak.migration.MigrationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.DeploymentStateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.datastore.DefaultMigrationManager;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MigrationDeniedTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

    }

    /**
     * Tests migration should not be allowed when DB version is set to snapshot version like "999.0.0", but Keycloak server version is lower like "23.0.0"
     */
    @Test
    @ModelTest
    public void testMigrationDeniedWithDBSnapshotAndServerNonSnapshot(KeycloakSession s) {
        KeycloakModelUtils.runJobInTransaction(s.getKeycloakSessionFactory(), (session) -> {
            MigrationModel model = session.getProvider(DeploymentStateProvider.class).getMigrationModel();
            String databaseVersion = model.getStoredVersion();
            Assert.assertNotNull("Stored DB version was null", model.getStoredVersion());

            String currentVersion = Version.VERSION;
            try {
                // Simulate to manually set runtime version of KeycloakServer to 23. Migration should fail as the version is lower than DB version.
                Version.VERSION = "23.0.0";
                model.setStoredVersion(Constants.SNAPSHOT_VERSION);

                new DefaultMigrationManager(session, false).migrate();
                Assert.fail("Not expected to successfully run migration. DB version was " + databaseVersion + ". Keycloak version was " + currentVersion);
            } catch (ModelException expected) {
                Assert.assertTrue(expected.getMessage().startsWith("Incorrect state of migration. You are trying to run server version"));
            } finally {
                // Revert version to the state before the test
                Version.VERSION = currentVersion;
                session.getTransactionManager().rollback();
            }
        });
    }

    /**
     * Tests migration should not be allowed when DB version is set to non-snapshot version like "23.0.0", but Keycloak server version is snapshot version "999.0.0"
     */
    @Test
    @ModelTest
    public void testMigrationDeniedWithDBNonSnapshotAndServerSnapshot(KeycloakSession s) {
        KeycloakModelUtils.runJobInTransaction(s.getKeycloakSessionFactory(), (session) -> {
            MigrationModel model = session.getProvider(DeploymentStateProvider.class).getMigrationModel();
            String databaseVersion = model.getStoredVersion();
            Assert.assertNotNull("Stored DB version was null", model.getStoredVersion());

            String currentVersion = Version.VERSION;
            try {
                // Simulate to manually set DB version to 23 when server version is SNAPSHOT. Migration should fail as it is an attempt to run production DB with the development server
                Version.VERSION = Constants.SNAPSHOT_VERSION;
                model.setStoredVersion("23.0.0");

                new DefaultMigrationManager(session, false).migrate();
                Assert.fail("Not expected to successfully run migration. DB version was " + databaseVersion + ". Keycloak version was " + currentVersion);
            } catch (ModelException expected) {
                Assert.assertTrue(expected.getMessage().startsWith("Incorrect state of migration. You are trying to run nightly server version"));
            } finally {
                // Revert version to the state before the test
                Version.VERSION = currentVersion;
                session.getTransactionManager().rollback();
            }
        });
    }
}
