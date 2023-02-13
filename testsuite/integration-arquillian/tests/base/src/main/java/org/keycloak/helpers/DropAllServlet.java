/*
 * JBoss, Home of Professional Open Source
 * Copyright 2019 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.helpers;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * This is not a part of any test app, it is deployment that drops all tables in KeycloakDS.
 *
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
@WebServlet(name = "DropAllServlet", urlPatterns = {"/DropAll"}, loadOnStartup = 1)
public class DropAllServlet extends HttpServlet {

    /**
     * Content of jboss-deployment-structure.xml which has to be used with this servlet.
     */
    public static final String jbossDeploymentStructureContent = "" +
            "<jboss-deployment-structure>" +
            "<deployment>" +
            "<dependencies>" +
            "<module name=\"org.liquibase\" />" +
            "</dependencies>" +
            "</deployment>" +
            "</jboss-deployment-structure>";

    /**
     * Name of war file used to deploy this servlet.
     */
    public static final String WAR_NAME = "dropall.war";

    private static final long serialVersionUID = 1L;

    @Resource(lookup = "java:jboss/datasources/KeycloakDS")
    DataSource dataSource;

    private static Logger log = Logger.getLogger(DropAllServlet.class.getName());

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            Connection connection = dataSource.getConnection();
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            String dbName = database.getShortName();
            if (dbName.contains("postgres")) {
                deleteAllDataPostgresql(connection);
            } else if (dbName.contains("mssql")) {
                deleteAllDataMssql(connection);
            } else {
                Liquibase liquibase = new Liquibase((String) null, null, database);
                liquibase.dropAll();
            }
        } catch (Throwable e) {
            log.error(e);
            throw new ServletException(e);
        }
        log.warn("All Keycloak tables successfully dropped");
    }

    private static final String dropQuery =
            "_drop_table_ ADMIN_EVENT_ENTITY _cascade_;\n" +
            "_drop_table_ ASSOCIATED_POLICY _cascade_;\n" +
            "_drop_table_ AUTHENTICATION_EXECUTION _cascade_;\n" +
            "_drop_table_ AUTHENTICATION_FLOW _cascade_;\n" +
            "_drop_table_ AUTHENTICATOR_CONFIG _cascade_;\n" +
            "_drop_table_ AUTHENTICATOR_CONFIG_ENTRY _cascade_;\n" +
            "_drop_table_ BROKER_LINK _cascade_;\n" +
            "_drop_table_ CLIENT_ATTRIBUTES _cascade_;\n" +
            "_drop_table_ CLIENT_AUTH_FLOW_BINDINGS _cascade_;\n" +
            "_drop_table_ CLIENT_INITIAL_ACCESS _cascade_;\n" +
            "_drop_table_ CLIENT_NODE_REGISTRATIONS _cascade_;\n" +
            "_drop_table_ CLIENT_SCOPE_ATTRIBUTES _cascade_;\n" +
            "_drop_table_ CLIENT_SCOPE_CLIENT _cascade_;\n" +
            "_drop_table_ CLIENT_SCOPE_ROLE_MAPPING _cascade_;\n" +
            "_drop_table_ CLIENT_SESSION_AUTH_STATUS _cascade_;\n" +
            "_drop_table_ CLIENT_SESSION_NOTE _cascade_;\n" +
            "_drop_table_ CLIENT_SESSION_PROT_MAPPER _cascade_;\n" +
            "_drop_table_ CLIENT_SESSION_ROLE _cascade_;\n" +
            "_drop_table_ CLIENT_USER_SESSION_NOTE _cascade_;\n" +
            "_drop_table_ CLIENT_SESSION _cascade_;\n" +
            "_drop_table_ COMPONENT_CONFIG _cascade_;\n" +
            "_drop_table_ COMPONENT _cascade_;\n" +
            "_drop_table_ COMPOSITE_ROLE _cascade_;\n" +
            "_drop_table_ CREDENTIAL _cascade_;\n" +
            "_drop_table_ DEFAULT_CLIENT_SCOPE _cascade_;\n" +
            "_drop_table_ EVENT_ENTITY _cascade_;\n" +
            "_drop_table_ EXAMPLE_COMPANY _cascade_;\n" +
            "_drop_table_ FEDERATED_IDENTITY _cascade_;\n" +
            "_drop_table_ FEDERATED_USER _cascade_;\n" +
            "_drop_table_ FED_USER_ATTRIBUTE _cascade_;\n" +
            "_drop_table_ FED_USER_CONSENT _cascade_;\n" +
            "_drop_table_ FED_USER_CONSENT_CL_SCOPE _cascade_;\n" +
            "_drop_table_ FED_USER_CREDENTIAL _cascade_;\n" +
            "_drop_table_ FED_USER_GROUP_MEMBERSHIP _cascade_;\n" +
            "_drop_table_ FED_USER_REQUIRED_ACTION _cascade_;\n" +
            "_drop_table_ FED_USER_ROLE_MAPPING _cascade_;\n" +
            "_drop_table_ GROUP_ATTRIBUTE _cascade_;\n" +
            "_drop_table_ GROUP_ROLE_MAPPING _cascade_;\n" +
            "_drop_table_ IDENTITY_PROVIDER_CONFIG _cascade_;\n" +
            "_drop_table_ IDENTITY_PROVIDER _cascade_;\n" +
            "_drop_table_ IDP_MAPPER_CONFIG _cascade_;\n" +
            "_drop_table_ IDENTITY_PROVIDER_MAPPER _cascade_;\n" +
            "_drop_table_ MIGRATION_MODEL _cascade_;\n" +
            "_drop_table_ OFFLINE_CLIENT_SESSION _cascade_;\n" +
            "_drop_table_ OFFLINE_USER_SESSION _cascade_;\n" +
            "_drop_table_ POLICY_CONFIG _cascade_;\n" +
            "_drop_table_ PROTOCOL_MAPPER_CONFIG _cascade_;\n" +
            "_drop_table_ PROTOCOL_MAPPER _cascade_;\n" +
            "_drop_table_ CLIENT_SCOPE _cascade_;\n" +
            "_drop_table_ REALM_ATTRIBUTE _cascade_;\n" +
            "_drop_table_ REALM_DEFAULT_GROUPS _cascade_;\n" +
            "_drop_table_ KEYCLOAK_GROUP _cascade_;\n" +
            "_drop_table_ REALM_ENABLED_EVENT_TYPES _cascade_;\n" +
            "_drop_table_ REALM_EVENTS_LISTENERS _cascade_;\n" +
            "_drop_table_ REALM_REQUIRED_CREDENTIAL _cascade_;\n" +
            "_drop_table_ REALM_SMTP_CONFIG _cascade_;\n" +
            "_drop_table_ REALM_SUPPORTED_LOCALES _cascade_;\n" +
            "_drop_table_ REDIRECT_URIS _cascade_;\n" +
            "_drop_table_ REQUIRED_ACTION_CONFIG _cascade_;\n" +
            "_drop_table_ REQUIRED_ACTION_PROVIDER _cascade_;\n" +
            "_drop_table_ RESOURCE_ATTRIBUTE _cascade_;\n" +
            "_drop_table_ RESOURCE_POLICY _cascade_;\n" +
            "_drop_table_ RESOURCE_SCOPE _cascade_;\n" +
            "_drop_table_ RESOURCE_SERVER_PERM_TICKET _cascade_;\n" +
            "_drop_table_ RESOURCE_URIS _cascade_;\n" +
            "_drop_table_ RESOURCE_SERVER_RESOURCE _cascade_;\n" +
            "_drop_table_ ROLE_ATTRIBUTE _cascade_;\n" +
            "_drop_table_ SCOPE_MAPPING _cascade_;\n" +
            "_drop_table_ KEYCLOAK_ROLE _cascade_;\n" +
            "_drop_table_ SCOPE_POLICY _cascade_;\n" +
            "_drop_table_ RESOURCE_SERVER_POLICY _cascade_;\n" +
            "_drop_table_ RESOURCE_SERVER_SCOPE _cascade_;\n" +
            "_drop_table_ RESOURCE_SERVER _cascade_;\n" +
            "_drop_table_ USERNAME_LOGIN_FAILURE _cascade_;\n" +
            "_drop_table_ USER_ATTRIBUTE _cascade_;\n" +
            "_drop_table_ USER_CONSENT_CLIENT_SCOPE _cascade_;\n" +
            "_drop_table_ USER_CONSENT _cascade_;\n" +
            "_drop_table_ USER_FEDERATION_CONFIG _cascade_;\n" +
            "_drop_table_ USER_FEDERATION_MAPPER_CONFIG _cascade_;\n" +
            "_drop_table_ USER_FEDERATION_MAPPER _cascade_;\n" +
            "_drop_table_ USER_FEDERATION_PROVIDER _cascade_;\n" +
            "_drop_table_ REALM _cascade_;\n" +
            "_drop_table_ USER_GROUP_MEMBERSHIP _cascade_;\n" +
            "_drop_table_ USER_REQUIRED_ACTION _cascade_;\n" +
            "_drop_table_ USER_ROLE_MAPPING _cascade_;\n" +
            "_drop_table_ USER_ENTITY _cascade_;\n" +
            "_drop_table_ USER_SESSION_NOTE _cascade_;\n" +
            "_drop_table_ USER_SESSION _cascade_;\n" +
            "_drop_table_ WEB_ORIGINS _cascade_;\n" +
            "_drop_table_ CLIENT _cascade_;\n" +
            "";

    private void deleteAllData(Connection connection, String dropTable, String cascade, boolean executeAlterTable) throws Exception {
        try (Statement statement = connection.createStatement()) {
            String[] queries = dropQuery.split("\n");

            for (String subquery : queries) {
                subquery = subquery.replaceAll("_drop_table_", dropTable).replaceAll("_cascade_", cascade);
                log.info(subquery);
                if (!subquery.isEmpty() && subquery.length() > 3 && (executeAlterTable || !subquery.startsWith("alter table"))) {
                    statement.executeUpdate(subquery);
                }
            }
            connection.commit();
        }
    }

    /**
     * Dirty fix due to bug in Liquibase.
     * @param connection jdbc connection
     * @throws Exception in case anything goes wrong
     */
    private void deleteAllDataMssql(Connection connection) throws Exception {
        deleteAllData(connection, "DELETE FROM", "", true);
    }

    /**
     * Dirty fix due to bug in Liquibase.
     * @param connection jdbc connection
     * @throws Exception in case anything goes wrong
     */
    private void deleteAllDataPostgresql(Connection connection) throws Exception {
        StringBuilder tables = new StringBuilder();
        for (String part : dropQuery.split("\n")) {
            if (part.startsWith("_drop_table_")) {
                tables.append(part.replaceFirst("_drop_table_ ", "").replaceFirst(" _cascade_;", " ,"));
            }
        }
        tables.deleteCharAt(tables.length() - 1);
        try (Statement statement = connection.createStatement()) {
            String query = String.format("truncate table %s cascade;", tables.toString());
            log.infof("Query '%s'", query);
            statement.executeUpdate(query);
            connection.commit();
        }
    }

}
