/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.jpa.liquibase.updater;

import org.keycloak.models.map.storage.jpa.liquibase.connection.MapLiquibaseConnectionProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.RanChangeSet;
import liquibase.exception.LiquibaseException;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.updater.liquibase.ThreadLocalSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.jpa.updater.MapJpaUpdaterProvider;

public class MapJpaLiquibaseUpdaterProvider implements MapJpaUpdaterProvider {

    private static final Logger logger = Logger.getLogger(MapJpaLiquibaseUpdaterProvider.class);

    private final KeycloakSession session;

    public MapJpaLiquibaseUpdaterProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void update(Class modelType, Connection connection, String defaultSchema) {
        update(modelType, connection, null, defaultSchema);
    }

    @Override
    public void export(Class modelType, Connection connection, String defaultSchema, File file) {
        update(modelType, connection, file, defaultSchema);
    }

    private void update(Class modelType, Connection connection, File file, String defaultSchema) {
        logger.debug("Starting database update");

        // Need ThreadLocal as liquibase doesn't seem to have API to inject custom objects into tasks
        ThreadLocalSessionContext.setCurrentSession(session);

        Writer exportWriter = null;
        try {
            Liquibase liquibase = getLiquibase(modelType, connection, defaultSchema);
            if (file != null) {
                exportWriter = new FileWriter(file);
            }

            updateChangeSet(liquibase);

        } catch (LiquibaseException | IOException | SQLException e) {
            logger.error("Error has occurred while updating the database", e);
            throw new RuntimeException("Failed to update database", e);
        } finally {
            ThreadLocalSessionContext.removeCurrentSession();
            if (exportWriter != null) {
                try {
                    exportWriter.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
    }

    protected void updateChangeSet(Liquibase liquibase) throws LiquibaseException, SQLException {
        String changelog = liquibase.getChangeLogFile();
        List<ChangeSet> changeSets = this.getLiquibaseUnrunChangeSets(liquibase);
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
        }

    }

    @Override
    public Status validate(Class modelType, Connection connection, String defaultSchema) {
        logger.debug("Validating if database is updated");
        ThreadLocalSessionContext.setCurrentSession(session);

        try {
            Liquibase liquibase = getLiquibase(modelType, connection, defaultSchema);

            Status status = validateChangeSet(liquibase, liquibase.getChangeLogFile());
            if (status != Status.VALID) {
                return status;
            }

        } catch (LiquibaseException e) {
            throw new RuntimeException("Failed to validate database", e);
        }

        return Status.VALID;
    }

    protected Status validateChangeSet(Liquibase liquibase, String changelog) throws LiquibaseException {
        final Status result;
        List<ChangeSet> changeSets = this.getLiquibaseUnrunChangeSets(liquibase);

        if (!changeSets.isEmpty()) {
            if (changeSets.size() == liquibase.getDatabaseChangeLog().getChangeSets().size()) {
                result = Status.EMPTY;
            } else {
                logger.debugf("Validation failed. Database is not up-to-date for changelog %s", changelog);
                result = Status.OUTDATED;
            }
        } else {
            logger.debugf("Validation passed. Database is up-to-date for changelog %s", changelog);
            result = Status.VALID;
        }

        return result;
    }

    private List<ChangeSet> getLiquibaseUnrunChangeSets(Liquibase liquibase) throws LiquibaseException {
        return liquibase.listUnrunChangeSets(null, new LabelExpression(), false);
    }

    private Liquibase getLiquibase(Class modelType, Connection connection, String defaultSchema) throws LiquibaseException {
        MapLiquibaseConnectionProvider liquibaseProvider = session.getProvider(MapLiquibaseConnectionProvider.class);
        String modelName = ModelEntityUtil.getModelName(modelType);
        if (modelName == null) {
            throw new IllegalStateException("Cannot find changlelog for modelClass " + modelType.getName());
        }
        String changelog = "META-INF/jpa-" + modelName + "-changelog.xml";
        return liquibaseProvider.getLiquibaseForCustomUpdate(connection, defaultSchema, changelog, this.getClass().getClassLoader(), "databasechangelog");
    }

    @Override
    public void close() {
    }

    public static String getTable(String table, String defaultSchema) {
        return defaultSchema != null ? defaultSchema + "." + table : table;
    }

}
