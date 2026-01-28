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

import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.UserStorageProvider;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.DeleteStatement;
import liquibase.statement.core.InsertStatement;
import liquibase.structure.core.Table;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractUserFedToComponent extends CustomKeycloakTask {
    private final Logger logger = Logger.getLogger(getClass());
    protected void convertFedProviderToComponent(String providerId, String newMapperType) throws CustomChangeException {
        try {
            PreparedStatement statement = jdbcConnection.prepareStatement("select ID, REALM_ID, PRIORITY, DISPLAY_NAME, FULL_SYNC_PERIOD, CHANGED_SYNC_PERIOD, LAST_SYNC from " + getTableName("USER_FEDERATION_PROVIDER") + " WHERE PROVIDER_NAME=?");
            statement.setString(1, providerId);

            try {
                ResultSet resultSet = statement.executeQuery();
                try {
                    while (resultSet.next()) {
                        int index = 1;
                        String id = resultSet.getString(index++);
                        String realmId = resultSet.getString(index++);
                        int priority = resultSet.getInt(index++);
                        String displayName = resultSet.getString(index++);
                        int fullSyncPeriod = resultSet.getInt(index++);
                        int changedSyncPeriod = resultSet.getInt(index++);
                        int lastSync = resultSet.getInt(index++);


                        InsertStatement insertComponent = new InsertStatement(null, null, database.correctObjectName("COMPONENT", Table.class))
                                .addColumnValue("ID", id)
                                .addColumnValue("REALM_ID", realmId)
                                .addColumnValue("PARENT_ID", realmId)
                                .addColumnValue("NAME", displayName)
                                .addColumnValue("PROVIDER_ID", providerId)
                                .addColumnValue("PROVIDER_TYPE", UserStorageProvider.class.getName());

                        statements.add(insertComponent);

                        statements.add(componentConfigStatement(id, "priority", Integer.toString(priority)));
                        statements.add(componentConfigStatement(id, "fullSyncPeriod", Integer.toString(fullSyncPeriod)));
                        statements.add(componentConfigStatement(id, "changedSyncPeriod", Integer.toString(changedSyncPeriod)));
                        statements.add(componentConfigStatement(id, "lastSync", Integer.toString(lastSync)));
                        PreparedStatement configStatement = jdbcConnection.prepareStatement("select name, VALUE from " + getTableName("USER_FEDERATION_CONFIG") + " WHERE USER_FEDERATION_PROVIDER_ID=?");
                        configStatement.setString(1, id);
                        try {
                            ResultSet configSet = configStatement.executeQuery();
                            try {
                                while (configSet.next()) {
                                    String name = configSet.getString(1);
                                    String value = configSet.getString(2);
                                    //logger.info("adding component config: " + name + ": " + value);
                                    statements.add(componentConfigStatement(id, name, value));
                                }
                            } finally {
                                configSet.close();
                            }
                        } finally {
                            configStatement.close();
                        }

                        if (newMapperType != null) {
                            convertFedMapperToComponent(realmId, id, newMapperType);
                        }

                        DeleteStatement configDelete = new DeleteStatement(null, null, database.correctObjectName("USER_FEDERATION_CONFIG", Table.class));
                        configDelete.setWhere("USER_FEDERATION_PROVIDER_ID=?");
                        configDelete.addWhereParameters(id);

                        statements.add(configDelete);
                        DeleteStatement deleteStatement = new DeleteStatement(null, null, database.correctObjectName("USER_FEDERATION_PROVIDER", Table.class));
                        deleteStatement.setWhere("ID=?");
                        deleteStatement.addWhereParameters(id);
                        statements.add(deleteStatement);

                    }
                } finally {
                    resultSet.close();
                }
            } finally {
                statement.close();
            }

            confirmationMessage.append("Updated " + statements.size() + " records in USER_FEDERATION_PROVIDER table for " + providerId + " conversion to component model");
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when updating data from previous version", e);
        }
    }

    protected InsertStatement componentConfigStatement(String componentId, String name, String value) {
        return new InsertStatement(null, null, database.correctObjectName("COMPONENT_CONFIG", Table.class))
                .addColumnValue("ID", KeycloakModelUtils.generateId())
                .addColumnValue("COMPONENT_ID", componentId)
                .addColumnValue("NAME", name)
                .addColumnValue("VALUE", value);
    }

    protected void convertFedMapperToComponent(String realmId, String parentId, String newMapperType) throws CustomChangeException {
        try {
            PreparedStatement statement = jdbcConnection.prepareStatement("select ID, NAME, FEDERATION_MAPPER_TYPE from " + getTableName("USER_FEDERATION_MAPPER") + " WHERE FEDERATION_PROVIDER_ID=?");
            statement.setString(1, parentId);

            try {
                ResultSet resultSet = statement.executeQuery();
                try {
                    while (resultSet.next()) {
                        String id = resultSet.getString(1);
                        String mapperName = resultSet.getString(2);
                        String fedMapperType = resultSet.getString(3);

                        InsertStatement insertComponent = new InsertStatement(null, null, database.correctObjectName("COMPONENT", Table.class))
                                .addColumnValue("ID", id)
                                .addColumnValue("REALM_ID", realmId)
                                .addColumnValue("PARENT_ID", parentId)
                                .addColumnValue("NAME", mapperName)
                                .addColumnValue("PROVIDER_ID", fedMapperType)
                                .addColumnValue("PROVIDER_TYPE", newMapperType);

                        statements.add(insertComponent);



                        PreparedStatement configStatement = jdbcConnection.prepareStatement("select name, VALUE from " + getTableName("USER_FEDERATION_MAPPER_CONFIG") + " WHERE USER_FEDERATION_MAPPER_ID=?");
                        configStatement.setString(1, id);
                        try {
                            ResultSet configSet = configStatement.executeQuery();
                            try {
                                while (configSet.next()) {
                                    String name = configSet.getString(1);
                                    String value = configSet.getString(2);
                                    statements.add(componentConfigStatement(id, name, value));
                                }
                            } finally {
                                configSet.close();
                            }
                        } finally {
                            configStatement.close();
                        }
                        DeleteStatement configDelete = new DeleteStatement(null, null, database.correctObjectName("USER_FEDERATION_MAPPER_CONFIG", Table.class));
                        configDelete.setWhere("USER_FEDERATION_MAPPER_ID=?");
                        configDelete.addWhereParameters(id);
                        statements.add(configDelete);
                        DeleteStatement deleteStatement = new DeleteStatement(null, null, database.correctObjectName("USER_FEDERATION_MAPPER", Table.class));
                        deleteStatement.setWhere("ID=?");
                        deleteStatement.addWhereParameters(id);
                        statements.add(deleteStatement);


                    }
                } finally {
                    resultSet.close();
                }
            } finally {
                statement.close();
            }

            confirmationMessage.append("Updated " + statements.size() + " records in USER_FEDERATION_MAPPER table for " + parentId + " conversion to component model");
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when updating data from previous version", e);
        }
    }

}
