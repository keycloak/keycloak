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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.NativeSqlHelper;
import org.keycloak.models.KeycloakSession;

/**
 * Migration task registered for the historic manifest class name {@code AuthzResourceUseMoreURIs}.
 * Reads RESOURCE_SERVER_RESOURCE and INSERTs into RESOURCE_URIS.
 */
public class AuthzResourceUseMoreURIsTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Move URI to RESOURCE_URIS table";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema)
            throws SQLException {
        String resourceTable = NativeSqlHelper.qualified("RESOURCE_SERVER_RESOURCE", defaultSchema);
        String urisTable = NativeSqlHelper.qualified("RESOURCE_URIS", defaultSchema);
        List<String> stmts = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT ID, URI FROM " + resourceTable + " WHERE URI IS NOT NULL");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String resourceId = rs.getString(1);
                String uri = rs.getString(2);
                stmts.add("INSERT INTO " + urisTable + " (RESOURCE_ID, VALUE) VALUES ("
                        + NativeSqlHelper.sqlLiteral(resourceId) + ", "
                        + NativeSqlHelper.sqlLiteral(uri) + ")");
            }
        }
        return stmts;
    }
}
