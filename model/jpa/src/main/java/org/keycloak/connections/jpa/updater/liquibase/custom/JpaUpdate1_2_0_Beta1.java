/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.migration.MigrationProvider;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClaimMask;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.statement.core.InsertStatement;
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.core.Table;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JpaUpdate1_2_0_Beta1 extends CustomKeycloakTask {

    private String realmTableName;

    @Override
    protected String getTaskId() {
        return "Update 1.2.0.Beta1";
    }

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        realmTableName = database.correctObjectName("REALM", Table.class);

        try {
            convertSocialToIdFedRealms();
            convertSocialToIdFedUsers();
            addAccessCodeLoginTimeout();
            addNewAdminRoles();
            addDefaultProtocolMappers();
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when updating data from previous version", e);
        }
    }


    protected void convertSocialToIdFedRealms() throws SQLException, DatabaseException {
        String identityProviderTableName = database.correctObjectName("IDENTITY_PROVIDER", Table.class);
        String idpConfigTableName = database.correctObjectName("IDENTITY_PROVIDER_CONFIG", Table.class);

        String realmSocialConfigTable = getTableName("REALM_SOCIAL_CONFIG");
        String realmTableName = getTableName("REALM");
        PreparedStatement statement = jdbcConnection.prepareStatement("select RSC.NAME, VALUE, REALM_ID, UPDATE_PROFILE_ON_SOC_LOGIN from " + realmSocialConfigTable + " RSC," + realmTableName +
                " REALM where RSC.REALM_ID = REALM.ID ORDER BY RSC.REALM_ID, RSC.NAME");
        try {
            ResultSet resultSet = statement.executeQuery();
            try {
                boolean providerInProgress = false;
                String socialProviderId = null;
                String clientId = null;
                String clientSecret;
                String realmId = null;
                boolean updateProfileOnSocialLogin = false;
                boolean first = true;

                while (resultSet.next()) {
                    if (first) {
                        confirmationMessage.append("Migrating social to identity providers: ");
                        first = false;
                    }

                    if (!providerInProgress) {
                        String key = resultSet.getString("NAME");
                        int keyIndex = key.indexOf(".key");
                        if (keyIndex == -1) {
                            throw new IllegalStateException("Can't parse the provider from column: " + key);
                        }

                        socialProviderId = key.substring(0, keyIndex);
                        clientId = resultSet.getString("VALUE");
                        realmId = resultSet.getString("REALM_ID");
                        updateProfileOnSocialLogin = resultSet.getBoolean("UPDATE_PROFILE_ON_SOC_LOGIN");
                        providerInProgress = true;
                    } else {
                        clientSecret = resultSet.getString("VALUE");

                        String internalId = KeycloakModelUtils.generateId();
                        InsertStatement idpInsert = new InsertStatement(null, null, identityProviderTableName)
                                .addColumnValue("INTERNAL_ID", internalId)
                                .addColumnValue("ENABLED", true)
                                .addColumnValue("PROVIDER_ALIAS", socialProviderId)
                                .addColumnValue("PROVIDER_ID", socialProviderId)
                                .addColumnValue("UPDATE_PROFILE_FIRST_LOGIN", updateProfileOnSocialLogin)
                                .addColumnValue("STORE_TOKEN", false)
                                .addColumnValue("AUTHENTICATE_BY_DEFAULT", false)
                                .addColumnValue("REALM_ID", realmId);
                        InsertStatement clientIdInsert = new InsertStatement(null, null, idpConfigTableName)
                                .addColumnValue("IDENTITY_PROVIDER_ID", internalId)
                                .addColumnValue("NAME", "clientId")
                                .addColumnValue("VALUE", clientId);
                        InsertStatement clientSecretInsert = new InsertStatement(null, null, idpConfigTableName)
                                .addColumnValue("IDENTITY_PROVIDER_ID", internalId)
                                .addColumnValue("NAME", "clientSecret")
                                .addColumnValue("VALUE", clientSecret);

                        statements.add(idpInsert);
                        statements.add(clientIdInsert);
                        statements.add(clientSecretInsert);
                        confirmationMessage.append(socialProviderId + " in realm " + realmId + ", ");

                        providerInProgress = false;
                    }
                }

                // It means that some provider where processed
                if (!first) {
                    confirmationMessage.append(". ");
                }
            } finally {
                resultSet.close();
            }
        } finally {
            statement.close();
        }
    }

    protected void convertSocialToIdFedUsers() throws SQLException, DatabaseException {
        String federatedIdentityTableName = database.correctObjectName("FEDERATED_IDENTITY", Table.class);
        PreparedStatement statement = jdbcConnection.prepareStatement("select REALM_ID, USER_ID, SOCIAL_PROVIDER, SOCIAL_USER_ID, SOCIAL_USERNAME from " + getTableName("USER_SOCIAL_LINK"));
        try {
            ResultSet resultSet = statement.executeQuery();
            try {
                int count = 0;
                while (resultSet.next()) {
                    InsertStatement insert = new InsertStatement(null, null, federatedIdentityTableName)
                            .addColumnValue("REALM_ID", resultSet.getString("REALM_ID"))
                            .addColumnValue("USER_ID", resultSet.getString("USER_ID"))
                            .addColumnValue("IDENTITY_PROVIDER", resultSet.getString("SOCIAL_PROVIDER"))
                            .addColumnValue("FEDERATED_USER_ID", resultSet.getString("SOCIAL_USER_ID"))
                            .addColumnValue("FEDERATED_USERNAME", resultSet.getString("SOCIAL_USERNAME"));
                    count++;
                    statements.add(insert);
                }

                confirmationMessage.append("Updating " + count + " social links to federated identities. ");
            } finally {
                resultSet.close();
            }
        } finally {
            statement.close();
        }
    }

    protected void addAccessCodeLoginTimeout() {
        UpdateStatement statement = new UpdateStatement(null, null, realmTableName)
                .addNewColumnValue("LOGIN_LIFESPAN", 1800)
                .setWhereClause("LOGIN_LIFESPAN IS NULL");
        statements.add(statement);

        confirmationMessage.append("Updated LOGIN_LIFESPAN of all realms to 1800 seconds. ");
    }

    private void addNewAdminRoles() throws SQLException, DatabaseException{
        addNewMasterAdminRoles();
        addNewRealmAdminRoles();

        confirmationMessage.append("Adding new admin roles. ");
    }

    protected void addNewMasterAdminRoles() throws SQLException, DatabaseException {
        // Retrieve ID of admin role of master realm
        String adminRoleId = getAdminRoleId();
        String masterRealmId = Config.getAdminRealm();

        PreparedStatement statement = jdbcConnection.prepareStatement("select NAME from " + getTableName("REALM"));
        try {
            ResultSet resultSet = statement.executeQuery();
            try {
                while (resultSet.next()) {
                    String realmName = resultSet.getString("NAME");
                    String masterAdminAppName = realmName + "-realm";

                    PreparedStatement statement2 = jdbcConnection.prepareStatement("select ID from " + getTableName("CLIENT") + " where REALM_ID = ? AND NAME = ?");
                    statement2.setString(1, masterRealmId);
                    statement2.setString(2, masterAdminAppName);

                    try {
                        ResultSet resultSet2 = statement2.executeQuery();
                        try {
                            if (resultSet2.next()) {
                                String masterAdminAppId = resultSet2.getString("ID");

                                addAdminRole(AdminRoles.VIEW_IDENTITY_PROVIDERS, masterRealmId, masterAdminAppId, adminRoleId);
                                addAdminRole(AdminRoles.MANAGE_IDENTITY_PROVIDERS, masterRealmId, masterAdminAppId, adminRoleId);
                            } else {
                                throw new IllegalStateException("Couldn't find ID of '" + masterAdminAppName + "' application in 'master' realm. ");
                            }
                        } finally {
                            resultSet2.close();
                        }
                    } finally {
                        statement2.close();
                    }
                }
            } finally {
                resultSet.close();
            }
        } finally {
            statement.close();
        }
    }

    private String getAdminRoleId() throws SQLException, DatabaseException {
        PreparedStatement statement = jdbcConnection.prepareStatement("select ID from " + getTableName("KEYCLOAK_ROLE") + " where NAME = ? AND REALM = ?");
        statement.setString(1, AdminRoles.ADMIN);
        statement.setString(2, Config.getAdminRealm());

        try {
            ResultSet resultSet = statement.executeQuery();
            try {
                if (resultSet.next()) {
                    return resultSet.getString("ID");
                } else {
                    throw new IllegalStateException("Couldn't find ID of 'admin' role in 'master' realm");
                }
            } finally {
                resultSet.close();
            }
        } finally {
            statement.close();
        }
    }


    protected void addNewRealmAdminRoles() throws SQLException, DatabaseException {
        PreparedStatement statement = jdbcConnection.prepareStatement("select CLIENT.ID REALM_ADMIN_APP_ID, CLIENT.REALM_ID REALM_ID, KEYCLOAK_ROLE.ID ADMIN_ROLE_ID from " +
                getTableName("CLIENT") + " CLIENT," + getTableName("KEYCLOAK_ROLE") + " KEYCLOAK_ROLE where KEYCLOAK_ROLE.APPLICATION = CLIENT.ID AND CLIENT.NAME = 'realm-management' AND KEYCLOAK_ROLE.NAME = ?");
        statement.setString(1, AdminRoles.REALM_ADMIN);

        try {
            ResultSet resultSet = statement.executeQuery();
            try {

                while (resultSet.next()) {
                    String realmAdminAppId = resultSet.getString("REALM_ADMIN_APP_ID");
                    String realmId = resultSet.getString("REALM_ID");
                    String adminRoleId = resultSet.getString("ADMIN_ROLE_ID");

                    addAdminRole(AdminRoles.VIEW_IDENTITY_PROVIDERS, realmId, realmAdminAppId, adminRoleId);
                    addAdminRole(AdminRoles.MANAGE_IDENTITY_PROVIDERS, realmId, realmAdminAppId, adminRoleId);
                }
            } finally {
                resultSet.close();
            }
        } finally {
            statement.close();
        }
    }

    private void addAdminRole(String roleName, String realmId, String applicationId, String realmAdminAppRoleId) {
        String roleTableName = database.correctObjectName("KEYCLOAK_ROLE", Table.class);
        String compositeRoleTableName = database.correctObjectName("COMPOSITE_ROLE", Table.class);
        String newRoleId = KeycloakModelUtils.generateId();

        InsertStatement insertRole = new InsertStatement(null, null, roleTableName)
                .addColumnValue("ID", newRoleId)
                .addColumnValue("APP_REALM_CONSTRAINT", applicationId)
                .addColumnValue("APPLICATION_ROLE", true)
                .addColumnValue("NAME", roleName)
                .addColumnValue("REALM_ID", realmId)
                .addColumnValue("APPLICATION", applicationId);

        // Add newly created role to the composite roles of 'realm-admin' role
        InsertStatement insertCompRole = new InsertStatement(null, null, compositeRoleTableName)
                .addColumnValue("COMPOSITE", realmAdminAppRoleId)
                .addColumnValue("CHILD_ROLE", newRoleId);

        statements.add(insertRole);
        statements.add(insertCompRole);
    }

    protected void addDefaultProtocolMappers() throws SQLException, DatabaseException {
        String protocolMapperTableName = database.correctObjectName("PROTOCOL_MAPPER", Table.class);
        String protocolMapperCfgTableName = database.correctObjectName("PROTOCOL_MAPPER_CONFIG", Table.class);

        PreparedStatement statement = jdbcConnection.prepareStatement("select ID, NAME, ALLOWED_CLAIMS_MASK from " + getTableName("CLIENT"));

        try {
            ResultSet resultSet = statement.executeQuery();
            try {
                boolean first = true;
                while (resultSet.next()) {
                    if (first) {
                        confirmationMessage.append("Migrating claimsMask to protocol mappers for clients: ");
                        first = false;
                    }

                    Object acmObj = resultSet.getObject("ALLOWED_CLAIMS_MASK");
                    long mask = (acmObj != null) ? ((Number) acmObj).longValue() : ClaimMask.ALL;

                    MigrationProvider migrationProvider = this.kcSession.getProvider(MigrationProvider.class);
                    List<ProtocolMapperRepresentation> protocolMappers = migrationProvider.getMappersForClaimMask(mask);

                    for (ProtocolMapperRepresentation protocolMapper : protocolMappers) {
                        String mapperId = KeycloakModelUtils.generateId();

                        InsertStatement insert = new InsertStatement(null, null, protocolMapperTableName)
                                .addColumnValue("ID", mapperId)
                                .addColumnValue("PROTOCOL", protocolMapper.getProtocol())
                                .addColumnValue("NAME", protocolMapper.getName())
                                .addColumnValue("CONSENT_REQUIRED", false)
                                .addColumnValue("PROTOCOL_MAPPER_NAME", protocolMapper.getProtocolMapper())
                                .addColumnValue("CLIENT_ID", resultSet.getString("ID"));
                        statements.add(insert);

                        for (Map.Entry<String, String> cfgEntry : protocolMapper.getConfig().entrySet()) {
                            InsertStatement cfgInsert = new InsertStatement(null, null, protocolMapperCfgTableName)
                                    .addColumnValue("PROTOCOL_MAPPER_ID", mapperId)
                                    .addColumnValue("NAME", cfgEntry.getKey())
                                    .addColumnValue("VALUE", cfgEntry.getValue());
                            statements.add(cfgInsert);
                        }

                    }

                    confirmationMessage.append(resultSet.getString("NAME") + ", ");
                }

                // It means that some provider where processed
                if (!first) {
                    confirmationMessage.append(". ");
                }
            } finally {
                resultSet.close();
            }
        } finally {
            statement.close();
        }
    }
}
