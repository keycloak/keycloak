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

import org.keycloak.theme.DefaultThemeSelectorProvider;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.core.Table;

public class JpaUpdate27_0_0_UpdateLoginTheme extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        statements.add(new UpdateStatement(null, null, database.correctObjectName("REALM", Table.class))
                .addNewColumnValue("LOGIN_THEME", "keycloak.v3")
                .setWhereClause("LOGIN_THEME=? OR LOGIN_THEME=?")
                .addWhereParameter("keycloak.v2")
                .addWhereParameter("rh-sso.v2"));

        if ("oracle".equals(database.getShortName())) {
            statements.add(new UpdateStatement(null, null, database.correctObjectName("CLIENT_ATTRIBUTES", Table.class))
                    .addNewColumnValue("VALUE", "keycloak.v3")
                    .setWhereClause("NAME=? AND (DBMS_LOB.substr(VALUE,12)=? OR DBMS_LOB.substr(VALUE,11)=?)")
                    .addWhereParameter(DefaultThemeSelectorProvider.LOGIN_THEME_KEY)
                    .addWhereParameter("keycloak.v2")
                    .addWhereParameter("rh-sso.v2"));
        } else {
            statements.add(new UpdateStatement(null, null, database.correctObjectName("CLIENT_ATTRIBUTES", Table.class))
                    .addNewColumnValue("VALUE", "keycloak.v3")
                    .setWhereClause("NAME=? AND (VALUE=? OR VALUE=?)")
                    .addWhereParameter(DefaultThemeSelectorProvider.LOGIN_THEME_KEY)
                    .addWhereParameter("keycloak.v2")
                    .addWhereParameter("rh-sso.v2"));
        }
    }

    @Override
    protected String getTaskId() {
        return "Update login theme from keycloak.v2 to keycloak.v3 for keycloak 27.0.0";
    }

}
