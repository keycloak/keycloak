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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.NativeSqlHelper;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.storage.UserStorageProvider;

/**
 * Migration task registered for the historic manifest class name {@code MigrateUserFedToComponent}.
 * Converts all user federation providers (except LDAP, which is handled by PortLdap) to components.
 * Uses the live KeycloakSession to enumerate available UserStorageProvider factories.
 */
public class MigrateUserFedToComponentTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Update 2.5.0.Final";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        if (!NativeSqlHelper.isApplicable(connection, defaultSchema, dialect)) {
            return List.of();
        }

        List<String> stmts = new ArrayList<>();
        // Enumerate all UserStorageProvider factories except LDAP (handled by PortLdap)
        session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(UserStorageProvider.class)
                .map(ProviderFactory::getId)
                .filter(id -> !org.keycloak.models.LDAPConstants.LDAP_PROVIDER.equals(id))
                .forEach(providerId -> {
                    try {
                        AbstractUserFedToComponentHelper.convertFedProviderToComponent(
                                connection, defaultSchema, providerId, null, stmts, dialect);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
        return stmts;
    }
}
