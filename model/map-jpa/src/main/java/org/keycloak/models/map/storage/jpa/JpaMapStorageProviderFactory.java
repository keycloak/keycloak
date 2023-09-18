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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import liquibase.GlobalConfiguration;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.SessionImpl;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.hibernate.jpa.boot.spi.Bootstrap;
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
import org.keycloak.models.ModelException;
import org.keycloak.models.SingleUseObjectValueModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.locking.GlobalLockProvider;
import org.keycloak.models.map.client.MapProtocolMapperEntity;
import org.keycloak.models.map.client.MapProtocolMapperEntityImpl;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.lock.MapLockEntity;
import org.keycloak.models.map.common.SessionAttributesUtils;
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
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.jpa.authSession.JpaRootAuthenticationSessionMapStorage;
import org.keycloak.models.map.storage.jpa.authSession.entity.JpaAuthenticationSessionEntity;
import org.keycloak.models.map.storage.jpa.authSession.entity.JpaRootAuthenticationSessionEntity;
import org.keycloak.models.map.storage.jpa.authorization.permission.JpaPermissionMapStorage;
import org.keycloak.models.map.storage.jpa.authorization.permission.entity.JpaPermissionEntity;
import org.keycloak.models.map.storage.jpa.authorization.policy.JpaPolicyMapStorage;
import org.keycloak.models.map.storage.jpa.authorization.policy.entity.JpaPolicyEntity;
import org.keycloak.models.map.storage.jpa.authorization.resource.JpaResourceMapStorage;
import org.keycloak.models.map.storage.jpa.authorization.resource.entity.JpaResourceEntity;
import org.keycloak.models.map.storage.jpa.authorization.resourceServer.JpaResourceServerMapStorage;
import org.keycloak.models.map.storage.jpa.authorization.resourceServer.entity.JpaResourceServerEntity;
import org.keycloak.models.map.storage.jpa.authorization.scope.JpaScopeMapStorage;
import org.keycloak.models.map.storage.jpa.authorization.scope.entity.JpaScopeEntity;
import org.keycloak.models.map.storage.jpa.client.JpaClientMapStorage;
import org.keycloak.models.map.storage.jpa.client.entity.JpaClientEntity;
import org.keycloak.models.map.storage.jpa.clientScope.JpaClientScopeMapStorage;
import org.keycloak.models.map.storage.jpa.clientScope.entity.JpaClientScopeEntity;
import org.keycloak.models.map.storage.jpa.event.admin.JpaAdminEventMapStorage;
import org.keycloak.models.map.storage.jpa.event.admin.entity.JpaAdminEventEntity;
import org.keycloak.models.map.storage.jpa.event.auth.JpaAuthEventMapStorage;
import org.keycloak.models.map.storage.jpa.event.auth.entity.JpaAuthEventEntity;
import org.keycloak.models.map.storage.jpa.group.JpaGroupMapStorage;
import org.keycloak.models.map.storage.jpa.group.entity.JpaGroupEntity;
import org.keycloak.models.map.storage.jpa.lock.JpaLockMapStorage;
import org.keycloak.models.map.storage.jpa.lock.entity.JpaLockEntity;
import org.keycloak.models.map.storage.jpa.loginFailure.JpaUserLoginFailureMapStorage;
import org.keycloak.models.map.storage.jpa.loginFailure.entity.JpaUserLoginFailureEntity;
import org.keycloak.models.map.storage.jpa.realm.JpaRealmMapStorage;
import org.keycloak.models.map.storage.jpa.realm.entity.JpaComponentEntity;
import org.keycloak.models.map.storage.jpa.realm.entity.JpaRealmEntity;
import org.keycloak.models.map.storage.jpa.role.JpaRoleMapStorage;
import org.keycloak.models.map.storage.jpa.role.entity.JpaRoleEntity;
import org.keycloak.models.map.storage.jpa.singleUseObject.JpaSingleUseObjectMapStorage;
import org.keycloak.models.map.storage.jpa.singleUseObject.entity.JpaSingleUseObjectEntity;
import org.keycloak.models.map.storage.jpa.updater.MapJpaUpdaterProvider;
import org.keycloak.models.map.storage.jpa.userSession.JpaUserSessionMapStorage;
import org.keycloak.models.map.storage.jpa.userSession.entity.JpaClientSessionEntity;
import org.keycloak.models.map.storage.jpa.userSession.entity.JpaUserSessionEntity;
import org.keycloak.models.map.storage.jpa.user.JpaUserMapStorage;
import org.keycloak.models.map.storage.jpa.user.entity.JpaUserConsentEntity;
import org.keycloak.models.map.storage.jpa.user.entity.JpaUserEntity;
import org.keycloak.models.map.storage.jpa.user.entity.JpaUserFederatedIdentityEntity;
import org.keycloak.models.map.user.MapUserCredentialEntity;
import org.keycloak.models.map.user.MapUserCredentialEntityImpl;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.transaction.JtaTransactionManagerLookup;

