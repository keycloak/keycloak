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
import java.util.List;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.NativeSqlHelper;
import org.keycloak.enums.SqlDialect;
import org.keycloak.migration.MigrationProvider;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClaimMask;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate1_2_0_Beta1}.
 * Migrates social providers to identity providers, social links to federated identities,
 * adds access code login timeout, adds new admin roles, and migrates protocol mappers
 * from the old claims-mask model.
 */
public class JpaUpdate1_2_0_Beta1Task implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Update 1.2.0.Beta1";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        if (!NativeSqlHelper.isApplicable(connection, defaultSchema, dialect)) {
            return List.of();
        }
        List<String> stmts = new ArrayList<>();
        convertSocialToIdFedRealms(connection, dialect, defaultSchema, stmts);
        convertSocialToIdFedUsers(connection, defaultSchema, stmts);
        addAccessCodeLoginTimeout(defaultSchema, stmts);
        addNewAdminRoles(connection, dialect, defaultSchema, stmts);
        addDefaultProtocolMappers(session, connection, dialect, defaultSchema, stmts);
        return stmts;
    }

    // -------------------------------------------------------------------------
    // Social → Identity Providers
    // -------------------------------------------------------------------------

    private void convertSocialToIdFedRealms(Connection conn, SqlDialect dialect, String schema, List<String> stmts)
            throws SQLException {
        String realmSocialConfigTable = NativeSqlHelper.qualified("REALM_SOCIAL_CONFIG", schema);
        String realmTable = NativeSqlHelper.qualified("REALM", schema);
        String idpTable = NativeSqlHelper.qualified("IDENTITY_PROVIDER", schema);
        String idpConfigTable = NativeSqlHelper.qualified("IDENTITY_PROVIDER_CONFIG", schema);

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT RSC.NAME, VALUE, REALM_ID, UPDATE_PROFILE_ON_SOC_LOGIN FROM " + realmSocialConfigTable
                        + " RSC, " + realmTable + " REALM WHERE RSC.REALM_ID = REALM.ID"
                        + " ORDER BY RSC.REALM_ID, RSC.NAME");
             ResultSet rs = ps.executeQuery()) {

            boolean providerInProgress = false;
            String socialProviderId = null;
            String clientId = null;
            String realmId = null;
            boolean updateProfileOnSocialLogin = false;

            while (rs.next()) {
                if (!providerInProgress) {
                    String key = rs.getString("NAME");
                    int keyIndex = key.indexOf(".key");
                    if (keyIndex == -1) {
                        throw new IllegalStateException("Can't parse the provider from column: " + key);
                    }
                    socialProviderId = key.substring(0, keyIndex);
                    clientId = rs.getString("VALUE");
                    realmId = rs.getString("REALM_ID");
                    updateProfileOnSocialLogin = rs.getBoolean("UPDATE_PROFILE_ON_SOC_LOGIN");
                    providerInProgress = true;
                } else {
                    String clientSecret = rs.getString("VALUE");
                    String internalId = KeycloakModelUtils.generateId();

                    stmts.add("INSERT INTO " + idpTable
                            + " (INTERNAL_ID, ENABLED, PROVIDER_ALIAS, PROVIDER_ID, UPDATE_PROFILE_FIRST_LOGIN,"
                            + " STORE_TOKEN, AUTHENTICATE_BY_DEFAULT, REALM_ID) VALUES ("
                            + NativeSqlHelper.sqlLiteral(internalId) + ", "
                            + NativeSqlHelper.sqlLiteral(true, dialect) + ", "
                            + NativeSqlHelper.sqlLiteral(socialProviderId) + ", "
                            + NativeSqlHelper.sqlLiteral(socialProviderId) + ", "
                            + NativeSqlHelper.sqlLiteral(updateProfileOnSocialLogin, dialect) + ", "
                            + NativeSqlHelper.sqlLiteral(false, dialect) + ", "
                            + NativeSqlHelper.sqlLiteral(false, dialect) + ", "
                            + NativeSqlHelper.sqlLiteral(realmId) + ")");
                    stmts.add("INSERT INTO " + idpConfigTable
                            + " (IDENTITY_PROVIDER_ID, NAME, VALUE) VALUES ("
                            + NativeSqlHelper.sqlLiteral(internalId) + ", 'clientId', "
                            + NativeSqlHelper.sqlLiteral(clientId) + ")");
                    stmts.add("INSERT INTO " + idpConfigTable
                            + " (IDENTITY_PROVIDER_ID, NAME, VALUE) VALUES ("
                            + NativeSqlHelper.sqlLiteral(internalId) + ", 'clientSecret', "
                            + NativeSqlHelper.sqlLiteral(clientSecret) + ")");
                    providerInProgress = false;
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Social Links → Federated Identities
    // -------------------------------------------------------------------------

    private void convertSocialToIdFedUsers(Connection conn, String schema, List<String> stmts)
            throws SQLException {
        String userSocialLinkTable = NativeSqlHelper.qualified("USER_SOCIAL_LINK", schema);
        String fedIdentityTable = NativeSqlHelper.qualified("FEDERATED_IDENTITY", schema);

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT REALM_ID, USER_ID, SOCIAL_PROVIDER, SOCIAL_USER_ID, SOCIAL_USERNAME FROM "
                        + userSocialLinkTable);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                stmts.add("INSERT INTO " + fedIdentityTable
                        + " (REALM_ID, USER_ID, IDENTITY_PROVIDER, FEDERATED_USER_ID, FEDERATED_USERNAME) VALUES ("
                        + NativeSqlHelper.sqlLiteral(rs.getString("REALM_ID")) + ", "
                        + NativeSqlHelper.sqlLiteral(rs.getString("USER_ID")) + ", "
                        + NativeSqlHelper.sqlLiteral(rs.getString("SOCIAL_PROVIDER")) + ", "
                        + NativeSqlHelper.sqlLiteral(rs.getString("SOCIAL_USER_ID")) + ", "
                        + NativeSqlHelper.sqlLiteral(rs.getString("SOCIAL_USERNAME")) + ")");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Access code login timeout
    // -------------------------------------------------------------------------

    private void addAccessCodeLoginTimeout(String schema, List<String> stmts) {
        String realmTable = NativeSqlHelper.qualified("REALM", schema);
        stmts.add("UPDATE " + realmTable + " SET LOGIN_LIFESPAN = 1800 WHERE LOGIN_LIFESPAN IS NULL");
    }

    // -------------------------------------------------------------------------
    // Admin roles
    // -------------------------------------------------------------------------

    private void addNewAdminRoles(Connection conn, SqlDialect dialect, String schema, List<String> stmts) throws SQLException {
        addNewMasterAdminRoles(conn, dialect, schema, stmts);
        addNewRealmAdminRoles(conn, dialect, schema, stmts);
    }

    private void addNewMasterAdminRoles(Connection conn, SqlDialect dialect, String schema, List<String> stmts) throws SQLException {
        String adminRoleId = getAdminRoleId(conn, schema);
        String masterRealmId = Config.getAdminRealm();
        String realmTable = NativeSqlHelper.qualified("REALM", schema);
        String clientTable = NativeSqlHelper.qualified("CLIENT", schema);

        try (PreparedStatement ps = conn.prepareStatement("SELECT NAME FROM " + realmTable);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String realmName = rs.getString("NAME");
                String masterAdminAppName = realmName + "-realm";

                try (PreparedStatement ps2 = conn.prepareStatement(
                        "SELECT ID FROM " + clientTable + " WHERE REALM_ID = ? AND NAME = ?")) {
                    ps2.setString(1, masterRealmId);
                    ps2.setString(2, masterAdminAppName);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        if (rs2.next()) {
                            String masterAdminAppId = rs2.getString("ID");
                            addAdminRole(schema, stmts, dialect, AdminRoles.VIEW_IDENTITY_PROVIDERS,
                                    masterRealmId, masterAdminAppId, adminRoleId);
                            addAdminRole(schema, stmts, dialect, AdminRoles.MANAGE_IDENTITY_PROVIDERS,
                                    masterRealmId, masterAdminAppId, adminRoleId);
                        }
                    }
                }
            }
        }
    }

    private String getAdminRoleId(Connection conn, String schema) throws SQLException {
        String roleTable = NativeSqlHelper.qualified("KEYCLOAK_ROLE", schema);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ID FROM " + roleTable + " WHERE NAME = ? AND REALM = ?")) {
            ps.setString(1, AdminRoles.ADMIN);
            ps.setString(2, Config.getAdminRealm());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("ID");
            }
        }
        throw new IllegalStateException("Couldn't find ID of 'admin' role in 'master' realm");
    }

    private void addNewRealmAdminRoles(Connection conn, SqlDialect dialect, String schema, List<String> stmts) throws SQLException {
        String clientTable = NativeSqlHelper.qualified("CLIENT", schema);
        String roleTable = NativeSqlHelper.qualified("KEYCLOAK_ROLE", schema);

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT CLIENT.ID, CLIENT.REALM_ID, KEYCLOAK_ROLE.ID FROM "
                        + clientTable + " CLIENT, " + roleTable + " KEYCLOAK_ROLE"
                        + " WHERE KEYCLOAK_ROLE.APPLICATION = CLIENT.ID"
                        + " AND CLIENT.NAME = 'realm-management' AND KEYCLOAK_ROLE.NAME = ?")) {
            ps.setString(1, AdminRoles.REALM_ADMIN);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String realmAdminAppId = rs.getString(1);
                    String realmId = rs.getString(2);
                    String adminRoleId = rs.getString(3);
                    addAdminRole(schema, stmts, dialect, AdminRoles.VIEW_IDENTITY_PROVIDERS, realmId, realmAdminAppId, adminRoleId);
                    addAdminRole(schema, stmts, dialect, AdminRoles.MANAGE_IDENTITY_PROVIDERS, realmId, realmAdminAppId, adminRoleId);
                }
            }
        }
    }

    private void addAdminRole(String schema, List<String> stmts, SqlDialect dialect, String roleName,
            String realmId, String applicationId, String realmAdminAppRoleId) {
        String roleTable = NativeSqlHelper.qualified("KEYCLOAK_ROLE", schema);
        String compositeRoleTable = NativeSqlHelper.qualified("COMPOSITE_ROLE", schema);
        String newRoleId = KeycloakModelUtils.generateId();

        stmts.add("INSERT INTO " + roleTable
                + " (ID, APP_REALM_CONSTRAINT, APPLICATION_ROLE, NAME, REALM_ID, APPLICATION) VALUES ("
                + NativeSqlHelper.sqlLiteral(newRoleId) + ", "
                + NativeSqlHelper.sqlLiteral(applicationId) + ", "
                + NativeSqlHelper.sqlLiteral(true, dialect) + ", "
                + NativeSqlHelper.sqlLiteral(roleName) + ", "
                + NativeSqlHelper.sqlLiteral(realmId) + ", "
                + NativeSqlHelper.sqlLiteral(applicationId) + ")");

        stmts.add("INSERT INTO " + compositeRoleTable
                + " (COMPOSITE, CHILD_ROLE) VALUES ("
                + NativeSqlHelper.sqlLiteral(realmAdminAppRoleId) + ", "
                + NativeSqlHelper.sqlLiteral(newRoleId) + ")");
    }

    // -------------------------------------------------------------------------
    // Protocol mappers from claims mask
    // -------------------------------------------------------------------------

    private void addDefaultProtocolMappers(KeycloakSession session, Connection conn, SqlDialect dialect, String schema,
            List<String> stmts) throws SQLException {
        String clientTable = NativeSqlHelper.qualified("CLIENT", schema);
        String protocolMapperTable = NativeSqlHelper.qualified("PROTOCOL_MAPPER", schema);
        String protocolMapperCfgTable = NativeSqlHelper.qualified("PROTOCOL_MAPPER_CONFIG", schema);
        MigrationProvider migrationProvider = session.getProvider(MigrationProvider.class);

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ID, NAME, ALLOWED_CLAIMS_MASK FROM " + clientTable);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String clientId = rs.getString("ID");
                Object acmObj = rs.getObject("ALLOWED_CLAIMS_MASK");
                long mask = (acmObj != null) ? ((Number) acmObj).longValue() : ClaimMask.ALL;

                List<ProtocolMapperRepresentation> mappers = migrationProvider.getMappersForClaimMask(mask);
                for (ProtocolMapperRepresentation mapper : mappers) {
                    String mapperId = KeycloakModelUtils.generateId();
                    stmts.add("INSERT INTO " + protocolMapperTable
                            + " (ID, PROTOCOL, NAME, CONSENT_REQUIRED, PROTOCOL_MAPPER_NAME, CLIENT_ID) VALUES ("
                            + NativeSqlHelper.sqlLiteral(mapperId) + ", "
                            + NativeSqlHelper.sqlLiteral(mapper.getProtocol()) + ", "
                            + NativeSqlHelper.sqlLiteral(mapper.getName()) + ", "
                            + NativeSqlHelper.sqlLiteral(false, dialect) + ", "
                            + NativeSqlHelper.sqlLiteral(mapper.getProtocolMapper()) + ", "
                            + NativeSqlHelper.sqlLiteral(clientId) + ")");

                    for (Map.Entry<String, String> cfgEntry : mapper.getConfig().entrySet()) {
                        stmts.add("INSERT INTO " + protocolMapperCfgTable
                                + " (PROTOCOL_MAPPER_ID, NAME, VALUE) VALUES ("
                                + NativeSqlHelper.sqlLiteral(mapperId) + ", "
                                + NativeSqlHelper.sqlLiteral(cfgEntry.getKey()) + ", "
                                + NativeSqlHelper.sqlLiteral(cfgEntry.getValue()) + ")");
                    }
                }
            }
        }
    }
}
