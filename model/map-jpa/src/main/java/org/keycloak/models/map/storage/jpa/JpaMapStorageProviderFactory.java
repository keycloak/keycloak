/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.jpa;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;

import org.hibernate.boot.Metadata;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.util.StackUtil;
import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.connections.jpa.util.JpaUtils;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.map.storage.jpa.client.entity.JpaClientEntity;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RoleModel;
import org.keycloak.models.dblock.DBLockProvider;
import org.keycloak.models.map.client.MapProtocolMapperEntity;
import org.keycloak.models.map.client.MapProtocolMapperEntityImpl;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.jpa.client.JpaClientMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.clientscope.JpaClientScopeMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.clientscope.entity.JpaClientScopeEntity;
import org.keycloak.models.map.storage.jpa.hibernate.listeners.JpaEntityVersionListener;
import org.keycloak.models.map.storage.jpa.hibernate.listeners.JpaOptimisticLockingListener;
import org.keycloak.models.map.storage.jpa.role.JpaRoleMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.role.entity.JpaRoleEntity;
import org.keycloak.models.map.storage.jpa.updater.MapJpaUpdaterProvider;
import static org.keycloak.models.map.storage.jpa.updater.MapJpaUpdaterProvider.Status.VALID;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public class JpaMapStorageProviderFactory implements 
        AmphibianProviderFactory<MapStorageProvider>,
        MapStorageProviderFactory,
        EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "jpa-map-storage";

    private volatile EntityManagerFactory emf;
    private Config.Scope config;
    private static final Logger logger = Logger.getLogger(JpaMapStorageProviderFactory.class);

    public final static DeepCloner CLONER = new DeepCloner.Builder()
            //client
            .constructor(JpaClientEntity.class,                 JpaClientEntity::new)
            .constructor(MapProtocolMapperEntity.class,         MapProtocolMapperEntityImpl::new)
            //client-scope
            .constructor(JpaClientScopeEntity.class,            JpaClientScopeEntity::new)
            //role
            .constructor(JpaRoleEntity.class,                   JpaRoleEntity::new)
            .build();

    private static final Map<Class<?>, Function<EntityManager, MapKeycloakTransaction>> MODEL_TO_TX = new HashMap<>();
    static {
        MODEL_TO_TX.put(ClientScopeModel.class,     JpaClientScopeMapKeycloakTransaction::new);
        MODEL_TO_TX.put(ClientModel.class,          JpaClientMapKeycloakTransaction::new);
        MODEL_TO_TX.put(RoleModel.class,            JpaRoleMapKeycloakTransaction::new);
    }

    public MapKeycloakTransaction createTransaction(Class<?> modelType, EntityManager em) {
        return MODEL_TO_TX.get(modelType).apply(em);
    }

    @Override
    public MapStorageProvider create(KeycloakSession session) {
        lazyInit();
        return new JpaMapStorageProvider(this, session, emf.createEntityManager());
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "JPA Map Storage";
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.MAP_STORAGE);
    }

    @Override
    public void close() {
        if (emf != null) {
            emf.close();
        }
    }

    private void lazyInit() {
        if (emf == null) {
            synchronized (this) {
                if (emf == null) {
                    logger.debugf("Initializing JPA connections %s", StackUtil.getShortStackTrace());

                    Map<String, Object> properties = new HashMap<>();

                    String unitName = config.get("persistenceUnitName", "keycloak-jpa-default");

                    String dataSource = config.get("dataSource");
                    if (dataSource != null) {
                        properties.put(AvailableSettings.JPA_NON_JTA_DATASOURCE, dataSource);
                    } else {
                        properties.put(AvailableSettings.JPA_JDBC_URL, config.get("url"));
                        properties.put(AvailableSettings.JPA_JDBC_DRIVER, config.get("driver"));

                        String user = config.get("user");
                        if (user != null) {
                            properties.put(AvailableSettings.JPA_JDBC_USER, user);
                        }
                        String password = config.get("password");
                        if (password != null) {
                            properties.put(AvailableSettings.JPA_JDBC_PASSWORD, password);
                        }
                    }

                    String schema = config.get("schema");
                    if (schema != null) {
                        properties.put(JpaUtils.HIBERNATE_DEFAULT_SCHEMA, schema);
                    }

                    properties.put("hibernate.show_sql", config.getBoolean("showSql", false));
                    properties.put("hibernate.format_sql", config.getBoolean("formatSql", true));
                    properties.put("hibernate.dialect", config.get("driverDialect"));

                    properties.put(
                            "hibernate.integrator_provider",
                            (IntegratorProvider) () -> Collections.singletonList(
                                    new org.hibernate.integrator.spi.Integrator() {

                                        @Override
                                        public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactoryImplementor,
                                                              SessionFactoryServiceRegistry sessionFactoryServiceRegistry) {
                                            final EventListenerRegistry eventListenerRegistry =
                                                    sessionFactoryServiceRegistry.getService( EventListenerRegistry.class );

                                            eventListenerRegistry.appendListeners(EventType.PRE_INSERT, JpaOptimisticLockingListener.INSTANCE);
                                            eventListenerRegistry.appendListeners(EventType.PRE_UPDATE, JpaOptimisticLockingListener.INSTANCE);
                                            eventListenerRegistry.appendListeners(EventType.PRE_DELETE, JpaOptimisticLockingListener.INSTANCE);

                                            eventListenerRegistry.appendListeners(EventType.PRE_INSERT, JpaEntityVersionListener.INSTANCE);
                                            eventListenerRegistry.appendListeners(EventType.PRE_UPDATE, JpaEntityVersionListener.INSTANCE);
                                            eventListenerRegistry.appendListeners(EventType.PRE_DELETE, JpaEntityVersionListener.INSTANCE);
                                        }

                                        @Override
                                        public void disintegrate(SessionFactoryImplementor sessionFactoryImplementor,
                                                                 SessionFactoryServiceRegistry sessionFactoryServiceRegistry) {

                                        }
                                    }
                            )
                    );

                    Integer isolation = config.getInt("isolation");
                    if (isolation != null) {
                        if (isolation < Connection.TRANSACTION_REPEATABLE_READ) {
                            logger.warn("Concurrent requests may not be reliable with transaction level lower than TRANSACTION_REPEATABLE_READ.");
                        }
                        properties.put(AvailableSettings.ISOLATION, String.valueOf(isolation));
                    } else {
                        // default value is TRANSACTION_READ_COMMITTED
                        logger.warn("Concurrent requests may not be reliable with transaction level lower than TRANSACTION_REPEATABLE_READ.");
                    }

                    logger.trace("Creating EntityManagerFactory");
                    this.emf = Persistence.createEntityManagerFactory(unitName, properties);
                    logger.trace("EntityManagerFactory created");
                }
            }
        }
    }

    public void validateAndUpdateSchema(KeycloakSession session, Class<?> modelType) {
        Connection connection = getConnection();

        try {
            if (logger.isDebugEnabled()) printOperationalInfo(connection);

            MapJpaUpdaterProvider updater = session.getProvider(MapJpaUpdaterProvider.class);
            MapJpaUpdaterProvider.Status status = updater.validate(modelType, connection, config.get("schema"));

            if (! status.equals(VALID)) {
                update(modelType, connection, session);
            }
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.warn("Can't close connection", e);
                }
            }
        }
    }

    private Connection getConnection() {
        try {
            String dataSourceLookup = config.get("dataSource");
            if (dataSourceLookup != null) {
                DataSource dataSource = (DataSource) new InitialContext().lookup(dataSourceLookup);
                return dataSource.getConnection();
            } else {
                Class.forName(config.get("driver"));
                return DriverManager.getConnection(
                        StringPropertyReplacer.replaceProperties(config.get("url"), System.getProperties()), 
                        config.get("user"), 
                        config.get("password"));
            }
        } catch (ClassNotFoundException | SQLException | NamingException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    private void printOperationalInfo(Connection connection) {
        try {
            HashMap<String, String> operationalInfo = new LinkedHashMap<>();
            DatabaseMetaData md = connection.getMetaData();
            operationalInfo.put("databaseUrl", md.getURL());
            operationalInfo.put("databaseUser", md.getUserName());
            operationalInfo.put("databaseProduct", md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
            operationalInfo.put("databaseDriver", md.getDriverName() + " " + md.getDriverVersion());

            logger.debugf("Database info: %s", operationalInfo.toString());
        } catch (SQLException e) {
            logger.warn("Unable to prepare operational info due database exception: " + e.getMessage());
        }
    }

    private void update(Class<?> modelType, Connection connection, KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession lockSession) -> {
            // TODO locking tables based on modelType: https://github.com/keycloak/keycloak/issues/9388
            DBLockProvider dbLock = session.getProvider(DBLockProvider.class);
            dbLock.waitForLock(DBLockProvider.Namespace.DATABASE);
            try {
                session.getProvider(MapJpaUpdaterProvider.class).update(modelType, connection, config.get("schema"));
            } finally {
                dbLock.releaseLock();
            }
        });
    }
}
