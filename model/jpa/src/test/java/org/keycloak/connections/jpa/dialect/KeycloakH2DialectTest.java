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

package org.keycloak.connections.jpa.dialect;

import java.sql.DriverManager;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.jboss.logging.Logger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KeycloakH2DialectTest {
    private static final Logger LOG = Logger.getLogger(KeycloakH2DialectTest.class);

    @Test
    public void hibernatePassesDetectedDatabaseVersionToDialect() throws Exception {
        String url = "jdbc:h2:mem:dialect-version";
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.url", url)
                .applySetting("hibernate.connection.username", "sa")
                .applySetting("hibernate.connection.password", "")
                .applySetting("hibernate.dialect", KeycloakH2Dialect.class.getName())
                .build();
        try (var connection = DriverManager.getConnection(url, "sa", "")) {
            var metadata = connection.getMetaData();
            var dialect = registry.getService(JdbcEnvironment.class).getDialect();
            var version = dialect.getVersion();
            LOG.infof("H2 database version: %s; identified dialect version: %s",
                    metadata.getDatabaseProductVersion(), version);
            assertEquals(KeycloakH2Dialect.class, dialect.getClass());
            assertEquals(metadata.getDatabaseMajorVersion(), version.getDatabaseMajorVersion());
            assertEquals(metadata.getDatabaseMinorVersion(), version.getDatabaseMinorVersion());
            // H2 includes the patch version followed by its release date in JDBC metadata.
            String patch = metadata.getDatabaseProductVersion().split("[. ]")[2];
            assertEquals(Integer.parseInt(patch), version.getDatabaseMicroVersion());
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
