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

import org.jboss.logging.Logger;
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
            new MigrateTo6_0_0()
    };

    public static void migrate(KeycloakSession session) {
        ModelVersion latest = migrations[migrations.length-1].getVersion();
        MigrationModel model = session.realms().getMigrationModel();
        ModelVersion stored = null;
        if (model.getStoredVersion() != null) {
            stored = new ModelVersion(model.getStoredVersion());
            if (latest.equals(stored)) {
                return;
            }
        }

        for (Migration m : migrations) {
            if (stored == null || stored.lessThan(m.getVersion())) {
                if (stored != null) {
                    logger.debugf("Migrating older model to %s", m.getVersion());
                }
                m.migrate(session);
            }
        }

        model.setStoredVersion(latest.toString());
    }

    public static final ModelVersion RHSSO_VERSION_7_0_KEYCLOAK_VERSION = new ModelVersion("1.9.8");
    public static final ModelVersion RHSSO_VERSION_7_1_KEYCLOAK_VERSION = new ModelVersion("2.5.0");
    public static final ModelVersion RHSSO_VERSION_7_2_KEYCLOAK_VERSION = new ModelVersion("3.4.2");


    public static void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        ModelVersion latest = migrations[migrations.length-1].getVersion();
        ModelVersion stored = migrations[0].getVersion();
        if (rep.getKeycloakVersion() != null) {
            stored = new ModelVersion(rep.getKeycloakVersion());
            // hack for importing RH-SSO json export
            // NOTE!!!!! We need to do something once we reach community version 7.  If community version is 7 or higher, look for the GA qualifier to identify it as RH SSO
            if (latest.getMajor() < 7 || (stored.getMajor() == 7 && stored.getQualifier().equals("GA"))) {
                if (stored.getMajor() == 7) {
                    if (stored.getMinor() == 0) {
                        stored = RHSSO_VERSION_7_0_KEYCLOAK_VERSION;
                    } else if (stored.getMinor() == 1) {
                        stored = RHSSO_VERSION_7_1_KEYCLOAK_VERSION;
                    } else if (stored.getMinor() == 2) {
                        stored = RHSSO_VERSION_7_2_KEYCLOAK_VERSION;
                    }
                }
            }
            // strip out qualifier
            stored = new ModelVersion(stored.major, stored.minor, stored.micro);
            if (latest.equals(stored) || latest.lessThan(stored)) {
                return;
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
}
