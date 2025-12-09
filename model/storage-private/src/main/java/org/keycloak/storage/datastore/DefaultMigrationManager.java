/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.storage.datastore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.keycloak.common.Version;
import org.keycloak.migration.MigrationModel;
import org.keycloak.migration.ModelVersion;
import org.keycloak.migration.migrators.MigrateTo12_0_0;
import org.keycloak.migration.migrators.MigrateTo14_0_0;
import org.keycloak.migration.migrators.MigrateTo18_0_0;
import org.keycloak.migration.migrators.MigrateTo1_2_0;
import org.keycloak.migration.migrators.MigrateTo1_3_0;
import org.keycloak.migration.migrators.MigrateTo1_4_0;
import org.keycloak.migration.migrators.MigrateTo1_5_0;
import org.keycloak.migration.migrators.MigrateTo1_6_0;
import org.keycloak.migration.migrators.MigrateTo1_7_0;
import org.keycloak.migration.migrators.MigrateTo1_8_0;
import org.keycloak.migration.migrators.MigrateTo1_9_0;
import org.keycloak.migration.migrators.MigrateTo1_9_2;
import org.keycloak.migration.migrators.MigrateTo20_0_0;
import org.keycloak.migration.migrators.MigrateTo21_0_0;
import org.keycloak.migration.migrators.MigrateTo22_0_0;
import org.keycloak.migration.migrators.MigrateTo23_0_0;
import org.keycloak.migration.migrators.MigrateTo24_0_0;
import org.keycloak.migration.migrators.MigrateTo24_0_3;
import org.keycloak.migration.migrators.MigrateTo25_0_0;
import org.keycloak.migration.migrators.MigrateTo26_0_0;
import org.keycloak.migration.migrators.MigrateTo26_1_0;
import org.keycloak.migration.migrators.MigrateTo26_2_0;
import org.keycloak.migration.migrators.MigrateTo26_3_0;
import org.keycloak.migration.migrators.MigrateTo26_4_0;
import org.keycloak.migration.migrators.MigrateTo26_4_3;
import org.keycloak.migration.migrators.MigrateTo2_0_0;
import org.keycloak.migration.migrators.MigrateTo2_1_0;
import org.keycloak.migration.migrators.MigrateTo2_2_0;
import org.keycloak.migration.migrators.MigrateTo2_3_0;
import org.keycloak.migration.migrators.MigrateTo2_5_0;
import org.keycloak.migration.migrators.MigrateTo3_0_0;
import org.keycloak.migration.migrators.MigrateTo3_1_0;
import org.keycloak.migration.migrators.MigrateTo3_2_0;
import org.keycloak.migration.migrators.MigrateTo3_4_0;
import org.keycloak.migration.migrators.MigrateTo3_4_1;
import org.keycloak.migration.migrators.MigrateTo3_4_2;
import org.keycloak.migration.migrators.MigrateTo4_0_0;
import org.keycloak.migration.migrators.MigrateTo4_2_0;
import org.keycloak.migration.migrators.MigrateTo4_6_0;
import org.keycloak.migration.migrators.MigrateTo6_0_0;
import org.keycloak.migration.migrators.MigrateTo8_0_0;
import org.keycloak.migration.migrators.MigrateTo8_0_2;
import org.keycloak.migration.migrators.MigrateTo9_0_0;
import org.keycloak.migration.migrators.MigrateTo9_0_4;
import org.keycloak.migration.migrators.Migration;
import org.keycloak.models.Constants;
import org.keycloak.models.DeploymentStateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.MigrationManager;

import org.jboss.logging.Logger;

/**
 * This wraps the functionality for migrations of the storage.
 *
 * @author Alexander Schwartz
 */
public class DefaultMigrationManager implements MigrationManager {
    private static final Logger logger = Logger.getLogger(DefaultMigrationManager.class);

