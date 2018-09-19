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

package org.keycloak.connections.jpa.updater.liquibase.custom;

import liquibase.change.custom.CustomSqlChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Table;
import org.keycloak.connections.jpa.updater.liquibase.LiquibaseJpaUpdaterProvider;
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
            ArrayList<SqlStatement> statements = new ArrayList<SqlStatement>();

            String correctedTableName = database.correctObjectName("REALM", Table.class);
            String correctedSchemaName = database.escapeObjectName(database.getDefaultSchemaName(), Schema.class);

            if (SnapshotGeneratorFactory.getInstance().has(new Table().setName(correctedTableName), database)) {
                ResultSet resultSet = connection.createStatement().executeQuery("SELECT ID FROM " + LiquibaseJpaUpdaterProvider.getTable(correctedTableName, correctedSchemaName) + " WHERE CODE_SECRET IS NULL");
                while (resultSet.next()) {
                    String id = resultSet.getString(1);

                    UpdateStatement statement = new UpdateStatement(null, null, correctedTableName)
                            .addNewColumnValue("CODE_SECRET", KeycloakModelUtils.generateCodeSecret())
                            .setWhereClause("ID=?").addWhereParameters(id);
                    statements.add(statement);

                    if (!resultSet.isFirst()) {
                        sb.append(", ");
                    }
                    sb.append(id);
                }

                if (!statements.isEmpty()) {
                    confirmationMessage = sb.toString();
                }
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
