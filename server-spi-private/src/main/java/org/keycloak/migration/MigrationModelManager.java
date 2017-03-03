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
import org.keycloak.migration.migrators.Migration;
import org.keycloak.models.KeycloakSession;

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
            new MigrateTo3_0_0()
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
}
