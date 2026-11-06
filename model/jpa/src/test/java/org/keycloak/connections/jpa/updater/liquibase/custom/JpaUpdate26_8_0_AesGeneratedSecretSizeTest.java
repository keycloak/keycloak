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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

import org.keycloak.common.util.Base64Url;
import org.keycloak.connections.jpa.updater.liquibase.LiquibaseConstants;

import liquibase.Scope;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.statement.SqlStatement;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies the backfill of the secretSize configuration for aes-generated key providers created before the default
 * changed in 26.7.0, so that the admin console shows their real size and saving them does not rotate the key
 * (see https://github.com/keycloak/keycloak/issues/54016).
 */
public class JpaUpdate26_8_0_AesGeneratedSecretSizeTest {

    private static final String PARENT_ID = "00000000-0000-0000-0000-000000000001";

    @Test
    public void testStoresSizeOfProvidersWithoutConfiguredSize() throws Exception {
        byte[] legacySecret = new byte[16];
        Arrays.fill(legacySecret, (byte) 'a');
        byte[] legacySecretSecondRealm = new byte[16];
        Arrays.fill(legacySecretSecondRealm, (byte) 'b');
        byte[] modernSecret = new byte[32];
        Arrays.fill(modernSecret, (byte) 'c');
        byte[] nonDefaultSecret = new byte[24];
        Arrays.fill(nonDefaultSecret, (byte) 'd');
        byte[] hmacSecret = new byte[128];
        Arrays.fill(hmacSecret, (byte) 'e');

        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:aesGeneratedSecretSizeMigrationTest;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                // CustomKeycloakTask.isApplicable() snapshots the REALM table and requires at least one realm.
                statement.execute("CREATE TABLE REALM (ID VARCHAR(36) PRIMARY KEY)");
                statement.execute("INSERT INTO REALM (ID) VALUES ('00000000-0000-0000-0000-000000000003')");
                statement.execute("CREATE TABLE COMPONENT (ID VARCHAR(36) PRIMARY KEY, NAME VARCHAR(255), PARENT_ID VARCHAR(36), PROVIDER_TYPE VARCHAR(255), PROVIDER_ID VARCHAR(255))");
                statement.execute("CREATE TABLE COMPONENT_CONFIG (ID VARCHAR(36), COMPONENT_ID VARCHAR(36), NAME VARCHAR(255), VALUE VARCHAR(4000))");
            }
            // Provider created before 26.7.0: 16-byte secret, no secretSize stored.
            insertComponent(connection, "legacy-aes");
            insertConfig(connection, "legacy-aes", "kid", "kid-legacy-aes");
            insertConfig(connection, "legacy-aes", "secret", Base64Url.encode(legacySecret));
            // The unpadded base64url encoding of 16 bytes is 22 characters, as observed in the issue.
            Assert.assertEquals(22, Base64Url.encode(legacySecret).length());
            // Provider created after 26.7.0: 32-byte secret with the size configured, must stay untouched.
            insertComponent(connection, "modern-aes");
            insertConfig(connection, "modern-aes", "kid", "kid-modern-aes");
            insertConfig(connection, "modern-aes", "secret", Base64Url.encode(modernSecret));
            insertConfig(connection, "modern-aes", "secretSize", "32");
            // A second pre-26.7.0 provider in another realm: the migration is not realm-scoped.
            insertComponent(connection, "legacy-aes-second-realm", "00000000-0000-0000-0000-000000000002", "aes-generated");
            insertConfig(connection, "legacy-aes-second-realm", "kid", "kid-legacy-aes-second-realm");
            insertConfig(connection, "legacy-aes-second-realm", "secret", Base64Url.encode(legacySecretSecondRealm));
            // An aes-generated provider whose secret was never generated yet must be skipped.
            insertComponent(connection, "fallback-without-secret");
            insertConfig(connection, "fallback-without-secret", "priority", "-100");
            // A provider of another type must not be touched, even when it has no secretSize either.
            insertComponent(connection, "hmac-legacy", PARENT_ID, "hmac-generated");
            insertConfig(connection, "hmac-legacy", "kid", "kid-hmac-legacy");
            insertConfig(connection, "hmac-legacy", "secret", Base64Url.encode(hmacSecret));
            // An aes-generated provider with a non-default secret size but no stored size gets its actual size.
            insertComponent(connection, "aes-non-default-size");
            insertConfig(connection, "aes-non-default-size", "kid", "kid-aes-non-default-size");
            insertConfig(connection, "aes-non-default-size", "secret", Base64Url.encode(nonDefaultSecret));
            connection.commit();

            JpaUpdate26_8_0_AesGeneratedSecretSize task = new JpaUpdate26_8_0_AesGeneratedSecretSize();
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            SqlStatement[] statements = task.generateStatements(database);
            Executor executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor(LiquibaseConstants.JDBC_EXECUTOR, database);
            for (SqlStatement sqlStatement : statements) {
                executor.execute(sqlStatement);
            }
            connection.commit();

            Assert.assertEquals("16", secretSize(connection, "legacy-aes"));
            Assert.assertEquals("16", secretSize(connection, "legacy-aes-second-realm"));
            Assert.assertEquals("32", secretSize(connection, "modern-aes"));
            Assert.assertEquals("24", secretSize(connection, "aes-non-default-size"));
            Assert.assertNull(secretSize(connection, "fallback-without-secret"));
            Assert.assertNull(secretSize(connection, "hmac-legacy"));

            // The migration only adds the size; the secret and kid of the existing providers are never touched.
            Assert.assertEquals(Base64Url.encode(legacySecret), singleConfig(connection, "legacy-aes", "secret"));
            Assert.assertEquals("kid-legacy-aes", singleConfig(connection, "legacy-aes", "kid"));
            Assert.assertEquals(Base64Url.encode(hmacSecret), singleConfig(connection, "hmac-legacy", "secret"));

            // Exactly one secretSize entry per sized component, also for the one that already had a value.
            Assert.assertEquals(1, countConfigs(connection, "legacy-aes", "secretSize"));
            Assert.assertEquals(1, countConfigs(connection, "modern-aes", "secretSize"));

            Assert.assertTrue(task.getConfirmationMessage(), task.getConfirmationMessage().contains("3 aes-generated key provider(s)"));
        }
    }

    private void insertComponent(Connection connection, String componentId) throws Exception {
        insertComponent(connection, componentId, PARENT_ID, "aes-generated");
    }

    private void insertComponent(Connection connection, String componentId, String parentId, String providerId) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO COMPONENT (ID, NAME, PARENT_ID, PROVIDER_TYPE, PROVIDER_ID) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, componentId);
            ps.setString(2, componentId);
            ps.setString(3, parentId);
            ps.setString(4, "org.keycloak.keys.KeyProvider");
            ps.setString(5, providerId);
            ps.executeUpdate();
        }
    }

    private void insertConfig(Connection connection, String componentId, String name, String value) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO COMPONENT_CONFIG (ID, COMPONENT_ID, NAME, VALUE) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, componentId + "-" + name);
            ps.setString(2, componentId);
            ps.setString(3, name);
            ps.setString(4, value);
            ps.executeUpdate();
        }
    }

    private String singleConfig(Connection connection, String componentId, String name) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT VALUE FROM COMPONENT_CONFIG WHERE COMPONENT_ID = ? AND NAME = ?")) {
            ps.setString(1, componentId);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                Assert.assertTrue(rs.next());
                return rs.getString(1);
            }
        }
    }

    private String secretSize(Connection connection, String componentId) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT VALUE FROM COMPONENT_CONFIG WHERE COMPONENT_ID = ? AND NAME = 'secretSize'")) {
            ps.setString(1, componentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private int countConfigs(Connection connection, String componentId, String name) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM COMPONENT_CONFIG WHERE COMPONENT_ID = ? AND NAME = ?")) {
            ps.setString(1, componentId);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                Assert.assertTrue(rs.next());
                return rs.getInt(1);
            }
        }
    }
}
