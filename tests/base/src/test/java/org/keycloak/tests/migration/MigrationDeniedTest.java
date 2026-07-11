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
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.tests.migration;

import org.keycloak.common.Version;
import org.keycloak.migration.MigrationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.DeploymentStateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.datastore.DefaultMigrationManager;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.Assertions;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest
@DatabaseTest
public class MigrationDeniedTest {

    /**
     * Tests migration should not be allowed when DB version is set to snapshot version like "999.0.0",
     * but Keycloak server version is lower like "23.0.0".
     */
    @TestOnServer
    public void testMigrationDeniedWithDBSnapshotAndServerNonSnapshot(KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), currentSession -> {
            MigrationModel model = currentSession.getProvider(DeploymentStateProvider.class).getMigrationModel();
            Assertions.assertNotNull(model.getStoredVersion(), "Stored DB version was null");

            String currentVersion = Version.VERSION;
            try {
                // Simulate to manually set runtime version of KeycloakServer to 23. Migration should fail as the version is lower than DB version.
                Version.VERSION = "23.0.0";
                model.setStoredVersion(Constants.SNAPSHOT_VERSION);

                ModelException exception = Assertions.assertThrows(ModelException.class,
                        () -> new DefaultMigrationManager(currentSession, false).migrate());
                Assertions.assertTrue(exception.getMessage().startsWith("Incorrect state of migration. You are trying to run server version"));
            } finally {
                // Revert version to the state before the test
                Version.VERSION = currentVersion;
                currentSession.getTransactionManager().rollback();
            }
        });
    }

    /**
     * Tests migration should not be allowed when DB version is set to non-snapshot version like "23.0.0",
     * but Keycloak server version is snapshot version "999.0.0".
     */
    @TestOnServer
    public void testMigrationDeniedWithDBNonSnapshotAndServerSnapshot(KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), currentSession -> {
            MigrationModel model = currentSession.getProvider(DeploymentStateProvider.class).getMigrationModel();
            Assertions.assertNotNull(model.getStoredVersion(), "Stored DB version was null");

            String currentVersion = Version.VERSION;
            try {
                // Simulate to manually set DB version to 23 when server version is SNAPSHOT. Migration should fail as it is an attempt to run production DB with the development server.
                Version.VERSION = Constants.SNAPSHOT_VERSION;
                model.setStoredVersion("23.0.0");

                ModelException exception = Assertions.assertThrows(ModelException.class,
                        () -> new DefaultMigrationManager(currentSession, false).migrate());
                Assertions.assertTrue(exception.getMessage().startsWith("Incorrect state of migration. You are trying to run nightly server version"));
            } finally {
                // Revert version to the state before the test
                Version.VERSION = currentVersion;
                currentSession.getTransactionManager().rollback();
            }
        });
    }
}
