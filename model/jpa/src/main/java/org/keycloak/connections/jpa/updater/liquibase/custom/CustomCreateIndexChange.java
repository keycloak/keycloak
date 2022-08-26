/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.connections.jpa.updater.liquibase.custom;

import java.io.StringWriter;

import liquibase.Scope;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.updater.liquibase.LiquibaseConstants;
import org.keycloak.connections.jpa.updater.liquibase.LiquibaseJpaUpdaterProvider;
import org.keycloak.connections.jpa.updater.liquibase.conn.DefaultLiquibaseConnectionProvider;

import liquibase.change.AddColumnConfig;
import liquibase.change.ChangeFactory;
import liquibase.change.ChangeMetaData;
import liquibase.change.ChangeParameterMetaData;
import liquibase.change.DatabaseChange;
import liquibase.change.core.CreateIndexChange;
import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.exception.ValidationErrors;
import liquibase.exception.Warnings;
import liquibase.executor.ExecutorService;
import liquibase.executor.LoggingExecutor;
import liquibase.snapshot.InvalidExampleException;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.CreateIndexStatement;
import liquibase.statement.core.RawSqlStatement;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Table;

/**
 * @author <a href="mailto:yoshiyuki.tabata.jy@hitachi.com">Yoshiyuki Tabata</a>
 */
@DatabaseChange(name = "createIndex", description = "Creates an index on an existing column or set of columns conditionally based on the number of records.", priority = ChangeMetaData.PRIORITY_DEFAULT
    + 1, appliesTo = "index")
public class CustomCreateIndexChange extends CreateIndexChange {
    private static final Logger logger = Logger.getLogger(CustomCreateIndexChange.class);
    private int indexCreationThreshold;

    @Override
    public SqlStatement[] generateStatements(Database database) {
        // This check is for manual migration
        if (Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor(LiquibaseConstants.JDBC_EXECUTOR, database) instanceof LoggingExecutor)
            return super.generateStatements(database);

        Object indexCreationThreshold = ((AbstractJdbcDatabase) database)
            .get(DefaultLiquibaseConnectionProvider.INDEX_CREATION_THRESHOLD_PARAM);

        if (indexCreationThreshold instanceof Integer) {
            this.indexCreationThreshold = (Integer) indexCreationThreshold;
            if (this.indexCreationThreshold <= 0)
                return super.generateStatements(database);
        } else {
            return super.generateStatements(database);
        }
        try {
            // To check that the table already exists or not on which the index will be created.
            if (!SnapshotGeneratorFactory.getInstance()
                .has(new Table().setName(getTableName()).setSchema(new Schema(getCatalogName(), getSchemaName())), database))
                return super.generateStatements(database);

            int result = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor(LiquibaseConstants.JDBC_EXECUTOR, database)
                    .queryForInt(new RawSqlStatement("SELECT COUNT(*) FROM " + getTableNameForSqlSelects(database, getTableName())));

            if (result > this.indexCreationThreshold) {
                String loggingString = createLoggingString(database);
                logger.warnv("Following index should be created: {0}", loggingString);
                getChangeSet().setComments(loggingString);
                return new SqlStatement[] {};
            }

        } catch (DatabaseException | InvalidExampleException e) {
            throw new UnexpectedLiquibaseException("Database error while index threshold validation.", e);
        }

        return super.generateStatements(database);
    }

    private String getTableNameForSqlSelects(Database database, String tableName) {
        String correctedSchemaName = database.escapeObjectName(database.getDefaultSchemaName(), Schema.class);
        return LiquibaseJpaUpdaterProvider.getTable(tableName, correctedSchemaName);
    }

    private String createLoggingString(Database database) throws DatabaseException {
        StringWriter writer = new StringWriter();
        LoggingExecutor loggingExecutor = new LoggingExecutor(Scope.getCurrentScope().getSingleton(ExecutorService.class)
                .getExecutor(LiquibaseConstants.JDBC_EXECUTOR, database), writer, database);
        SqlStatement sqlStatement = new CreateIndexStatement(getIndexName(), getCatalogName(), getSchemaName(), getTableName(),
            this.isUnique(), getAssociatedWith(), getColumns().toArray(new AddColumnConfig[0]))
                .setTablespace(getTablespace()).setClustered(getClustered());

        loggingExecutor.execute(sqlStatement);

        return writer.toString();
    }

    @Override
    public boolean generateStatementsVolatile(Database database) {
        SqlStatement[] statements = super.generateStatements(database);
        if (statements == null) {
            return false;
        }
        for (SqlStatement statement : statements) {
            if (SqlGeneratorFactory.getInstance().generateStatementsVolatile(statement, database)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Warnings warn(Database database) {
        Warnings warnings = new Warnings();
        if (generateStatementsVolatile(database)) {
            return warnings;
        }

        SqlStatement[] statements = super.generateStatements(database);
        if (statements == null) {
            return warnings;
        }
        for (SqlStatement statement : statements) {
            if (SqlGeneratorFactory.getInstance().supports(statement, database)) {
                warnings.addAll(SqlGeneratorFactory.getInstance().warn(statement, database));
            } else if (statement.skipOnUnsupported()) {
                warnings.addWarning(statement.getClass().getName() + " is not supported on " + database.getShortName() + ", but "
                        + Scope.getCurrentScope().getSingleton(ChangeFactory.class).getChangeMetaData(this).getName() + " will still execute");
            }
        }

        return warnings;
    }

    @Override
    public ValidationErrors validate(Database database) {
        ValidationErrors changeValidationErrors = new ValidationErrors();

        ChangeFactory changeFactory = Scope.getCurrentScope().getSingleton(ChangeFactory.class);
        for (ChangeParameterMetaData param : changeFactory.getChangeMetaData(this).getParameters().values()) {
            if (param.isRequiredFor(database) && param.getCurrentValue(this) == null) {
                changeValidationErrors.addError(param.getParameterName() + " is required for "
                    + changeFactory.getChangeMetaData(this).getName() + " on " + database.getShortName());
            }
        }
        if (changeValidationErrors.hasErrors()) {
            return changeValidationErrors;
        }

        if (!generateStatementsVolatile(database)) {
            String unsupportedWarning = changeFactory.getChangeMetaData(this).getName() + " is not supported on "
                + database.getShortName();
            boolean sawUnsupportedError = false;

            SqlStatement[] statements = super.generateStatements(database);
            if (statements != null) {
                for (SqlStatement statement : statements) {
                    boolean supported = SqlGeneratorFactory.getInstance().supports(statement, database);
                    if (!supported && !sawUnsupportedError) {
                        if (!statement.skipOnUnsupported()) {
                            changeValidationErrors.addError(unsupportedWarning);
                            sawUnsupportedError = true;
                        }
                    } else {
                        changeValidationErrors.addAll(SqlGeneratorFactory.getInstance().validate(statement, database));
                    }
                }
            }
        }

        return changeValidationErrors;
    }

}
