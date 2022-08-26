/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
import liquibase.statement.core.InsertStatement;
import liquibase.structure.core.Table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class JpaUpdate19_0_0_DefaultPostLogoutRedirectUri extends CustomKeycloakTask {

    private static final String POST_LOGOUT_REDIRECT_URIS = "post.logout.redirect.uris";

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String sql = "SELECT DISTINCT CLIENT_ID FROM " + getTableName("REDIRECT_URIS");

        try (PreparedStatement statement = jdbcConnection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                statements.add(
                        new InsertStatement(null, null, database.correctObjectName("CLIENT_ATTRIBUTES", Table.class))
                                .addColumnValue("CLIENT_ID", rs.getString(1))
                                .addColumnValue("NAME", POST_LOGOUT_REDIRECT_URIS)
                                .addColumnValue("VALUE", "+")
                );
            }
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when extracting data from previous version", e);
        }
    }

    @Override
    protected String getTaskId() {
        return "Default post_logout_redirect_uris (19.0.0)";
    }

}
