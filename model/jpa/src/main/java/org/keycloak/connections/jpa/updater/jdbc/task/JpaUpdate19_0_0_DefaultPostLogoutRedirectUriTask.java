/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.connections.jpa.updater.jdbc.task;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.NativeSqlHelper;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate19_0_0_DefaultPostLogoutRedirectUri}.
 */
public class JpaUpdate19_0_0_DefaultPostLogoutRedirectUriTask implements JpaMigrationTask {

    private static final String POST_LOGOUT_REDIRECT_URIS = "post.logout.redirect.uris";

    @Override
    public String getTaskId() {
        return "Default post_logout_redirect_uris (19.0.0)";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        String clientAttributesTable = NativeSqlHelper.qualified("CLIENT_ATTRIBUTES", defaultSchema);
        String redirectUrisTable = NativeSqlHelper.qualified("REDIRECT_URIS", defaultSchema);
        return List.of(
                "INSERT INTO " + clientAttributesTable + " (CLIENT_ID, NAME, VALUE) "
                        + "SELECT DISTINCT CLIENT_ID, " + NativeSqlHelper.sqlLiteral(POST_LOGOUT_REDIRECT_URIS) + ", '+' "
                        + "FROM " + redirectUrisTable + " WHERE CLIENT_ID NOT IN "
                        + "(SELECT CLIENT_ID FROM " + clientAttributesTable
                        + " WHERE NAME = " + NativeSqlHelper.sqlLiteral(POST_LOGOUT_REDIRECT_URIS) + ")"
        );
    }
}
