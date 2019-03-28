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
import liquibase.exception.LiquibaseException;
import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

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
            Liquibase liquibase = new Liquibase((String) null, null, database);
            liquibase.dropAll();
        } catch (SQLException | LiquibaseException e) {
            log.error(e);
            throw new ServletException(e);
        }
        log.warn("All Keycloak tables successfully dropped");
    }
}
