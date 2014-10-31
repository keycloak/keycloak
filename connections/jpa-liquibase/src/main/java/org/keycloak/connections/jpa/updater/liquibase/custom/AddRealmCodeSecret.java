package org.keycloak.connections.jpa.updater.liquibase.custom;

import liquibase.change.custom.CustomSqlChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.UpdateStatement;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AddRealmCodeSecret implements CustomSqlChange {

    private String confirmationMessage;

    @Override
    public SqlStatement[] generateStatements(Database database) throws CustomChangeException {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Generated codeSecret for realms: ");

            Connection connection = ((JdbcConnection) (database.getConnection())).getWrappedConnection();
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT ID FROM REALM WHERE CODE_SECRET IS NULL");

            ArrayList<SqlStatement> statements = new ArrayList<SqlStatement>();
            while (resultSet.next()) {
                String id = resultSet.getString(1);

                UpdateStatement statement = new UpdateStatement(null, null, "REALM")
                        .addNewColumnValue("CODE_SECRET", KeycloakModelUtils.generateCodeSecret())
                        .setWhereClause("ID='" + id + "'");
                statements.add(statement);

                if (!resultSet.isFirst()) {
                    sb.append(", ");
                }
                sb.append(id);
            }

            if (!statements.isEmpty()) {
                confirmationMessage = sb.toString();
            }

            return statements.toArray(new SqlStatement[statements.size()]);
        } catch (Exception e) {
            throw new CustomChangeException("Failed to add realm code secret", e);
        }
    }

    @Override
    public String getConfirmationMessage() {
        return confirmationMessage;
    }

    @Override
    public void setUp() throws SetupException {

    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {

    }

    @Override
    public ValidationErrors validate(Database database) {
        return null;
    }

}
