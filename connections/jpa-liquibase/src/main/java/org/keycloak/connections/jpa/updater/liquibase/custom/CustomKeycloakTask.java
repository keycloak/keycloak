package org.keycloak.connections.jpa.updater.liquibase.custom;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import liquibase.change.custom.CustomSqlChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.statement.SqlStatement;
import liquibase.structure.core.Table;
import org.keycloak.connections.jpa.updater.liquibase.ThreadLocalSessionContext;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class CustomKeycloakTask implements CustomSqlChange {

    protected KeycloakSession kcSession;

    protected Database database;
    protected JdbcConnection jdbcConnection;
    protected Connection connection;
    protected StringBuilder confirmationMessage = new StringBuilder();
    protected List<SqlStatement> statements = new ArrayList<SqlStatement>();

    @Override
    public ValidationErrors validate(Database database) {
        return null;
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {

    }

    @Override
    public String getConfirmationMessage() {
        return confirmationMessage.toString();
    }

    @Override
    public void setUp() throws SetupException {
        this.kcSession = ThreadLocalSessionContext.getCurrentSession();
        if (this.kcSession == null) {
            throw new SetupException("No KeycloakSession provided in ThreadLocal");
        }
    }

    @Override
    public SqlStatement[] generateStatements(Database database) throws CustomChangeException {
        this.database = database;
        jdbcConnection = (JdbcConnection) database.getConnection();
        connection = jdbcConnection.getWrappedConnection();

        if (isApplicable()) {
            confirmationMessage.append(getTaskId() + ": ");
            generateStatementsImpl();
        } else {
            confirmationMessage.append(getTaskId() + ": no update applicable for this task");
        }

        return statements.toArray(new SqlStatement[statements.size()]);
    }

    protected boolean isApplicable() throws CustomChangeException {
        try {
            String correctedTableName = database.correctObjectName("REALM", Table.class);
            if (SnapshotGeneratorFactory.getInstance().has(new Table().setName(correctedTableName), database)) {
                ResultSet resultSet = connection.createStatement().executeQuery("SELECT ID FROM REALM");
                try {
                    return (resultSet.next());
                } finally {
                    resultSet.close();
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new CustomChangeException("Failed to check database availability", e);
        }
    }

    /**
     * It's supposed to fill SQL statements to the "statements" variable and fill "confirmationMessage"
     */
    protected abstract void generateStatementsImpl() throws CustomChangeException;

    protected abstract String getTaskId();
}
