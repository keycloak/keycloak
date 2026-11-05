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
package org.keycloak.connections.jpa.updater.jdbc;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;

import org.keycloak.enums.SqlDialect;

/**
 * {@link SqlSink} that writes SQL to a {@link Writer} ({@code export} / manual mode).
 * Each SQL statement is written followed by a statement terminator appropriate for the dialect
 * and a blank line.  Comments are written as SQL line comments.
 */
public class ScriptSqlSink implements SqlSink {

    private final Writer writer;
    private final SqlDialect dialect;

    public ScriptSqlSink(Writer writer, SqlDialect dialect) {
        this.writer = writer;
        this.dialect = dialect;
    }

    @Override
    public void execute(String sql, String changeSetId) throws SQLException {
        try {
            writer.write(sql);
            // Emit dialect-specific statement terminator
            switch (dialect) {
                case MSSQL -> {
                    // MSSQL uses GO as a batch separator on a new line
                    writer.write(System.lineSeparator());
                    writer.write("GO");
                }
                default -> {
                    // Keycloak's generated DDL/DML contains no PL/SQL blocks, so ';' is sufficient for
                    // every other dialect (including Oracle SQL*Plus).
                    if (!sql.endsWith(";")) {
                        writer.write(";");
                    }
                }
            }
            writer.write(System.lineSeparator());
            writer.write(System.lineSeparator());
        } catch (IOException e) {
            throw new SQLException("Failed to write SQL to export file", e);
        }
    }

    @Override
    public void comment(String text) throws SQLException {
        try {
            writer.write("-- ");
            writer.write(text);
            writer.write(System.lineSeparator());
        } catch (IOException e) {
            throw new SQLException("Failed to write comment to export file", e);
        }
    }

    @Override
    public boolean isExportMode() {
        return true;
    }
}
