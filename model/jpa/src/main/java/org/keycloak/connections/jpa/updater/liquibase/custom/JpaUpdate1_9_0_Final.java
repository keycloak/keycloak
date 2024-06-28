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

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.core.Table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JpaUpdate1_9_0_Final extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String userFederationProviderTableName = database.correctObjectName("USER_FEDERATION_PROVIDER", Table.class);

        try {
            PreparedStatement statement = jdbcConnection.prepareStatement("select REALM_ID, USERFEDERATIONPROVIDERS_ID from " + getTableName("FED_PROVIDERS"));

            try {
                ResultSet resultSet = statement.executeQuery();
                try {
                    while (resultSet.next()) {
                        String realmId = resultSet.getString(1);
                        String userFederationProviderId = resultSet.getString(2);

                        UpdateStatement updateStatement = new UpdateStatement(null, null, userFederationProviderTableName)
                                .addNewColumnValue("REALM_ID", realmId)
                                .setWhereClause("ID='" + userFederationProviderId + "'");
                        statements.add(updateStatement);
                    }
                } finally {
                    resultSet.close();
                }
            } finally {
                statement.close();
            }

            confirmationMessage.append("Updated " + statements.size() + " records in USER_FEDERATION_PROVIDER table");
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when updating data from previous version", e);
        }
    }

    @Override
    protected String getTaskId() {
        return "Update 1.9.0.Final";
    }
}
