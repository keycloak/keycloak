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
package org.keycloak.connections.jpa.updater.jdbc;

import org.keycloak.Config;
import org.keycloak.connections.jpa.updater.JpaUpdaterProvider;
import org.keycloak.connections.jpa.updater.JpaUpdaterProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory for the native JDBC {@link JdbcJpaUpdaterProvider}.
 *
 * <p>The only {@link JpaUpdaterProviderFactory} implementation; it replaces the former Liquibase based
 * updater.
 *
 * <p>Supports the {@code index-creation-threshold} option (default {@code 300000}): when a table has more rows
 * than the threshold, index creation is skipped during automatic migration. Zero or a negative value disables
 * the check. Example: {@code --spi-connections-jpa-updater--jdbc--index-creation-threshold=0}.
 *
 * @see JdbcJpaUpdaterProvider
 */
public class JdbcJpaUpdaterProviderFactory implements JpaUpdaterProviderFactory {

    public static final String PROVIDER_ID = "jdbc";

    private long indexCreationThreshold = ChangeSetRunner.DEFAULT_INDEX_THRESHOLD;

    @Override
    public JpaUpdaterProvider create(KeycloakSession session) {
        return new JdbcJpaUpdaterProvider(session, indexCreationThreshold);
    }

    @Override
    public void init(Config.Scope config) {
        indexCreationThreshold = config.getLong("indexCreationThreshold", ChangeSetRunner.DEFAULT_INDEX_THRESHOLD);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}
