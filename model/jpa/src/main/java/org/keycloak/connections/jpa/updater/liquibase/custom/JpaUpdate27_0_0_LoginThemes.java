package org.keycloak.connections.jpa.updater.liquibase.custom;

import org.keycloak.theme.DefaultThemeSelectorProvider;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.core.Table;

public class JpaUpdate27_0_0_LoginThemes extends CustomKeycloakTask {

    private static final String LOGIN_V3_THEME = "keycloak.v3";

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        statements.add(new UpdateStatement(null, null, database.correctObjectName("REALM", Table.class))
                .addNewColumnValue("LOGIN_THEME", LOGIN_V3_THEME)
                .setWhereClause("LOGIN_THEME=? OR LOGIN_THEME=? OR LOGIN_THEME=?")
                .addWhereParameter("keycloak")
                .addWhereParameter("keycloak.v2")
                .addWhereParameter("rh-sso.v2"));

        // Oracle cannot compare NCLOBs with SQL '=': https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Comparison-Conditions.html
        if ("oracle".equals(database.getShortName())) {
            for (String legacyTheme : new String[] {"keycloak", "keycloak.v2", "rh-sso.v2"}) {
                statements.add(new UpdateStatement(null, null, database.correctObjectName("CLIENT_ATTRIBUTES", Table.class))
                        .addNewColumnValue("VALUE", LOGIN_V3_THEME)
                        .setWhereClause("NAME=? AND DBMS_LOB.COMPARE(VALUE, ?)=0")
                        .addWhereParameter(DefaultThemeSelectorProvider.LOGIN_THEME_KEY)
                        .addWhereParameter(legacyTheme));
            }
        } else {
            statements.add(new UpdateStatement(null, null, database.correctObjectName("CLIENT_ATTRIBUTES", Table.class))
                    .addNewColumnValue("VALUE", LOGIN_V3_THEME)
                    .setWhereClause("NAME=? AND (VALUE=? OR VALUE=? OR VALUE=?)")
                    .addWhereParameter(DefaultThemeSelectorProvider.LOGIN_THEME_KEY)
                    .addWhereParameter("keycloak")
                    .addWhereParameter("keycloak.v2")
                    .addWhereParameter("rh-sso.v2"));
        }
    }

    @Override
    protected String getTaskId() {
        return "Migrate Login v1 themes for Keycloak 27.0.0";
    }
}
