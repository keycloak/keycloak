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
import org.keycloak.migration.migrators.MigrationTo1_2_0_CR1;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MigrationModelManager {
    private static Logger logger = Logger.getLogger(MigrationModelManager.class);

    public static void migrate(KeycloakSession session) {
        MigrationModel model = session.realms().getMigrationModel();
        String storedVersion = model.getStoredVersion();
        if (MigrationModel.LATEST_VERSION.equals(storedVersion)) return;
        ModelVersion stored = null;
        if (storedVersion != null) {
            stored = new ModelVersion(storedVersion);
        }

        if (stored == null || stored.lessThan(MigrationTo1_2_0_CR1.VERSION)) {
            if (stored != null) {
                logger.debug("Migrating older model to 1.2.0.CR1 updates");
            }
            new MigrationTo1_2_0_CR1().migrate(session);
        }
        if (stored == null || stored.lessThan(MigrateTo1_3_0.VERSION)) {
            if (stored != null) {
                logger.debug("Migrating older model to 1.3.0 updates");
            }
            new MigrateTo1_3_0().migrate(session);
        }
        if (stored == null || stored.lessThan(MigrateTo1_4_0.VERSION)) {
            if (stored != null) {
                logger.debug("Migrating older model to 1.4.0 updates");
            }
            new MigrateTo1_4_0().migrate(session);
        }
        if (stored == null || stored.lessThan(MigrateTo1_5_0.VERSION)) {
            if (stored != null) {
                logger.debug("Migrating older model to 1.5.0 updates");
            }
            new MigrateTo1_5_0().migrate(session);
        }
        if (stored == null || stored.lessThan(MigrateTo1_6_0.VERSION)) {
            if (stored != null) {
                logger.debug("Migrating older model to 1.6.0 updates");
            }
            new MigrateTo1_6_0().migrate(session);
        }
        if (stored == null || stored.lessThan(MigrateTo1_7_0.VERSION)) {
            if (stored != null) {
                logger.debug("Migrating older model to 1.7.0 updates");
            }
            new MigrateTo1_7_0().migrate(session);
        }
        if (stored == null || stored.lessThan(MigrateTo1_8_0.VERSION)) {
            if (stored != null) {
                logger.debug("Migrating older model to 1.8.0 updates");
            }
            new MigrateTo1_8_0().migrate(session);
        }
        if (stored == null || stored.lessThan(MigrateTo1_9_0.VERSION)) {
            if (stored != null) {
                logger.debug("Migrating older model to 1.9.0 updates");
            }
            new MigrateTo1_9_0().migrate(session);
        }
        if (stored == null || stored.lessThan(MigrateTo1_9_2.VERSION)) {
            if (stored != null) {
                logger.debug("Migrating older model to 1.9.2 updates");
            }
            new MigrateTo1_9_2().migrate(session);
        }
        if (stored == null || stored.lessThan(MigrateTo2_0_0.VERSION)) {
            if (stored != null) {
                logger.debug("Migrating older model to 2.0.0 updates");
            }
            new MigrateTo2_0_0().migrate(session);
        }
        if (stored == null || stored.lessThan(MigrateTo2_1_0.VERSION)) {
            if (stored != null) {
                logger.debug("Migrating older model to 2.1.0 updates");
            }
            new MigrateTo2_1_0().migrate(session);
        }

        model.setStoredVersion(MigrationModel.LATEST_VERSION);
    }
}
