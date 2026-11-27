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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.statement.SqlStatement;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * During manual migration script generation the custom changes read the pre-upgrade database. For a starting version
 * older than 26.0.0 that database has realms, but neither {@code ORG}, {@code ORG_DOMAIN} nor
 * {@code IDENTITY_PROVIDER.ORGANIZATION_ID}, so the tasks that read them must emit nothing instead of failing.
 */
public class CustomKeycloakTaskPreUpgradeSchemaTest {

    @Test
    public void domainIdpRoutingEmitsNothingWhenOrganizationSchemaIsAbsent() throws Exception {
        try (Connection connection = openPreUpgradeDatabase()) {
            assertEquals(0, new JpaUpdate26_8_0_DomainIdpRoutingMigration().generateStatements(database(connection)).length);
        }
    }

    @Test
    public void idpMapperOrganizationIdEmitsNothingWhenOrganizationSchemaIsAbsent() throws Exception {
        try (Connection connection = openPreUpgradeDatabase()) {
            assertEquals(0, new JpaUpdate26_8_0_IdpMapperOrganizationId().generateStatements(database(connection)).length);
        }
    }

    private static Connection openPreUpgradeDatabase() throws Exception {
        Connection connection = open();
        execute(connection,
                "CREATE TABLE REALM (ID VARCHAR(36) PRIMARY KEY)",
                "INSERT INTO REALM VALUES ('master')",
                "CREATE TABLE IDENTITY_PROVIDER (INTERNAL_ID VARCHAR(36) PRIMARY KEY, REALM_ID VARCHAR(36), PROVIDER_ALIAS VARCHAR(255))",
                "CREATE TABLE IDENTITY_PROVIDER_CONFIG (IDENTITY_PROVIDER_ID VARCHAR(36), NAME VARCHAR(255), VALUE VARCHAR(255))",
                "CREATE TABLE IDENTITY_PROVIDER_MAPPER (ID VARCHAR(36) PRIMARY KEY, IDP_ALIAS VARCHAR(255), REALM_ID VARCHAR(36))",
                "CREATE TABLE IDP_MAPPER_CONFIG (IDP_MAPPER_ID VARCHAR(36), NAME VARCHAR(255), VALUE VARCHAR(255))");
        return connection;
    }

    @Test
    public void legacyKcOrgConfigLinksAreMigratedFromA25Database() throws Exception {
        try (Connection connection = open()) {
            // 25.x: organizations exist but the IdP link is still the kc.org entry of IDENTITY_PROVIDER_CONFIG
            execute(connection,
                    "CREATE TABLE REALM (ID VARCHAR(36) PRIMARY KEY)",
                    "INSERT INTO REALM VALUES ('master')",
                    "CREATE TABLE ORG (ID VARCHAR(255) PRIMARY KEY, REALM_ID VARCHAR(255))",
                    "CREATE TABLE ORG_DOMAIN (ID VARCHAR(36) PRIMARY KEY, NAME VARCHAR(255), ORG_ID VARCHAR(255))",
                    "CREATE TABLE IDENTITY_PROVIDER (INTERNAL_ID VARCHAR(36) PRIMARY KEY, REALM_ID VARCHAR(36), PROVIDER_ALIAS VARCHAR(255))",
                    "CREATE TABLE IDENTITY_PROVIDER_CONFIG (IDENTITY_PROVIDER_ID VARCHAR(36), NAME VARCHAR(255), VALUE VARCHAR(255))",
                    "CREATE TABLE IDENTITY_PROVIDER_MAPPER (ID VARCHAR(36) PRIMARY KEY, IDP_ALIAS VARCHAR(255), REALM_ID VARCHAR(36))",
                    "CREATE TABLE IDP_MAPPER_CONFIG (IDP_MAPPER_ID VARCHAR(36), NAME VARCHAR(255), VALUE VARCHAR(255))",
                    "INSERT INTO ORG VALUES ('org1', 'master')",
                    "INSERT INTO ORG_DOMAIN VALUES ('dom1', 'example.org', 'org1')",
                    "INSERT INTO IDENTITY_PROVIDER VALUES ('idp1', 'master', 'alias1')",
                    "INSERT INTO IDENTITY_PROVIDER_CONFIG VALUES ('idp1', 'kc.org', 'org1')",
                    "INSERT INTO IDENTITY_PROVIDER_CONFIG VALUES ('idp1', 'kc.org.domain', 'example.org')",
                    "INSERT INTO IDENTITY_PROVIDER_MAPPER VALUES ('mapper1', 'alias1', 'master')",
                    "INSERT INTO IDP_MAPPER_CONFIG VALUES ('mapper1', 'groupType', 'ORGANIZATION')");

            Database database = database(connection);

            SqlStatement[] routing = new JpaUpdate26_8_0_DomainIdpRoutingMigration().generateStatements(database);
            // the ORG_IDENTITY_PROVIDER link, the domain routing and three config clean-up deletes
            assertEquals(5, routing.length);
            SqlStatement[] mapper = new JpaUpdate26_8_0_IdpMapperOrganizationId().generateStatements(database);
            assertEquals(1, mapper.length);
        }
    }

