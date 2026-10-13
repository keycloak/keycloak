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

import org.keycloak.keys.KeyProvider;
import org.keycloak.models.utils.KeycloakModelUtils;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.InsertStatement;
import liquibase.structure.core.Table;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExtractRealmKeysFromRealmTable extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        try {
            PreparedStatement statement = jdbcConnection.prepareStatement("select ID, PRIVATE_KEY, CERTIFICATE from " + getTableName("REALM"));

            try {
                ResultSet resultSet = statement.executeQuery();
                try {
                    while (resultSet.next()) {
                        String realmId = resultSet.getString(1);
                        String privateKeyPem = resultSet.getString(2);
                        String certificatePem = resultSet.getString(3);

                        String componentId = KeycloakModelUtils.generateId();

                        InsertStatement insertComponent = new InsertStatement(null, null, database.correctObjectName("COMPONENT", Table.class))
                                .addColumnValue("ID", componentId)
                                .addColumnValue("REALM_ID", realmId)
                                .addColumnValue("PARENT_ID", realmId)
                                .addColumnValue("NAME", "rsa")
                                .addColumnValue("PROVIDER_ID", "rsa")
                                .addColumnValue("PROVIDER_TYPE", KeyProvider.class.getName());

                        statements.add(insertComponent);

                        statements.add(componentConfigStatement(componentId, "priority", "100"));
                        statements.add(componentConfigStatement(componentId, "privateKey", privateKeyPem));
                        statements.add(componentConfigStatement(componentId, "certificate", certificatePem));
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

    private InsertStatement componentConfigStatement(String componentId, String name, String value) {
        return new InsertStatement(null, null, database.correctObjectName("COMPONENT_CONFIG", Table.class))
                .addColumnValue("ID", KeycloakModelUtils.generateId())
                .addColumnValue("COMPONENT_ID", componentId)
                .addColumnValue("NAME", name)
                .addColumnValue("VALUE", value);
    }

    @Override
    protected String getTaskId() {
        return "Update 2.3.0.Final";
    }
}
