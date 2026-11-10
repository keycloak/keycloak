package org.keycloak.connections.jpa.updater.liquibase.custom;

import org.keycloak.theme.DefaultThemeSelectorProvider;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.core.Table;

public class JpaUpdate27_0_0_LoginThemes extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        statements.add(new UpdateStatement(null, null, database.correctObjectName("REALM", Table.class))
                .addNewColumnValue("LOGIN_THEME", "keycloak.v2")
                .setWhereClause("LOGIN_THEME=?")
                .addWhereParameter("keycloak"));

        // Oracle cannot compare NCLOBs with SQL '=': https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Comparison-Conditions.html
        String clientThemeCondition = "oracle".equals(database.getShortName())
                ? "NAME=? AND DBMS_LOB.COMPARE(VALUE, ?)=0"
                : "NAME=? AND VALUE=?";
        statements.add(new UpdateStatement(null, null, database.correctObjectName("CLIENT_ATTRIBUTES", Table.class))
                .addNewColumnValue("VALUE", "keycloak.v2")
                .setWhereClause(clientThemeCondition)
                .addWhereParameter(DefaultThemeSelectorProvider.LOGIN_THEME_KEY)
                .addWhereParameter("keycloak"));
    }

    @Override
    protected String getTaskId() {
        return "Migrate Login v1 themes for Keycloak 27.0.0";
    }
}
