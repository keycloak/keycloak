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

import static org.keycloak.models.map.storage.jpa.updater.MapJpaUpdaterProvider.Status.VALID;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.common.Profile;
import org.keycloak.common.util.StackUtil;
import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.ActionTokenValueModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.dblock.DBLockProvider;
import org.keycloak.models.map.client.MapProtocolMapperEntity;
import org.keycloak.models.map.client.MapProtocolMapperEntityImpl;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.realm.entity.MapAuthenticationExecutionEntity;
import org.keycloak.models.map.realm.entity.MapAuthenticationExecutionEntityImpl;
import org.keycloak.models.map.realm.entity.MapAuthenticationFlowEntity;
import org.keycloak.models.map.realm.entity.MapAuthenticationFlowEntityImpl;
import org.keycloak.models.map.realm.entity.MapAuthenticatorConfigEntity;
import org.keycloak.models.map.realm.entity.MapAuthenticatorConfigEntityImpl;
import org.keycloak.models.map.realm.entity.MapClientInitialAccessEntity;
import org.keycloak.models.map.realm.entity.MapClientInitialAccessEntityImpl;
import org.keycloak.models.map.realm.entity.MapIdentityProviderEntity;
import org.keycloak.models.map.realm.entity.MapIdentityProviderEntityImpl;
import org.keycloak.models.map.realm.entity.MapIdentityProviderMapperEntity;
import org.keycloak.models.map.realm.entity.MapIdentityProviderMapperEntityImpl;
import org.keycloak.models.map.realm.entity.MapOTPPolicyEntity;
import org.keycloak.models.map.realm.entity.MapOTPPolicyEntityImpl;
import org.keycloak.models.map.realm.entity.MapRequiredActionProviderEntity;
import org.keycloak.models.map.realm.entity.MapRequiredActionProviderEntityImpl;
import org.keycloak.models.map.realm.entity.MapRequiredCredentialEntity;
import org.keycloak.models.map.realm.entity.MapRequiredCredentialEntityImpl;
import org.keycloak.models.map.realm.entity.MapWebAuthnPolicyEntity;
import org.keycloak.models.map.realm.entity.MapWebAuthnPolicyEntityImpl;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.jpa.authSession.JpaRootAuthenticationSessionMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.authSession.entity.JpaAuthenticationSessionEntity;
import org.keycloak.models.map.storage.jpa.authSession.entity.JpaRootAuthenticationSessionEntity;
import org.keycloak.models.map.storage.jpa.authorization.permission.JpaPermissionMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.authorization.permission.entity.JpaPermissionEntity;
import org.keycloak.models.map.storage.jpa.authorization.policy.JpaPolicyMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.authorization.policy.entity.JpaPolicyEntity;
import org.keycloak.models.map.storage.jpa.authorization.resource.JpaResourceMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.authorization.resource.entity.JpaResourceEntity;
import org.keycloak.models.map.storage.jpa.authorization.resourceServer.JpaResourceServerMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.authorization.resourceServer.entity.JpaResourceServerEntity;
import org.keycloak.models.map.storage.jpa.authorization.scope.JpaScopeMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.authorization.scope.entity.JpaScopeEntity;
import org.keycloak.models.map.storage.jpa.client.JpaClientMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.client.entity.JpaClientEntity;
import org.keycloak.models.map.storage.jpa.clientscope.JpaClientScopeMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.clientscope.entity.JpaClientScopeEntity;
import org.keycloak.models.map.storage.jpa.event.admin.JpaAdminEventMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.event.admin.entity.JpaAdminEventEntity;
import org.keycloak.models.map.storage.jpa.event.auth.JpaAuthEventMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.event.auth.entity.JpaAuthEventEntity;
import org.keycloak.models.map.storage.jpa.group.JpaGroupMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.group.entity.JpaGroupEntity;
import org.keycloak.models.map.storage.jpa.hibernate.listeners.JpaAutoFlushListener;
import org.keycloak.models.map.storage.jpa.hibernate.listeners.JpaEntityVersionListener;
import org.keycloak.models.map.storage.jpa.hibernate.listeners.JpaOptimisticLockingListener;
import org.keycloak.models.map.storage.jpa.loginFailure.JpaUserLoginFailureMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.loginFailure.entity.JpaUserLoginFailureEntity;
import org.keycloak.models.map.storage.jpa.realm.JpaRealmMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.realm.entity.JpaComponentEntity;
import org.keycloak.models.map.storage.jpa.realm.entity.JpaRealmEntity;
import org.keycloak.models.map.storage.jpa.role.JpaRoleMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.role.entity.JpaRoleEntity;
import org.keycloak.models.map.storage.jpa.singleUseObject.JpaSingleUseObjectMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.singleUseObject.entity.JpaSingleUseObjectEntity;
import org.keycloak.models.map.storage.jpa.updater.MapJpaUpdaterProvider;
import org.keycloak.models.map.storage.jpa.user.JpaUserMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.user.entity.JpaUserConsentEntity;
import org.keycloak.models.map.storage.jpa.user.entity.JpaUserEntity;
import org.keycloak.models.map.storage.jpa.user.entity.JpaUserFederatedIdentityEntity;
import org.keycloak.models.map.user.MapUserCredentialEntity;
import org.keycloak.models.map.user.MapUserCredentialEntityImpl;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.sessions.RootAuthenticationSessionModel;

