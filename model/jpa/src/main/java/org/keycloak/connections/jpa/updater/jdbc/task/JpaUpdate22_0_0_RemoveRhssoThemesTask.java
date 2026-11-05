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
import java.util.ArrayList;
import java.util.List;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;
import org.keycloak.theme.DefaultThemeSelectorProvider;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate22_0_0_RemoveRhssoThemes}.
 */
public class JpaUpdate22_0_0_RemoveRhssoThemesTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Remove RH-SSO themes for keycloak 22.0.0";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        String realmTable = SqlDialect.qualified("REALM", defaultSchema);
        String clientAttributesTable = SqlDialect.qualified("CLIENT_ATTRIBUTES", defaultSchema);
        List<String> stmts = new ArrayList<>();

        // remove login theme
        stmts.add("UPDATE " + realmTable
                + " SET LOGIN_THEME = NULL WHERE LOGIN_THEME = 'rh-sso'");
        // remove email theme
        stmts.add("UPDATE " + realmTable
                + " SET EMAIL_THEME = NULL WHERE EMAIL_THEME = 'rh-sso'");
        // update account theme
        stmts.add("UPDATE " + realmTable
                + " SET ACCOUNT_THEME = 'keycloak.v2'"
                + " WHERE ACCOUNT_THEME = 'rh-sso'"
                + " OR ACCOUNT_THEME = 'rh-sso.v2'"
                + " OR ACCOUNT_THEME = 'keycloak'");

        // remove login theme from clients — Oracle needs DBMS_LOB.substr
        String loginThemeKey = dialect.literal(DefaultThemeSelectorProvider.LOGIN_THEME_KEY);
        if (dialect == SqlDialect.ORACLE) {
            stmts.add("DELETE FROM " + clientAttributesTable
                    + " WHERE NAME = " + loginThemeKey
                    + " AND DBMS_LOB.substr(VALUE,10) = 'rh-sso'");
        } else {
            stmts.add("DELETE FROM " + clientAttributesTable
                    + " WHERE NAME = " + loginThemeKey
                    + " AND VALUE = 'rh-sso'");
        }
        return stmts;
    }
}
