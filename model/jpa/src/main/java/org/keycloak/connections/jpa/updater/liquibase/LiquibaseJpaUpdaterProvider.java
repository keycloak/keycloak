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
        logger.debug("Starting database update");

        // Need ThreadLocal as liquibase doesn't seem to have API to inject custom objects into tasks
        ThreadLocalSessionContext.setCurrentSession(session);

        try {
            // Run update with keycloak master changelog first
            Liquibase liquibase = getLiquibaseForKeycloakUpdate(connection, defaultSchema);
            updateChangeSet(liquibase, liquibase.getChangeLogFile());

            // Run update for each custom JpaEntityProvider
            Set<JpaEntityProvider> jpaProviders = session.getAllProviders(JpaEntityProvider.class);
            for (JpaEntityProvider jpaProvider : jpaProviders) {
                String customChangelog = jpaProvider.getChangelogLocation();
                if (customChangelog != null) {
                    String factoryId = jpaProvider.getFactoryId();
                    String changelogTableName = JpaUtils.getCustomChangelogTableName(factoryId);
                    liquibase = getLiquibaseForCustomProviderUpdate(connection, defaultSchema, customChangelog, jpaProvider.getClass().getClassLoader(), changelogTableName);
                    updateChangeSet(liquibase, liquibase.getChangeLogFile());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to update database", e);
        } finally {
            ThreadLocalSessionContext.removeCurrentSession();
        }
    }

    protected void updateChangeSet(Liquibase liquibase, String changelog) throws LiquibaseException {
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

            liquibase.update((Contexts) null);
            logger.debugv("Completed database update for changelog {0}", changelog);
        } else {
            logger.debugv("Database is up to date for changelog {0}", changelog);

            // Needs to restart liquibase services to clear changeLogHistory.
            Method resetServices = Reflections.findDeclaredMethod(Liquibase.class, "resetServices");
            Reflections.invokeMethod(true, resetServices, liquibase);
        }
    }

    @Override
    public void validate(Connection connection, String defaultSchema) {
        logger.debug("Validating if database is updated");

        try {
            // Validate with keycloak master changelog first
            Liquibase liquibase = getLiquibaseForKeycloakUpdate(connection, defaultSchema);
            validateChangeSet(liquibase, liquibase.getChangeLogFile());

            // Validate each custom JpaEntityProvider
            Set<JpaEntityProvider> jpaProviders = session.getAllProviders(JpaEntityProvider.class);
            for (JpaEntityProvider jpaProvider : jpaProviders) {
                String customChangelog = jpaProvider.getChangelogLocation();
                if (customChangelog != null) {
                    String factoryId = jpaProvider.getFactoryId();
                    String changelogTableName = JpaUtils.getCustomChangelogTableName(factoryId);
                    liquibase = getLiquibaseForCustomProviderUpdate(connection, defaultSchema, customChangelog, jpaProvider.getClass().getClassLoader(), changelogTableName);
                    validateChangeSet(liquibase, liquibase.getChangeLogFile());
                }
            }

        } catch (LiquibaseException e) {
            throw new RuntimeException("Failed to validate database", e);
        }
    }

    protected void validateChangeSet(Liquibase liquibase, String changelog) throws LiquibaseException {
        List<ChangeSet> changeSets = liquibase.listUnrunChangeSets((Contexts) null);
        if (!changeSets.isEmpty()) {
            List<RanChangeSet> ranChangeSets = liquibase.getDatabase().getRanChangeSetList();
            String errorMessage = String.format("Failed to validate database schema. Schema needs updating database from %s to %s. Please change databaseSchema to 'update' or use other database. Used changelog was %s",
                    ranChangeSets.get(ranChangeSets.size() - 1).getId(), changeSets.get(changeSets.size() - 1).getId(), changelog);
            throw new RuntimeException(errorMessage);
        } else {
            logger.debugf("Validation passed. Database is up-to-date for changelog %s", changelog);
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
