/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.cluster.jpa;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.cluster.ClusterEventStoreProvider;
import org.keycloak.cluster.ClusterEventStoreProviderFactory;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import org.jboss.logging.Logger;

public class JpaClusterEventStoreProviderFactory implements ClusterEventStoreProviderFactory {

    private static final Logger logger = Logger.getLogger(JpaClusterEventStoreProviderFactory.class);

    public static final String ID = "jpa";

    private boolean useListenNotify = true;
    private volatile boolean pgNotifyEnabled;

    @Override
    public ClusterEventStoreProvider create(KeycloakSession session) {
        return new JpaClusterEventStoreProvider(session, pgNotifyEnabled);
    }

    @Override
    public void init(Config.Scope config) {
        useListenNotify = config.getBoolean("useListenNotify", true);
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                    .name("useListenNotify")
                    .type("boolean")
                    .helpText("When enabled and the database is PostgreSQL, call pg_notify() after persisting cluster events "
                            + "to allow near-instant delivery via LISTEN/NOTIFY. Set to false to rely on polling only.")
                    .defaultValue(true)
                    .add()
                .build();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        if (!useListenNotify) {
            return;
        }
        JpaConnectionProviderFactory jpaFactory = (JpaConnectionProviderFactory) factory.getProviderFactory(JpaConnectionProvider.class);
        if (jpaFactory != null) {
            try (Connection connection = jpaFactory.getConnection()) {
                pgNotifyEnabled = PostgresClusterEventListener.isSupported(connection);
                if (!pgNotifyEnabled) {
                    logger.warn("This DB connection class does not support NOTIFY/LISTEN");
                }
            } catch (SQLException e) {
                logger.warn("Unable to determine if the database supports NOTIFY/LISTEN, defaulting to not using it", e);
                pgNotifyEnabled = false;
            }
        }
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        return Set.of(JpaConnectionProvider.class);
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }
}
