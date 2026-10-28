package org.keycloak.schema.generator;

import liquibase.change.custom.CustomSqlChange;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.SqlStatement;

public class DummyCustomChange implements CustomSqlChange, CustomTaskChange {

    @Override
    public SqlStatement[] generateStatements(Database database) throws CustomChangeException {
        return new SqlStatement[0];
    }

    @Override
    public void execute(Database database) throws CustomChangeException {
    }

    @Override
    public String getConfirmationMessage() {
        return "Stubbed custom change";
    }

    @Override
    public void setUp() throws SetupException {
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }
}
