package org.keycloak.connections.jpa.updater.liquibase.custom;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.InsertStatement;
import liquibase.structure.core.Table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author mhajas
 */
public class AuthzResourceUseMoreURIs extends CustomKeycloakTask {
    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        try {
            PreparedStatement statement = jdbcConnection.prepareStatement("select ID,URI from " + getTableName("RESOURCE_SERVER_RESOURCE") + " where URI is not null");

            try {
                ResultSet resultSet = statement.executeQuery();
                try {
                    while (resultSet.next()) {
                        String resourceId = resultSet.getString(1);
                        String resourceUri = resultSet.getString(2);

                        InsertStatement insertComponent = new InsertStatement(null, null, database.correctObjectName("RESOURCE_URIS", Table.class))
                                .addColumnValue("RESOURCE_ID", resourceId)
                                .addColumnValue("VALUE", resourceUri);

                        statements.add(insertComponent);
                    }
                } finally {
                    resultSet.close();
                }
            } finally {
                statement.close();
            }

            confirmationMessage.append("Moved " + statements.size() + " records from RESOURCE_SERVER_RESOURCE to RESOURCE_URIS table");
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when updating data from previous version", e);
        }
    }

    @Override
    protected String getTaskId() {
        return "Update 4.2.0.Final";
    }
}
