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

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import liquibase.datatype.DataTypeFactory;
import liquibase.exception.CustomChangeException;
import liquibase.statement.core.InsertStatement;
import liquibase.structure.core.Table;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JpaUpdate1_2_0_CR1 extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String realmClientTableName = database.correctObjectName("REALM_CLIENT", Table.class);

        try {
            String trueValue = DataTypeFactory.getInstance().getTrueBooleanValue(database);
            PreparedStatement statement = jdbcConnection.prepareStatement("select CLIENT.REALM_ID, CLIENT.ID CLIENT_ID from " + getTableName("CLIENT") + " CLIENT where CLIENT.CONSENT_REQUIRED = " + trueValue);

            try {
                ResultSet resultSet = statement.executeQuery();
                try {
                    while (resultSet.next()) {
                        String realmId = resultSet.getString("REALM_ID");
                        String oauthClientId = resultSet.getString("CLIENT_ID");

                        InsertStatement realmClientInsert = new InsertStatement(null, null, realmClientTableName)
                                .addColumnValue("REALM_ID", realmId)
                                .addColumnValue("CLIENT_ID", oauthClientId);
                        statements.add(realmClientInsert);
                    }
                } finally {
                    resultSet.close();
                }
            } finally {
                statement.close();
            }

            confirmationMessage.append("Inserted " + statements.size() + " OAuth Clients to REALM_CLIENT table");
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when updating data from previous version", e);
        }
    }

    @Override
    protected String getTaskId() {
        return "Update 1.2.0.CR1";
    }
}
