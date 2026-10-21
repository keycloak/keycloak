/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.InsertStatement;
import liquibase.structure.core.Table;

/**
 *
 * @author hmlnarik
 */
public class JpaUpdate4_0_0_DefaultClientScopes extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String clientTableName = database.correctObjectName("CLIENT", Table.class);
        String clientScopeClientTableName = database.correctObjectName("CLIENT_SCOPE_CLIENT", Table.class);

        try (PreparedStatement statement = jdbcConnection.prepareStatement("SELECT ID, CLIENT_TEMPLATE_ID FROM " + clientTableName);
          ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String clientId = rs.getString(1);
                String clientTemplateId = rs.getString(2);

                if (clientId == null || clientId.trim().isEmpty()) {
                    continue;
                }
                if (clientTemplateId == null || clientTemplateId.trim().isEmpty()) {
                    continue;
                }

                statements.add(
                  new InsertStatement(null, null, clientScopeClientTableName)
                    .addColumnValue("CLIENT_ID", clientId.trim())
                    .addColumnValue("SCOPE_ID", clientTemplateId.trim())
                    .addColumnValue("DEFAULT_SCOPE", Boolean.TRUE)
                );
            }

            confirmationMessage.append("Updated " + statements.size() + " records in CLIENT_SCOPE_CLIENT table");
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when updating data from previous version", e);
        }
    }

    @Override
    protected String getTaskId() {
        return "Update 4.0.0.Final (Default client scopes)";
    }

}