    private static final Migration[] migrations = {
            new MigrateTo1_2_0(),
            new MigrateTo1_3_0(),
            new MigrateTo1_4_0(),
            new MigrateTo1_5_0(),
            new MigrateTo1_6_0(),
            new MigrateTo1_7_0(),
            new MigrateTo1_8_0(),
            new MigrateTo1_9_0(),
            new MigrateTo1_9_2(),
            new MigrateTo2_0_0(),
            new MigrateTo2_1_0(),
            new MigrateTo2_2_0(),
            new MigrateTo2_3_0(),
            new MigrateTo2_5_0(),
            new MigrateTo3_0_0(),
            new MigrateTo3_1_0(),
            new MigrateTo3_2_0(),
            new MigrateTo3_4_0(),
            new MigrateTo3_4_1(),
            new MigrateTo3_4_2(),
            new MigrateTo4_0_0(),
            new MigrateTo4_2_0(),
            new MigrateTo4_6_0(),
            new MigrateTo6_0_0(),
            new MigrateTo8_0_0(),
            new MigrateTo8_0_2(),
            new MigrateTo9_0_0(),
            new MigrateTo9_0_4(),
            new MigrateTo12_0_0(),
            new MigrateTo14_0_0(),
            new MigrateTo18_0_0(),
            new MigrateTo20_0_0(),
            new MigrateTo21_0_0(),
            new MigrateTo22_0_0(),
            new MigrateTo23_0_0(),
            new MigrateTo24_0_0(),
            new MigrateTo24_0_3(),
            new MigrateTo25_0_0(),
            new MigrateTo26_0_0(),
            new MigrateTo26_1_0(),
            new MigrateTo26_2_0(),
            new MigrateTo26_3_0(),
            new MigrateTo26_4_0(),
            new MigrateTo26_4_3()
    };

    private final KeycloakSession session;
    private final boolean allowMigrateExistingDatabaseToSnapshot;

    public DefaultMigrationManager(KeycloakSession session, boolean allowMigrateExistingDatabaseToSnapshot) {
        this.session = session;
        this.allowMigrateExistingDatabaseToSnapshot = allowMigrateExistingDatabaseToSnapshot;
    }

    @Override
    public void migrate() {
        session.setAttribute(Constants.STORAGE_BATCH_ENABLED, Boolean.getBoolean("keycloak.migration.batch-enabled"));
        session.setAttribute(Constants.STORAGE_BATCH_SIZE, Integer.getInteger("keycloak.migration.batch-size"));
        MigrationModel model = session.getProvider(DeploymentStateProvider.class).getMigrationModel();

        ModelVersion currentVersion = new ModelVersion(Version.VERSION);
        ModelVersion latestUpdate = migrations[migrations.length-1].getVersion();
        ModelVersion databaseVersion = model.getStoredVersion() != null ? new ModelVersion(model.getStoredVersion()) : null;

        if (SNAPSHOT_VERSION.equals(currentVersion) && databaseVersion != null && databaseVersion.lessThan(SNAPSHOT_VERSION) && !allowMigrateExistingDatabaseToSnapshot) {
            throw new ModelException("Incorrect state of migration. You are trying to run nightly server version '" + currentVersion + "' against a database, which was previously migrated to version '" + databaseVersion +
                    "'. This indicates that you are trying to run development server version against production database, which can result in a loss or corruption of data, and also does not allow upgrading. If it is intended, " +
                    "use the option spi-datastore-legacy-allow-migrate-existing-database-to-snapshot of the datastore provider when starting the server and explicitly set it to true.");
        }
        if (databaseVersion == null || databaseVersion.lessThan(latestUpdate)) {
            for (Migration m : migrations) {
                if (databaseVersion == null || databaseVersion.lessThan(m.getVersion())) {
                    if (databaseVersion != null) {
                        logger.infof("Migrating older model to %s", m.getVersion());
                    }
                    m.migrate(session);
                }
            }
        } else if (currentVersion.lessThan(databaseVersion)) {
            if (databaseVersion.equals(SNAPSHOT_VERSION)) {
                throw new ModelException("Incorrect state of migration. You are trying to run server version '" + currentVersion + "' against a database which was migrated to snapshot version '"
                        + databaseVersion + "'. Databases that have been migrated to a snapshot version can't be migrated to a released version of Keycloak or to a more recent snapshot version.");
            } else {
                logger.warnf("Possibly incorrect state of migration. You are trying to run server version '" + currentVersion + "' against database, which was already migrated to newer version '"  +
                        databaseVersion + "'.");
            }
        }

        if (databaseVersion == null || databaseVersion.lessThan(currentVersion)) {
            model.setStoredVersion(currentVersion.toString());
        }

        Version.RESOURCES_VERSION = model.getResourcesTag();
    }

