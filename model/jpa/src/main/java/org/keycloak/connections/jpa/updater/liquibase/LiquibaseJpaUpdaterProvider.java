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

package org.keycloak.connections.jpa.updater.liquibase;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.RanChangeSet;
import liquibase.exception.LiquibaseException;
import org.jboss.logging.Logger;
import org.keycloak.common.util.reflections.Reflections;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.updater.JpaUpdaterProvider;
import org.keycloak.connections.jpa.updater.liquibase.conn.LiquibaseConnectionProvider;
import org.keycloak.connections.jpa.util.JpaUtils;
import org.keycloak.models.KeycloakSession;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LiquibaseJpaUpdaterProvider implements JpaUpdaterProvider {

    private static final Logger logger = Logger.getLogger(LiquibaseJpaUpdaterProvider.class);

    public static final String CHANGELOG = "META-INF/jpa-changelog-master.xml";
    public static final String DB2_CHANGELOG = "META-INF/db2-jpa-changelog-master.xml";

    private final KeycloakSession session;

    public LiquibaseJpaUpdaterProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void update(Connection connection, String defaultSchema) {
        update(connection, null, defaultSchema);
    }

    @Override
    public void export(Connection connection, String defaultSchema, File file) {
        update(connection, file, defaultSchema);
    }

    private void update(Connection connection, File file, String defaultSchema) {
        logger.debug("Starting database update");

        // Need ThreadLocal as liquibase doesn't seem to have API to inject custom objects into tasks
        ThreadLocalSessionContext.setCurrentSession(session);

        try {
            // Run update with keycloak master changelog first
            Liquibase liquibase = getLiquibaseForKeycloakUpdate(connection, defaultSchema);
            updateChangeSet(liquibase, liquibase.getChangeLogFile(), file);

            // Run update for each custom JpaEntityProvider
            Set<JpaEntityProvider> jpaProviders = session.getAllProviders(JpaEntityProvider.class);
            for (JpaEntityProvider jpaProvider : jpaProviders) {
                String customChangelog = jpaProvider.getChangelogLocation();
                if (customChangelog != null) {
                    String factoryId = jpaProvider.getFactoryId();
                    String changelogTableName = JpaUtils.getCustomChangelogTableName(factoryId);
                    liquibase = getLiquibaseForCustomProviderUpdate(connection, defaultSchema, customChangelog, jpaProvider.getClass().getClassLoader(), changelogTableName);
                    updateChangeSet(liquibase, liquibase.getChangeLogFile(), file);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to update database", e);
        } finally {
            ThreadLocalSessionContext.removeCurrentSession();
        }
    }


    protected void updateChangeSet(Liquibase liquibase, String changelog, File exportFile) throws LiquibaseException, IOException {
        List<ChangeSet> changeSets = liquibase.listUnrunChangeSets((Contexts) null);
        if (!changeSets.isEmpty()) {
            List<RanChangeSet> ranChangeSets = liquibase.getDatabase().getRanChangeSetList();
            if (ranChangeSets.isEmpty()) {
                logger.infov("Initializing database schema. Using changelog {0}", changelog);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debugv("Updating database from {0} to {1}. Using changelog {2}", ranChangeSets.get(ranChangeSets.size() - 1).getId(), changeSets.get(changeSets.size() - 1).getId(), changelog);
                } else {
                    logger.infov("Updating database. Using changelog {0}", changelog);
                }
            }

            if (exportFile != null) {
                liquibase.update((Contexts) null, new FileWriter(exportFile));
            } else {
                liquibase.update((Contexts) null);
            }

            logger.debugv("Completed database update for changelog {0}", changelog);
        } else {
            logger.debugv("Database is up to date for changelog {0}", changelog);

            // Needs to restart liquibase services to clear changeLogHistory.
            Method resetServices = Reflections.findDeclaredMethod(Liquibase.class, "resetServices");
            Reflections.invokeMethod(true, resetServices, liquibase);
        }
    }

    @Override
    public Status validate(Connection connection, String defaultSchema) {
        logger.debug("Validating if database is updated");
        ThreadLocalSessionContext.setCurrentSession(session);

        try {
            // Validate with keycloak master changelog first
            Liquibase liquibase = getLiquibaseForKeycloakUpdate(connection, defaultSchema);

            Status status = validateChangeSet(liquibase, liquibase.getChangeLogFile());
            if (status != Status.VALID) {
                return status;
            }

            // Validate each custom JpaEntityProvider
            Set<JpaEntityProvider> jpaProviders = session.getAllProviders(JpaEntityProvider.class);
            for (JpaEntityProvider jpaProvider : jpaProviders) {
                String customChangelog = jpaProvider.getChangelogLocation();
                if (customChangelog != null) {
                    String factoryId = jpaProvider.getFactoryId();
                    String changelogTableName = JpaUtils.getCustomChangelogTableName(factoryId);
                    liquibase = getLiquibaseForCustomProviderUpdate(connection, defaultSchema, customChangelog, jpaProvider.getClass().getClassLoader(), changelogTableName);
                    if (validateChangeSet(liquibase, liquibase.getChangeLogFile()) != Status.VALID) {
                        return Status.OUTDATED;
                    }
                }
            }
        } catch (LiquibaseException e) {
            throw new RuntimeException("Failed to validate database", e);
        }

        return Status.VALID;
    }

    protected Status validateChangeSet(Liquibase liquibase, String changelog) throws LiquibaseException {
        List<ChangeSet> changeSets = liquibase.listUnrunChangeSets((Contexts) null);
        if (!changeSets.isEmpty()) {
            if (changeSets.size() == liquibase.getDatabaseChangeLog().getChangeSets().size()) {
                return Status.EMPTY;
            } else {
                logger.debugf("Validation failed. Database is not up-to-date for changelog %s", changelog);
                return Status.OUTDATED;
            }
        } else {
            logger.debugf("Validation passed. Database is up-to-date for changelog %s", changelog);
            return Status.VALID;
        }
    }

    private Liquibase getLiquibaseForKeycloakUpdate(Connection connection, String defaultSchema) throws LiquibaseException {
        LiquibaseConnectionProvider liquibaseProvider = session.getProvider(LiquibaseConnectionProvider.class);
        return liquibaseProvider.getLiquibase(connection, defaultSchema);
    }

    private Liquibase getLiquibaseForCustomProviderUpdate(Connection connection, String defaultSchema, String changelogLocation, ClassLoader classloader, String changelogTableName) throws LiquibaseException {
        LiquibaseConnectionProvider liquibaseProvider = session.getProvider(LiquibaseConnectionProvider.class);
        return liquibaseProvider.getLiquibaseForCustomUpdate(connection, defaultSchema, changelogLocation, classloader, changelogTableName);
    }

    @Override
    public void close() {
    }

    public static String getTable(String table, String defaultSchema) {
        return defaultSchema != null ? defaultSchema + "." + table : table;
    }

}
