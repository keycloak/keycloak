package org.keycloak.migration.jpa;

import liquibase.change.AddColumnConfig;
import liquibase.change.custom.CustomSqlChange;
import liquibase.database.Database;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.core.MySQLDatabase;
import liquibase.database.core.OracleDatabase;
import liquibase.database.core.PostgresDatabase;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.CreateIndexStatement;
import liquibase.statement.core.RawSqlStatement;
import liquibase.structure.core.Column;
import liquibase.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

public class LazyCreateIndexCustomChange implements CustomSqlChange {

    private String catalogName;
    private String schemaName;
    private String tableName;
    private String indexName;
    private List<AddColumnConfig> columns = Collections.emptyList();

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public SqlStatement[] generateStatements(Database database) {
        final String escapedTableName = database.escapeTableName(getCatalogName(), getSchemaName(), getTableName());
        final String escapedIndexName = database.escapeIndexName(getCatalogName(), getSchemaName(), getIndexName());
        final String escapedColumnNames = joinEscapedColumnNames(database);

        final String rawQuery;
        if (database instanceof PostgresDatabase) {
            rawQuery = "CREATE INDEX " + escapedIndexName +
                    " ON " + escapedTableName +
                    " (" + escapedColumnNames + ") CONCURRENTLY";
        } else if (database instanceof OracleDatabase) {
            rawQuery = "CREATE INDEX " + escapedIndexName +
                    " ON " + escapedTableName +
                    " (" + escapedColumnNames + ") ONLINE";
        } else if (database instanceof MySQLDatabase) {
            rawQuery = "ALTER TABLE " + escapedTableName +
                    " ADD INDEX " + escapedIndexName +
                    " (" + escapedColumnNames + ")" +
                    ", LOCK=NONE;";
        } else if (database instanceof MSSQLDatabase) {
            rawQuery = "CREATE NONCLUSTERED INDEX " + escapedIndexName +
                    " ON " + escapedTableName + " (" + escapedColumnNames + ")" +
                    " REBUILD WITH (ONLINE = ON)";
        } else {
            return new CreateIndexStatement[]{new CreateIndexStatement(
                    getIndexName(),
                    getCatalogName(),
                    getSchemaName(),
                    getTableName(),
                    false,
                    null,
                    getColumns().toArray(new AddColumnConfig[0])
            )};
        }

        return new SqlStatement[]{new RawSqlStatement(rawQuery)};
    }

    private String joinEscapedColumnNames(Database database) {
        StringJoiner joiner = new StringJoiner(", ");
        for (AddColumnConfig v : getColumns()) {
            joiner.add(database.escapeColumnName(getCatalogName(), getSchemaName(), getTableName(), v.getName()));
        }
        return joiner.toString();
    }

    @Override
    public String getConfirmationMessage() {
        return null;
    }

    @Override
    public void setUp() throws SetupException {

    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {

    }

    @Override
    public ValidationErrors validate(Database database) {
        ValidationErrors changeValidationErrors = new ValidationErrors();

        if (StringUtils.trimToNull(getTableName()) == null) {
            changeValidationErrors.addError("Property 'tableName' is required for " + LazyCreateIndexCustomChange.class.getSimpleName() + " on " + database.getShortName());
        }
        if (StringUtils.trimToNull(getIndexName()) == null) {
            changeValidationErrors.addError("Property 'indexName' is required for " + LazyCreateIndexCustomChange.class.getSimpleName() + " on " + database.getShortName());
        }
        if (getColumns().isEmpty()) {
            changeValidationErrors.addError("Property 'columns' is required for " + LazyCreateIndexCustomChange.class.getSimpleName() + " on " + database.getShortName());
        }

        if (changeValidationErrors.hasErrors()) {
            return changeValidationErrors;
        } else {
            return null;
        }
    }

    public String getCatalogName() {
        return catalogName;
    }

    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

    public List<AddColumnConfig> getColumns() {
        return columns;
    }

    public void setColumns(String columns) {
        if (StringUtils.trimToNull(columns) == null) {
            this.columns = Collections.emptyList();
        } else {
            final List<AddColumnConfig> list = new ArrayList<>();
            for (String column : columns.split(",")) {
                list.add(new AddColumnConfig(Column.fromName(column.trim())));
            }
            this.columns = list;
        }
    }

}
