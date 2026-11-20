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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.connections.jpa.updater.liquibase.LiquibaseJpaUpdaterProvider;
import org.keycloak.connections.jpa.updater.liquibase.ThreadLocalSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;

import liquibase.change.custom.CustomSqlChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.statement.SqlStatement;
import liquibase.structure.core.Column;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Table;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class CustomKeycloakTask implements CustomSqlChange {

    protected KeycloakSession kcSession;

    protected Database database;
    protected JdbcConnection jdbcConnection;
    protected Connection connection;
    protected StringBuilder confirmationMessage = new StringBuilder();
    protected List<SqlStatement> statements = new ArrayList<SqlStatement>();

    @Override
    public ValidationErrors validate(Database database) {
        return null;
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {

    }

    @Override
    public String getConfirmationMessage() {
        return confirmationMessage.toString();
    }

    @Override
    public void setUp() throws SetupException {
        this.kcSession = ThreadLocalSessionContext.getCurrentSession();
        if (this.kcSession == null) {
            throw new SetupException("Thread bound session is null");
        }
    }

    @Override
    public SqlStatement[] generateStatements(Database database) throws CustomChangeException {
        this.database = database;
        jdbcConnection = (JdbcConnection) database.getConnection();
        connection = jdbcConnection.getWrappedConnection();

        if (isApplicable()) {
            confirmationMessage.append(getTaskId() + ": ");
            generateStatementsImpl();
        } else {
            confirmationMessage.append(getTaskId() + ": no update applicable for this task");
        }

        return statements.toArray(new SqlStatement[statements.size()]);
    }

    protected boolean isApplicable() throws CustomChangeException {
        try {
            String correctedTableName = database.correctObjectName("REALM", Table.class);
            if (SnapshotGeneratorFactory.getInstance().has(new Table().setName(correctedTableName), database)) {
                // We're inside a liquibase managed transaction at this point. Some RDBMS don't like updates to tables
                // that were queried in the same transaction. So we need to create a savepoint and rollback to it so that
                // this select is effectively removed from a transaction and doesn't interfere with an update that will come later.
                Savepoint savepoint = connection.setSavepoint();
                try (Statement st = connection.createStatement(); ResultSet resultSet = st.executeQuery("SELECT ID FROM " + getTableName(correctedTableName))) {
                    return (resultSet.next());
                } finally {
                    connection.rollback(savepoint);
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new CustomChangeException("Failed to check database availability", e);
        }
    }

    /**
     * Checks whether the column exists on the database the task reads from. During manual migration script generation
     * that is the pre-upgrade database, which lacks every table and column added by earlier changesets of the script.
     */
    protected boolean hasColumn(String tableName, String columnName) throws CustomChangeException {
        try {
            // The schema is required: the MySQL column snapshot dereferences it and fails with a NullPointerException otherwise
            Table table = new Table().setName(database.correctObjectName(tableName, Table.class));
            table.setSchema(new Schema((String) null, database.getDefaultSchemaName()));
            Column column = new Column().setRelation(table).setName(database.correctObjectName(columnName, Column.class));
            return SnapshotGeneratorFactory.getInstance().has(column, database);
        } catch (Exception e) {
            throw new CustomChangeException("Failed to check for existence of column " + tableName + "." + columnName, e);
        }
    }

    /**
     * Checks whether the table exists on the database the task reads from, see {@link #hasColumn(String, String)}.
     */
    protected boolean hasTable(String tableName) throws CustomChangeException {
        try {
            return SnapshotGeneratorFactory.getInstance().has(new Table().setName(database.correctObjectName(tableName, Table.class)), database);
        } catch (Exception e) {
            throw new CustomChangeException("Failed to check for existence of table " + tableName, e);
        }
    }

    /**
     * Link of an identity provider to its organization, as stored before 26.8 when an IdP carried exactly one organization.
     */
    protected record IdpOrganizationLink(String idpInternalId, String realmId, String idpAlias, String organizationId) {
    }

    /**
     * Reads the identity provider to organization links of the database the task reads from. Since 26.0.0 they are in
     * {@code IDENTITY_PROVIDER.ORGANIZATION_ID}. Before that they are in the {@code kc.org} entry of
     * {@code IDENTITY_PROVIDER_CONFIG}, which is what the pre-upgrade database still holds when a manual migration script
     * is generated from a 25.x starting version: the statements of the 26.0.0 migration are only part of the script.
     */
    protected List<IdpOrganizationLink> getIdpOrganizationLinks() throws CustomChangeException {
        String idpTable = database.correctObjectName("IDENTITY_PROVIDER", Table.class);
        String sql;
        if (hasColumn("IDENTITY_PROVIDER", "ORGANIZATION_ID")) {
            sql = "SELECT ip.INTERNAL_ID, ip.REALM_ID, ip.PROVIDER_ALIAS, ip.ORGANIZATION_ID FROM " + getTableName(idpTable) +
                    " ip WHERE ip.ORGANIZATION_ID IS NOT NULL";
        } else {
            String configTable = database.correctObjectName("IDENTITY_PROVIDER_CONFIG", Table.class);
            sql = "SELECT ip.INTERNAL_ID, ip.REALM_ID, ip.PROVIDER_ALIAS, c.VALUE FROM " + getTableName(idpTable) + " ip" +
                    " JOIN " + getTableName(configTable) + " c ON c.IDENTITY_PROVIDER_ID = ip.INTERNAL_ID" +
                    " WHERE c.NAME = '" + OrganizationModel.ORGANIZATION_ATTRIBUTE + "'";
        }

        List<IdpOrganizationLink> links = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String organizationId = rs.getString(4);
                if (organizationId != null && !organizationId.isEmpty()) {
                    links.add(new IdpOrganizationLink(rs.getString(1), rs.getString(2), rs.getString(3), organizationId));
                }
            }
        } catch (Exception e) {
            throw new CustomChangeException("Failed to read the identity provider organization links", e);
        }
        return links;
    }

    /**
     * It's supposed to fill SQL statements to the "statements" variable and fill "confirmationMessage"
     */
    protected abstract void generateStatementsImpl() throws CustomChangeException;

    protected abstract String getTaskId();

    // get Table name for sql selects
    protected String getTableName(String tableName) {
        String correctedSchemaName = database.escapeObjectName(database.getDefaultSchemaName(), Schema.class);
        return LiquibaseJpaUpdaterProvider.getTable(tableName, correctedSchemaName);
    }
}
