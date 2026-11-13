/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

/**
 * Migrates built-in Account Console themes removed in 27.0.0 to {@code keycloak.v4}.
 */
public class JpaUpdate27_0_0_AccountThemes extends CustomKeycloakTask {

    private static final String ACCOUNT_V4_THEME = "keycloak.v4";

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        statements.add(new UpdateStatement(null, null, database.correctObjectName("REALM", Table.class))
                .addNewColumnValue("ACCOUNT_THEME", ACCOUNT_V4_THEME)
                .setWhereClause("ACCOUNT_THEME=? OR ACCOUNT_THEME=? OR ACCOUNT_THEME=?")
                .addWhereParameter("keycloak")
                .addWhereParameter("keycloak.v2")
                .addWhereParameter("keycloak.v3"));
    }

    @Override
    protected String getTaskId() {
        return "Migrate Account themes for Keycloak 27.0.0";
    }
}
