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
import org.keycloak.connections.jpa.updater.JpaUpdaterProvider;
import org.keycloak.connections.jpa.updater.liquibase.conn.LiquibaseConnectionProvider;
import org.keycloak.models.KeycloakSession;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

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
    public String getCurrentVersionSql(String defaultSchema) {
        return "SELECT ID from " + getTable("DATABASECHANGELOG", defaultSchema) + " ORDER BY DATEEXECUTED DESC LIMIT 1";
    }

    @Override
    public void update(Connection connection, String defaultSchema) {
        logger.debug("Starting database update");

        // Need ThreadLocal as liquibase doesn't seem to have API to inject custom objects into tasks
        ThreadLocalSessionContext.setCurrentSession(session);

        try {
            Liquibase liquibase = getLiquibase(connection, defaultSchema);

            List<ChangeSet> changeSets = liquibase.listUnrunChangeSets((Contexts) null);
            if (!changeSets.isEmpty()) {
                if (changeSets.get(0).getId().equals(FIRST_VERSION)) {
                    Statement statement = connection.createStatement();
                    try {
                        statement.executeQuery("SELECT id FROM " + getTable("REALM", defaultSchema));

                        logger.infov("Updating database from {0} to {1}", FIRST_VERSION, changeSets.get(changeSets.size() - 1).getId());
                        liquibase.markNextChangeSetRan(null);
                    } catch (SQLException e) {
                        logger.info("Initializing database schema");
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        List<RanChangeSet> ranChangeSets = liquibase.getDatabase().getRanChangeSetList();
                        logger.debugv("Updating database from {0} to {1}", ranChangeSets.get(ranChangeSets.size() - 1).getId(), changeSets.get(changeSets.size() - 1).getId());
                    } else {
                        logger.infov("Updating database");
                    }
                }

                liquibase.update((Contexts) null);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to update database", e);
        } finally {
            ThreadLocalSessionContext.removeCurrentSession();
        }

        logger.debug("Completed database update");
    }

    @Override
    public void validate(Connection connection, String defaultSchema) {
        logger.debug("Validating if database is updated");

        try {
            Liquibase liquibase = getLiquibase(connection, defaultSchema);

            List<ChangeSet> changeSets = liquibase.listUnrunChangeSets((Contexts) null);
            if (!changeSets.isEmpty()) {
                List<RanChangeSet> ranChangeSets = liquibase.getDatabase().getRanChangeSetList();
                String errorMessage = String.format("Failed to validate database schema. Schema needs updating database from %s to %s. Please change databaseSchema to 'update' or use other database",
                        ranChangeSets.get(ranChangeSets.size() - 1).getId(), changeSets.get(changeSets.size() - 1).getId());
                throw new RuntimeException(errorMessage);
            } else {
                logger.debug("Validation passed. Database is up-to-date");
            }

        } catch (LiquibaseException e) {
            throw new RuntimeException("Failed to validate database", e);
        }
    }

    private Liquibase getLiquibase(Connection connection, String defaultSchema) throws LiquibaseException {
        LiquibaseConnectionProvider liquibaseProvider = session.getProvider(LiquibaseConnectionProvider.class);
        return liquibaseProvider.getLiquibase(connection, defaultSchema);
    }

    @Override
    public void close() {
    }

    public static String getTable(String table, String defaultSchema) {
        return defaultSchema != null ? defaultSchema + "." + table : table;
    }

}