    @Test
    public void tasksReadingOrganizationSchemaEmitStatementsWhenItIsPresent() throws Exception {
        try (Connection connection = open()) {
            execute(connection,
                    "CREATE TABLE REALM (ID VARCHAR(36) PRIMARY KEY)",
                    "INSERT INTO REALM VALUES ('master')",
                    "CREATE TABLE ORG (ID VARCHAR(255) PRIMARY KEY, REALM_ID VARCHAR(255))",
                    "CREATE TABLE ORG_DOMAIN (ID VARCHAR(36) PRIMARY KEY, NAME VARCHAR(255), ORG_ID VARCHAR(255))",
                    "CREATE TABLE IDENTITY_PROVIDER (INTERNAL_ID VARCHAR(36) PRIMARY KEY, REALM_ID VARCHAR(36), PROVIDER_ALIAS VARCHAR(255), ORGANIZATION_ID VARCHAR(255))",
                    "CREATE TABLE IDENTITY_PROVIDER_CONFIG (IDENTITY_PROVIDER_ID VARCHAR(36), NAME VARCHAR(255), VALUE VARCHAR(255))",
                    "CREATE TABLE IDENTITY_PROVIDER_MAPPER (ID VARCHAR(36) PRIMARY KEY, IDP_ALIAS VARCHAR(255), REALM_ID VARCHAR(36))",
                    "CREATE TABLE IDP_MAPPER_CONFIG (IDP_MAPPER_ID VARCHAR(36), NAME VARCHAR(255), VALUE VARCHAR(255))");

            Database database = database(connection);

            SqlStatement[] routing = new JpaUpdate26_8_0_DomainIdpRoutingMigration().generateStatements(database);
            assertTrue(routing.length > 0);
            // no organization group mappers, so there is nothing to record, but the task must have run
            JpaUpdate26_8_0_IdpMapperOrganizationId mapperTask = new JpaUpdate26_8_0_IdpMapperOrganizationId();
            assertEquals(0, mapperTask.generateStatements(database).length);
            assertTrue(mapperTask.getConfirmationMessage().contains("Recorded the target organization"));
        }
    }

    private static Connection open() throws Exception {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";NON_KEYWORDS=VALUE";
        Connection connection = DriverManager.getConnection(url, "sa", "");
        connection.setAutoCommit(false);
        return connection;
    }

    private static Database database(Connection connection) throws Exception {
        return DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
    }

    private static void execute(Connection connection, String... sql) throws Exception {
        try (Statement st = connection.createStatement()) {
            for (String s : sql) {
                st.execute(s);
            }
        }
        connection.commit();
    }
}