public class JpaMapStorageProviderFactory implements 
        AmphibianProviderFactory<MapStorageProvider>,
        MapStorageProviderFactory,
        EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "jpa";
    private static final String SESSION_TX_PREFIX = "jpa-map-tx-";
    private static final AtomicInteger ENUMERATOR = new AtomicInteger(0);
    private static final Logger logger = Logger.getLogger(JpaMapStorageProviderFactory.class);

    public static final String HIBERNATE_DEFAULT_SCHEMA = "hibernate.default_schema";

    private volatile EntityManagerFactory emf;
    private final Set<Class<?>> validatedModels = ConcurrentHashMap.newKeySet();
    private Config.Scope config;
    private final String sessionProviderKey;
    private final String sessionTxKey;

    public final static DeepCloner CLONER = new DeepCloner.Builder()
        //auth-sessions
        .constructor(JpaRootAuthenticationSessionEntity.class,  JpaRootAuthenticationSessionEntity::new)
        .constructor(JpaAuthenticationSessionEntity.class,      JpaAuthenticationSessionEntity::new)
        //authorization
        .constructor(JpaResourceServerEntity.class,             JpaResourceServerEntity::new)
        .constructor(JpaResourceEntity.class,                   JpaResourceEntity::new)
        .constructor(JpaScopeEntity.class,                      JpaScopeEntity::new)
        .constructor(JpaPermissionEntity.class,                 JpaPermissionEntity::new)
        .constructor(JpaPolicyEntity.class,                     JpaPolicyEntity::new)
        //clients
        .constructor(JpaClientEntity.class,                     JpaClientEntity::new)
        .constructor(MapProtocolMapperEntity.class,             MapProtocolMapperEntityImpl::new)
        //client-scopes
        .constructor(JpaClientScopeEntity.class,                JpaClientScopeEntity::new)
        //events
        .constructor(JpaAdminEventEntity.class,                 JpaAdminEventEntity::new)
        .constructor(JpaAuthEventEntity.class,                  JpaAuthEventEntity::new)
        //groups
        .constructor(JpaGroupEntity.class,                      JpaGroupEntity::new)
        //realms
        .constructor(JpaRealmEntity.class,                      JpaRealmEntity::new)
        .constructor(JpaComponentEntity.class,                  JpaComponentEntity::new)
        .constructor(MapAuthenticationExecutionEntity.class,    MapAuthenticationExecutionEntityImpl::new)
        .constructor(MapAuthenticationFlowEntity.class,         MapAuthenticationFlowEntityImpl::new)
        .constructor(MapAuthenticatorConfigEntity.class,        MapAuthenticatorConfigEntityImpl::new)
        .constructor(MapClientInitialAccessEntity.class,        MapClientInitialAccessEntityImpl::new)
        .constructor(MapIdentityProviderEntity.class,           MapIdentityProviderEntityImpl::new)
        .constructor(MapIdentityProviderMapperEntity.class,     MapIdentityProviderMapperEntityImpl::new)
        .constructor(MapOTPPolicyEntity.class,                  MapOTPPolicyEntityImpl::new)
        .constructor(MapRequiredActionProviderEntity.class,     MapRequiredActionProviderEntityImpl::new)
        .constructor(MapRequiredCredentialEntity.class,         MapRequiredCredentialEntityImpl::new)
        .constructor(MapWebAuthnPolicyEntity.class,             MapWebAuthnPolicyEntityImpl::new)
        //roles
        .constructor(JpaRoleEntity.class,                       JpaRoleEntity::new)
        //single-use-objects
        .constructor(JpaSingleUseObjectEntity.class,            JpaSingleUseObjectEntity::new)
        //user-login-failures
        .constructor(JpaUserLoginFailureEntity.class,           JpaUserLoginFailureEntity::new)
        //users
        .constructor(JpaUserEntity.class,                       JpaUserEntity::new)
        .constructor(JpaUserConsentEntity.class,                JpaUserConsentEntity::new)
        .constructor(JpaUserFederatedIdentityEntity.class,      JpaUserFederatedIdentityEntity::new)
        .constructor(MapUserCredentialEntity.class,             MapUserCredentialEntityImpl::new)
        .build();

    private static final Map<Class<?>, Function<EntityManager, MapKeycloakTransaction>> MODEL_TO_TX = new HashMap<>();
    static {
        //auth-sessions
        MODEL_TO_TX.put(RootAuthenticationSessionModel.class,   JpaRootAuthenticationSessionMapKeycloakTransaction::new);
        //authorization
        MODEL_TO_TX.put(ResourceServer.class,                   JpaResourceServerMapKeycloakTransaction::new);
        MODEL_TO_TX.put(Resource.class,                         JpaResourceMapKeycloakTransaction::new);
        MODEL_TO_TX.put(Scope.class,                            JpaScopeMapKeycloakTransaction::new);
        MODEL_TO_TX.put(PermissionTicket.class,                 JpaPermissionMapKeycloakTransaction::new);
        MODEL_TO_TX.put(Policy.class,                           JpaPolicyMapKeycloakTransaction::new);
        //clients
        MODEL_TO_TX.put(ClientModel.class,                      JpaClientMapKeycloakTransaction::new);
        //client-scopes
        MODEL_TO_TX.put(ClientScopeModel.class,                 JpaClientScopeMapKeycloakTransaction::new);
        //events
        MODEL_TO_TX.put(AdminEvent.class,                       JpaAdminEventMapKeycloakTransaction::new);
        MODEL_TO_TX.put(Event.class,                            JpaAuthEventMapKeycloakTransaction::new);
        //groups
        MODEL_TO_TX.put(GroupModel.class,                       JpaGroupMapKeycloakTransaction::new);
        //realms
        MODEL_TO_TX.put(RealmModel.class,                       JpaRealmMapKeycloakTransaction::new);
        //roles
        MODEL_TO_TX.put(RoleModel.class,                        JpaRoleMapKeycloakTransaction::new);
        //single-use-objects
        MODEL_TO_TX.put(ActionTokenValueModel.class,            JpaSingleUseObjectMapKeycloakTransaction::new);
        //user-login-failures
        MODEL_TO_TX.put(UserLoginFailureModel.class,            JpaUserLoginFailureMapKeycloakTransaction::new);
        //users
        MODEL_TO_TX.put(UserModel.class,                        JpaUserMapKeycloakTransaction::new);
    }

    public JpaMapStorageProviderFactory() {
        int index = ENUMERATOR.getAndIncrement();
        this.sessionProviderKey = PROVIDER_ID + "-" + index;
        this.sessionTxKey = SESSION_TX_PREFIX + index;
    }

    public MapKeycloakTransaction createTransaction(Class<?> modelType, EntityManager em) {
        return MODEL_TO_TX.get(modelType).apply(em);
    }

    @Override
    public MapStorageProvider create(KeycloakSession session) {
        lazyInit();
        // check the session for a cached provider before creating a new one.
        JpaMapStorageProvider provider = session.getAttribute(this.sessionProviderKey, JpaMapStorageProvider.class);
        if (provider == null) {
            provider = new JpaMapStorageProvider(this, session, emf.createEntityManager(), this.sessionTxKey);
            session.setAttribute(this.sessionProviderKey, provider);
        }
        return provider;
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
        this.validatedModels.clear();
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
                        properties.put(HIBERNATE_DEFAULT_SCHEMA, schema);
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

                                            // replace auto-flush listener
                                            eventListenerRegistry.setListeners(EventType.AUTO_FLUSH, JpaAutoFlushListener.INSTANCE);
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
        /*
        For authz - there is validation run 5 times. For each authz model class separately.
        There is single changlelog "jpa-authz-changelog.xml" used.
        Possible optimization would be to cache: Set<String> validatedModelNames instead of classes. Something like:

        String modelName = ModelEntityUtil.getModelName(modelType);
        if (modelName == null) {
            throw new IllegalStateException("Cannot find changlelog for modelClass " + modelType.getName());
        }

        modelName = modelName.startsWith("authz-") ? "authz" : modelName;

        if (this.validatedModelNames.add(modelName)) {
        */
        if (this.validatedModels.add(modelType)) {
            Connection connection = getConnection();
            try {
                if (logger.isDebugEnabled()) printOperationalInfo(connection);

                MapJpaUpdaterProvider updater = session.getProvider(MapJpaUpdaterProvider.class);
                MapJpaUpdaterProvider.Status status = updater.validate(modelType, connection, config.get("schema"));

                if (!status.equals(VALID)) {
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