    public static final ModelVersion RHSSO_VERSION_7_0_KEYCLOAK_VERSION = new ModelVersion("1.9.8");
    public static final ModelVersion RHSSO_VERSION_7_1_KEYCLOAK_VERSION = new ModelVersion("2.5.5");
    public static final ModelVersion RHSSO_VERSION_7_2_KEYCLOAK_VERSION = new ModelVersion("3.4.3");
    public static final ModelVersion RHSSO_VERSION_7_3_KEYCLOAK_VERSION = new ModelVersion("4.8.3");
    public static final ModelVersion RHSSO_VERSION_7_4_KEYCLOAK_VERSION = new ModelVersion("9.0.3");
    public static final ModelVersion SNAPSHOT_VERSION = new ModelVersion(Constants.SNAPSHOT_VERSION);

    private static final Map<Pattern, ModelVersion> PATTERN_MATCHER = new LinkedHashMap<>();
    static {
        PATTERN_MATCHER.put(Pattern.compile("^7\\.0\\.\\d+\\.GA$"), RHSSO_VERSION_7_0_KEYCLOAK_VERSION);
        PATTERN_MATCHER.put(Pattern.compile("^7\\.1\\.\\d+\\.GA$"), RHSSO_VERSION_7_1_KEYCLOAK_VERSION);
        PATTERN_MATCHER.put(Pattern.compile("^7\\.2\\.\\d+\\.GA$"), RHSSO_VERSION_7_2_KEYCLOAK_VERSION);
        PATTERN_MATCHER.put(Pattern.compile("^7\\.3\\.\\d+\\.GA$"), RHSSO_VERSION_7_3_KEYCLOAK_VERSION);
        PATTERN_MATCHER.put(Pattern.compile("^7\\.4\\.\\d+\\.GA$"), RHSSO_VERSION_7_4_KEYCLOAK_VERSION);
    }

    @Override
    public void migrate(RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        ModelVersion stored = getModelVersionFromRep(rep);
        if (stored == null) {
            stored = migrations[0].getVersion();
        } else {
            ModelVersion currentVersion = new ModelVersion(Version.VERSION);
            if (currentVersion.lessThan(stored)) {
                logger.warnf("Possibly incorrect state of migration during realm import. You are running server version '" + currentVersion + "' when importing JSON file, which was created in the newer version '"  +
                        stored + "'.");
            }
        }

        for (Migration m : migrations) {
            if (stored == null || stored.lessThan(m.getVersion())) {
                if (stored != null) {
                    logger.debugf("Migrating older json representation to %s", m.getVersion());
                }
                try {
                    m.migrateImport(session, realm, rep, skipUserDependent);
                } catch (Exception e) {
                    logger.error("Failed to migrate json representation for version: " + m.getVersion(), e);
                }
            }
        }
    }

    public static ModelVersion convertRHSSOVersionToKeycloakVersion(String version) {
        // look for the keycloakVersion pattern to identify it as RH SSO
        for (var entry : PATTERN_MATCHER.entrySet()) {
            if (entry.getKey().matcher(version).find()) {
                return entry.getValue();
            }
        }
        // chceck if the version is in format for CD releases, e.g.: "keycloakVersion": "6"
        if (Pattern.compile("^[0-9]*$").matcher(version).find()) {
            return new ModelVersion(Integer.parseInt(version), 0, 0);
        }
        return null;
    }

    public static ModelVersion getModelVersionFromRep(RealmRepresentation rep) {
        ModelVersion version = null;
        if (rep.getKeycloakVersion() != null) {
            version = convertRHSSOVersionToKeycloakVersion(rep.getKeycloakVersion());
            if (version == null) {
                version = new ModelVersion(rep.getKeycloakVersion());
            }
        }
        return version;
    }

}