public class JpaMapStorageProviderFactory implements
        AmphibianProviderFactory<MapStorageProvider>,
        MapStorageProviderFactory,
        EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "jpa";
    private static final Logger logger = Logger.getLogger(JpaMapStorageProviderFactory.class);

    public static final String HIBERNATE_DEFAULT_SCHEMA = "hibernate.default_schema";

    private static final long DEFAULT_LOCK_TIMEOUT = 10000;

    private volatile EntityManagerFactory emf;
    private final Set<Class<?>> validatedModels = ConcurrentHashMap.newKeySet();
    private Config.Scope config;

    private final int factoryId = SessionAttributesUtils.grabNewFactoryIdentifier();
    private String databaseShortName;

    // Object instances for each single JpaMapStorageProviderFactory instance per model type.
    // Used to synchronize on when validating the model type area.
    private final ConcurrentHashMap<Class<?>, Object> SYNC_MODELS = new ConcurrentHashMap<>();

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
        //user/client session
        .constructor(JpaClientSessionEntity.class,              JpaClientSessionEntity::new)
        .constructor(JpaUserSessionEntity.class,                JpaUserSessionEntity::new)
        //lock
        .constructor(JpaLockEntity.class,                       JpaLockEntity::new)
        .build();

    private static final Map<Class<?>, BiFunction<KeycloakSession, EntityManager, MapStorage>> MODEL_TO_STORE = new HashMap<>();
    static {
        //auth-sessions
        MODEL_TO_STORE.put(RootAuthenticationSessionModel.class,   JpaRootAuthenticationSessionMapStorage::new);
        //authorization
        MODEL_TO_STORE.put(ResourceServer.class,                   JpaResourceServerMapStorage::new);
        MODEL_TO_STORE.put(Resource.class,                         JpaResourceMapStorage::new);
        MODEL_TO_STORE.put(Scope.class,                            JpaScopeMapStorage::new);
        MODEL_TO_STORE.put(PermissionTicket.class,                 JpaPermissionMapStorage::new);
        MODEL_TO_STORE.put(Policy.class,                           JpaPolicyMapStorage::new);
        //clients
        MODEL_TO_STORE.put(ClientModel.class,                      JpaClientMapStorage::new);
        //client-scopes
        MODEL_TO_STORE.put(ClientScopeModel.class,                 JpaClientScopeMapStorage::new);
        //events
        MODEL_TO_STORE.put(AdminEvent.class,                       JpaAdminEventMapStorage::new);
        MODEL_TO_STORE.put(Event.class,                            JpaAuthEventMapStorage::new);
        //groups
        MODEL_TO_STORE.put(GroupModel.class,                       JpaGroupMapStorage::new);
        //realms
        MODEL_TO_STORE.put(RealmModel.class,                       JpaRealmMapStorage::new);
        //roles
        MODEL_TO_STORE.put(RoleModel.class,                        JpaRoleMapStorage::new);
        //single-use-objects
        MODEL_TO_STORE.put(SingleUseObjectValueModel.class,            JpaSingleUseObjectMapStorage::new);
        //user-login-failures
        MODEL_TO_STORE.put(UserLoginFailureModel.class,            JpaUserLoginFailureMapStorage::new);
        //users
        MODEL_TO_STORE.put(UserModel.class,                        JpaUserMapStorage::new);
        //sessions
        MODEL_TO_STORE.put(UserSessionModel.class,                 JpaUserSessionMapStorage::new);
        //locks
        MODEL_TO_STORE.put(MapLockEntity.class,                    JpaLockMapStorage::new);
    }

    private boolean jtaEnabled;
    private JtaTransactionManagerLookup jtaLookup;

    public MapStorage createMapStorage(KeycloakSession session, Class<?> modelType, EntityManager em) {
        return MODEL_TO_STORE.get(modelType).apply(session, em);
    }

    @Override
    public MapStorageProvider create(KeycloakSession session) {
        lazyInit();

        return SessionAttributesUtils.createProviderIfAbsent(session, factoryId, JpaMapStorageProvider.class,
                session1 -> new JpaMapStorageProvider(this, session, PersistenceExceptionConverter.create(session, getEntityManager()), this.jtaEnabled, factoryId));
    }

    protected EntityManager getEntityManager() {
        EntityManager em = emf.createEntityManager();

        // This is a workaround for Hibernate not supporting jakarta.persistence.lock.timeout
        //   config option for Postgresql/CockroachDB - https://hibernate.atlassian.net/browse/HHH-16181
        if ("postgresql".equals(databaseShortName) || "cockroachdb".equals(databaseShortName)) {
            Long lockTimeout = config.getLong("lockTimeout", DEFAULT_LOCK_TIMEOUT);
            if (lockTimeout >= 0) {
                em.unwrap(SessionImpl.class)
                        .doWork(connection -> {
                            // 'SET LOCAL lock_timeout = ...' can't be used with parameters in a prepared statement, leads to an
                            //   'ERROR: syntax error at or near "$1"'
                            // on PostgreSQL.
                            // Using 'set_config()' instead as described here: https://www.postgresql.org/message-id/CAKFQuwbMaoO9%3DVUY1K0Nz5YBDyE6YQ9A_A6ncCxD%2Bt0yK1AxJg%40mail.gmail.com
                            // See https://www.postgresql.org/docs/13/functions-admin.html for the documentation on this function
                            try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT set_config('lock_timeout', ?, true)")) {
                                preparedStatement.setString(1, String.valueOf(lockTimeout));
                                ResultSet resultSet = preparedStatement.executeQuery();
                                resultSet.close();
                            }
                        });
            }
        }
        return em;
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        jtaLookup = (JtaTransactionManagerLookup) factory.getProviderFactory(JtaTransactionManagerLookup.class);
        jtaEnabled = jtaLookup != null && jtaLookup.getTransactionManager() != null;
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

    volatile boolean initialized;

    private void lazyInit() {
        /*
         On Quarkus, and EMF can be created even when the database is currently not available.
         Closing the EMF is not an option, as it is managed by Quarkus.
         Therefore, try to initialize it as often as needed, especially for the addSpecificNamedQueries()
         which would cause failures later if not initialized here.
        */
        if (!initialized) {
            synchronized (this) {
                if (emf == null) {
                    this.emf = createEntityManagerFactory();
                }
                if (!initialized) {
                    JpaMapUtils.addSpecificNamedQueries(emf);

                    // consistency check for transaction handling, as this would lead to data-inconsistencies as changes wouldn't commit when expected
                    if (jtaEnabled && !this.emf.getProperties().get(AvailableSettings.JAKARTA_TRANSACTION_TYPE).equals(PersistenceUnitTransactionType.JTA.name())) {
                        throw new ModelException("Consistency check failed: If Keycloak is run with JTA, the Entity Manager for JPA map storage should be run with JTA as well.");
                    }

                    // consistency check for auto-commit, as this would lead to data-inconsistencies as changes wouldn't roll back when expected
                    EntityManager em = getEntityManager();
                    try {
                        em.unwrap(SessionImpl.class).doWork(connection -> {
                            if (connection.getAutoCommit()) {
                                throw new ModelException("The database connection must not use auto-commit. For Quarkus, auto-commit was off once JTA was enabled for the EntityManager.");
                            }
                        });
                    } finally {
                        em.close();
                    }
                    initialized = true;
                }
            }
        }
    }

    protected EntityManagerFactory createEntityManagerFactory() {
        logger.debugf("Initializing JPA connections %s", StackUtil.getShortStackTrace());

        Map<String, Object> properties = new HashMap<>();
        String dataSource = config.get("dataSource");

        if (dataSource != null) {
            properties.put(AvailableSettings.JAKARTA_NON_JTA_DATASOURCE, dataSource);
        } else {
            properties.put(AvailableSettings.JAKARTA_JDBC_URL, config.get("url"));
            properties.put(AvailableSettings.JAKARTA_JDBC_DRIVER, config.get("driver"));

            String user = config.get("user");
            if (user != null) {
                properties.put(AvailableSettings.JAKARTA_JDBC_USER, user);
            }
            String password = config.get("password");
            if (password != null) {
                properties.put(AvailableSettings.JAKARTA_JDBC_PASSWORD, password);
            }
        }

        String schema = getSchema();
        if (schema != null) {
            properties.put(HIBERNATE_DEFAULT_SCHEMA, schema);
        }

        properties.put("hibernate.show_sql", config.getBoolean("showSql", false));
        properties.put("hibernate.format_sql", config.getBoolean("formatSql", true));
        properties.put("hibernate.dialect", config.get("driverDialect"));
        // metadata contributor to register the json type
        properties.put("hibernate.metadata_builder_contributor", "org.keycloak.models.map.storage.jpa.hibernate.contributor.JsonbMetadataBuilderContributor");
        properties.put(AvailableSettings.JAKARTA_VALIDATION_MODE, ValidationMode.NONE.name());
        Long lockTimeout = config.getLong("lockTimeout", DEFAULT_LOCK_TIMEOUT);
        if (lockTimeout >= 0) {
            // This property does not work for PostgreSQL/CockroachDB - https://hibernate.atlassian.net/browse/HHH-16181
            properties.put(AvailableSettings.JAKARTA_LOCK_TIMEOUT, String.valueOf(lockTimeout));
        } else {
            logger.warnf("Database %s used without lockTimeout option configured. This can result in deadlock where one connection waits for a pessimistic write lock forever.", databaseShortName);
        }

        logger.trace("Creating EntityManagerFactory");
        ParsedPersistenceXmlDescriptor descriptor = PersistenceXmlParser.locateIndividualPersistenceUnit(
                JpaMapStorageProviderFactory.class.getClassLoader()
                        .getResource("default-map-jpa-persistence.xml"));
        EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder(descriptor, properties).build();
        logger.trace("EntityManagerFactory created");

        return emf;
    }

    private String getSchema() {
        String schema = config.get("schema");
        if (schema != null && schema.contains("-") && ! Boolean.parseBoolean(System.getProperty(GlobalConfiguration.PRESERVE_SCHEMA_CASE.getKey()))) {
            System.setProperty(GlobalConfiguration.PRESERVE_SCHEMA_CASE.getKey(), "true");
            logger.warnf("The passed schema '%s' contains a dash. Setting liquibase config option PRESERVE_SCHEMA_CASE to true. See https://github.com/keycloak/keycloak/issues/20870 for more information.", schema);
        }
        return schema;
    }

    protected EntityManagerFactory getEntityManagerFactory() {
        return emf;
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
        if (!this.validatedModels.contains(modelType)) {
            synchronized (SYNC_MODELS.computeIfAbsent(modelType, mc -> new Object())) {
                if (!this.validatedModels.contains(modelType)) {
                    Transaction suspended = null;
                    try {
                        if (jtaEnabled) {
                            suspended = jtaLookup.getTransactionManager().suspend();
                            jtaLookup.getTransactionManager().begin();
                        }

                        Connection connection = getConnection();
                        try {
                            if (logger.isDebugEnabled()) printOperationalInfo(connection);

                            MapJpaUpdaterProvider updater = session.getProvider(MapJpaUpdaterProvider.class);
                            MapJpaUpdaterProvider.Status status = updater.validate(modelType, connection, getSchema());
                            databaseShortName = updater.getDatabaseShortName();

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

                        if (jtaEnabled) {
                            jtaLookup.getTransactionManager().commit();
                        }
                    } catch (SystemException | NotSupportedException | RollbackException | HeuristicMixedException |
                             HeuristicRollbackException e) {
                        if (jtaEnabled) {
                            try {
                                jtaLookup.getTransactionManager().rollback();
                            } catch (SystemException ex) {
                                logger.error("Unable to roll back JTA transaction, e");
                            }
                        }
                        throw new RuntimeException(e);
                    } finally {
                        if (suspended != null) {
                            try {
                                jtaLookup.getTransactionManager().resume(suspended);
                            } catch (InvalidTransactionException | SystemException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    validatedModels.add(modelType);
                }
            }
        }
    }

    protected Connection getConnection() {
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
        if (modelType == MapLockEntity.class) {
            // as the MapLockEntity is used by the MapGlobalLockProvider itself, don't create a global lock for creating that schema
            session.getProvider(MapJpaUpdaterProvider.class).update(modelType, connection, getSchema());
        } else {
            session.getProvider(GlobalLockProvider.class).withLock(modelType.getName(), lockedSession -> {
                lockedSession.getProvider(MapJpaUpdaterProvider.class).update(modelType, connection, getSchema());
                return null;
            });
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("lockTimeout")
                .type("long")
                .defaultValue(10000L)
                .helpText("The maximum time to wait in milliseconds when waiting for acquiring a pessimistic read lock. If set to negative there is no timeout configured.")
                .add().build();
    }
}
