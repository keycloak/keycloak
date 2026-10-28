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

import java.sql.SQLException;

/**
 * Abstraction over two execution modes for a change-set runner:
 * <ul>
 *   <li>{@link JdbcSqlSink} — executes SQL directly on the live JDBC connection ({@code update} mode).</li>
 *   <li>{@link ScriptSqlSink} — writes SQL to a file writer ({@code export} / manual mode).</li>
 * </ul>
 */
public interface SqlSink {

    /**
     * Sends one SQL statement to the sink.
     *
     * @param sql         the statement text (without trailing semicolon)
     * @param changeSetId the owning change-set id (used for diagnostics)
     */
    void execute(String sql, String changeSetId) throws SQLException;

    /**
     * Emits a comment line.  For {@link JdbcSqlSink} this is a no-op; for {@link ScriptSqlSink} the text
     * is written as a SQL comment.
     */
    void comment(String text) throws SQLException;

    /** Returns true when this sink writes to a file rather than executing statements. */
    boolean isExportMode();
}
