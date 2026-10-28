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
import java.util.HexFormat;
import java.util.List;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.NativeSqlHelper;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.jpa.JpaHashUtils;

/**
 * Migration task registered for the historic manifest class name {@code FederatedUserAttributeTextColumnMigration}.
 * MySQL/MariaDB only: migrates long FED_USER_ATTRIBUTE values to the LONG_VALUE column.
 *
 * <p>The hash columns are {@code VARBINARY} / {@code LONGBLOB} (MySQL/MariaDB), so binary values are
 * embedded using the {@code 0x…} hex literal syntax which MySQL and MariaDB both support.
 */
public class FederatedUserAttributeTextColumnMigrationTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Leave only single offline session per user and client";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema)
            throws SQLException {
        SqlDialect dialect = SqlDialect.of(connection);
        if (dialect != SqlDialect.MYSQL && dialect != SqlDialect.MARIADB) {
            return List.of();
        }

        String table = NativeSqlHelper.qualified("FED_USER_ATTRIBUTE", defaultSchema);
        List<String> stmts = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT t.ID, t.VALUE FROM " + table + " t WHERE LENGTH(t.VALUE) > 2024");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString(1);
                String value = rs.getString(2);
                if (value != null && value.length() > 2024) {
                    byte[] hash = JpaHashUtils.hashForAttributeValue(value);
                    byte[] hashLower = JpaHashUtils.hashForAttributeValueLowerCase(value);
                    stmts.add("UPDATE " + table
                            + " SET VALUE = NULL"
                            + ", LONG_VALUE_HASH = 0x" + HexFormat.of().formatHex(hash)
                            + ", LONG_VALUE_HASH_LOWER_CASE = 0x" + HexFormat.of().formatHex(hashLower)
                            + ", LONG_VALUE = " + NativeSqlHelper.sqlLiteral(value, dialect)
                            + " WHERE ID = " + NativeSqlHelper.sqlLiteral(id, dialect));
                }
            }
        }
        return stmts;
    }
}
