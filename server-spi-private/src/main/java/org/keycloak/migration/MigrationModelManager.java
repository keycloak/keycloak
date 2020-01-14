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

package org.keycloak.migration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.jboss.logging.Logger;
import org.keycloak.common.Version;
import org.keycloak.migration.migrators.MigrateTo1_2_0;
import org.keycloak.migration.migrators.MigrateTo1_3_0;
import org.keycloak.migration.migrators.MigrateTo1_4_0;
import org.keycloak.migration.migrators.MigrateTo1_5_0;
import org.keycloak.migration.migrators.MigrateTo1_6_0;
import org.keycloak.migration.migrators.MigrateTo1_7_0;
import org.keycloak.migration.migrators.MigrateTo1_8_0;
import org.keycloak.migration.migrators.MigrateTo1_9_0;
import org.keycloak.migration.migrators.MigrateTo1_9_2;
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
import org.keycloak.migration.migrators.Migration;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MigrationModelManager {
    private static Logger logger = Logger.getLogger(MigrationModelManager.class);

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
            new MigrateTo9_0_0()
    };

    public static void migrate(KeycloakSession session) {
        MigrationModel model = session.realms().getMigrationModel();

        ModelVersion currentVersion = new ModelVersion(Version.VERSION_KEYCLOAK);
        ModelVersion latestUpdate = migrations[migrations.length-1].getVersion();
        ModelVersion databaseVersion = model.getStoredVersion() != null ? new ModelVersion(model.getStoredVersion()) : null;

        if (databaseVersion == null || databaseVersion.lessThan(latestUpdate)) {
            for (Migration m : migrations) {
                if (databaseVersion == null || databaseVersion.lessThan(m.getVersion())) {
                    if (databaseVersion != null) {
                        logger.debugf("Migrating older model to %s", m.getVersion());
                    }
                    m.migrate(session);
                }
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

    private static final Map<Pattern, ModelVersion> PATTERN_MATCHER = new LinkedHashMap<>();
    static {
        PATTERN_MATCHER.put(Pattern.compile("^7\\.0\\.\\d+\\.GA$"), RHSSO_VERSION_7_0_KEYCLOAK_VERSION);
        PATTERN_MATCHER.put(Pattern.compile("^7\\.1\\.\\d+\\.GA$"), RHSSO_VERSION_7_1_KEYCLOAK_VERSION);
        PATTERN_MATCHER.put(Pattern.compile("^7\\.2\\.\\d+\\.GA$"), RHSSO_VERSION_7_2_KEYCLOAK_VERSION);
        PATTERN_MATCHER.put(Pattern.compile("^7\\.3\\.\\d+\\.GA$"), RHSSO_VERSION_7_3_KEYCLOAK_VERSION);
    }

    public static void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        ModelVersion stored = null;
        if (rep.getKeycloakVersion() != null) {
            stored = convertRHSSOVersionToKeycloakVersion(rep.getKeycloakVersion());
            if (stored == null) {
                stored = new ModelVersion(rep.getKeycloakVersion());
            }
        }
        if (stored == null) {
            stored = migrations[0].getVersion();
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
        for (Pattern pattern : PATTERN_MATCHER.keySet()) {
            if (pattern.matcher(version).find()) {
                return PATTERN_MATCHER.get(pattern);
            }
        }
        // chceck if the version is in format for CD releases, e.g.: "keycloakVersion": "6"
        if (Pattern.compile("^[0-9]*$").matcher(version).find()) {
            return new ModelVersion(Integer.parseInt(version), 0, 0);
        }
        return null;
    }
}
