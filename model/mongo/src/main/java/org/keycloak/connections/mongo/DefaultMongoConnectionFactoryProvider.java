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

package org.keycloak.connections.mongo;

import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.impl.MongoStoreImpl;
import org.keycloak.connections.mongo.impl.context.TransactionMongoStoreInvocationContext;
import org.keycloak.connections.mongo.updater.MongoUpdaterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.dblock.DBLockManager;
import org.keycloak.models.dblock.DBLockProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultMongoConnectionFactoryProvider implements MongoConnectionProviderFactory, ServerInfoAwareProviderFactory {

    enum MigrationStrategy {
        UPDATE, VALIDATE
    }

    // TODO Make it dynamic
    private String[] entities = new String[]{
            "org.keycloak.models.mongo.keycloak.entities.MongoRealmEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoUserEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoGroupEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoClientEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoClientTemplateEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoUserConsentEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoMigrationModelEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoOnlineUserSessionEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoOfflineUserSessionEntity",
            "org.keycloak.models.entities.IdentityProviderEntity",
            "org.keycloak.models.entities.ClientIdentityProviderMappingEntity",
            "org.keycloak.models.entities.RequiredCredentialEntity",
            "org.keycloak.models.entities.CredentialEntity",
            "org.keycloak.models.entities.FederatedIdentityEntity",
            "org.keycloak.models.entities.UserFederationProviderEntity",
            "org.keycloak.models.entities.UserFederationMapperEntity",
            "org.keycloak.models.entities.ProtocolMapperEntity",
            "org.keycloak.models.entities.IdentityProviderMapperEntity",
            "org.keycloak.models.entities.AuthenticationExecutionEntity",
            "org.keycloak.models.entities.AuthenticationFlowEntity",
            "org.keycloak.models.entities.AuthenticatorConfigEntity",
            "org.keycloak.models.entities.RequiredActionProviderEntity",
            "org.keycloak.models.entities.PersistentUserSessionEntity",
            "org.keycloak.models.entities.PersistentClientSessionEntity",
            "org.keycloak.models.entities.ComponentEntity",
            "org.keycloak.authorization.mongo.entities.PolicyEntity",
            "org.keycloak.authorization.mongo.entities.ResourceEntity",
            "org.keycloak.authorization.mongo.entities.ResourceServerEntity",
            "org.keycloak.authorization.mongo.entities.ScopeEntity"
    };

    private static final Logger logger = Logger.getLogger(DefaultMongoConnectionFactoryProvider.class);

    private static final int STATE_BEFORE_INIT = 0;   // Even before MongoClient is created
    private static final int STATE_BEFORE_UPDATE = 1; // Mongo client was created, but DB is not yet updated to last version
    private static final int STATE_AFTER_UPDATE = 2;  // Mongo client was created and DB updated. DB is fully initialized now

    private volatile int state = STATE_BEFORE_INIT;

    private MongoClient client;

    private MongoStore mongoStore;
    private DB db;
    protected Config.Scope config;
    
    private Map<String,String> operationalInfo;

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public DB getDBBeforeUpdate() {
        lazyInitBeforeUpdate();
        return db;
    }

    private void lazyInitBeforeUpdate() {
        if (state == STATE_BEFORE_INIT) {
            synchronized (this) {
                if (state == STATE_BEFORE_INIT) {
                    try {
                        this.client = createMongoClient();
                        String dbName = config.get("db", "keycloak");
                        this.db = client.getDB(dbName);

                        state = STATE_BEFORE_UPDATE;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }


    @Override
    public MongoConnectionProvider create(KeycloakSession session) {
        lazyInit(session);

        TransactionMongoStoreInvocationContext invocationContext = new TransactionMongoStoreInvocationContext(mongoStore);
        session.getTransactionManager().enlist(new MongoKeycloakTransaction(invocationContext));
        return new DefaultMongoConnectionProvider(db, mongoStore, invocationContext);
    }

    private void lazyInit(KeycloakSession session) {
        lazyInitBeforeUpdate();

        if (state == STATE_BEFORE_UPDATE) {
            synchronized (this) {
                if (state == STATE_BEFORE_UPDATE) {
                    try {
                        update(session);
                        this.mongoStore = new MongoStoreImpl(db, getManagedEntities());

                        state = STATE_AFTER_UPDATE;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private void update(KeycloakSession session) {
        MigrationStrategy strategy = getMigrationStrategy();

        MongoUpdaterProvider mongoUpdater = session.getProvider(MongoUpdaterProvider.class);
        if (mongoUpdater == null) {
            throw new RuntimeException("Can't update database: Mongo updater provider not found");
        }

        DBLockProvider dbLock = new DBLockManager(session).getDBLock();
        if (dbLock.hasLock()) {
            updateOrValidateDB(strategy, session, mongoUpdater);
        } else {
            logger.trace("Don't have DBLock retrieved before upgrade. Needs to acquire lock first in separate transaction");

            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {

                @Override
                public void run(KeycloakSession lockSession) {
                    DBLockManager dbLockManager = new DBLockManager(lockSession);
                    DBLockProvider dbLock2 = dbLockManager.getDBLock();
                    dbLock2.waitForLock();
                    try {
                        updateOrValidateDB(strategy, session, mongoUpdater);
                    } finally {
                        dbLock2.releaseLock();
                    }
                }

            });
        }
    }


    private Class[] getManagedEntities() throws ClassNotFoundException {
       Class[] entityClasses = new Class[entities.length];
        for (int i = 0; i < entities.length; i++) {
            entityClasses[i] = getClass().getClassLoader().loadClass(entities[i]);
        }
        return entityClasses;
    }

    protected void updateOrValidateDB(MigrationStrategy strategy, KeycloakSession session, MongoUpdaterProvider mongoUpdater) {
        switch (strategy) {
            case UPDATE:
                mongoUpdater.update(session, db);
                break;
            case VALIDATE:
                mongoUpdater.validate(session, db);
                break;
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }

    @Override
    public String getId() {
        return "default";
    }


    /**
     * Override this method if you want more possibility to configure Mongo client. It can be also used to inject mongo client
     * from different source.
     *
     * This method can assume that "config" is already set and can use it.
     *
     * @return mongoClient instance, which will be shared for whole Keycloak
     *
     * @throws UnknownHostException
     */
    protected MongoClient createMongoClient() throws UnknownHostException {
        operationalInfo = new LinkedHashMap<>();
        String dbName = config.get("db", "keycloak");

        String uriString = config.get("uri");
        if (uriString != null) {
            MongoClientURI uri = new MongoClientURI(uriString);
            MongoClient client = new MongoClient(uri);

            StringBuilder hostsBuilder = new StringBuilder();
            for (int i=0 ; i<uri.getHosts().size() ; i++) {
                if (i!=0) {
                    hostsBuilder.append(", ");
                }
                hostsBuilder.append(uri.getHosts().get(i));
            }
            String hosts = hostsBuilder.toString();

            operationalInfo.put("mongoHosts", hosts);
            operationalInfo.put("mongoDatabaseName", dbName);
            operationalInfo.put("mongoUser", uri.getUsername());

            logger.debugv("Initialized mongo model. host(s): %s, db: %s", uri.getHosts(), dbName);
            return client;
        } else {
            String host = config.get("host", ServerAddress.defaultHost());
            int port = config.getInt("port", ServerAddress.defaultPort());

            String user = config.get("user");
            String password = config.get("password");

            MongoClientOptions clientOptions = getClientOptions();

            MongoClient client;
            if (user != null && password != null) {
                MongoCredential credential = MongoCredential.createCredential(user, dbName, password.toCharArray());
                client = new MongoClient(new ServerAddress(host, port), Collections.singletonList(credential), clientOptions);
            } else {
                client = new MongoClient(new ServerAddress(host, port), clientOptions);
            }

            operationalInfo.put("mongoServerAddress", client.getAddress().toString());
            operationalInfo.put("mongoDatabaseName", dbName);
            operationalInfo.put("mongoUser", user);

            logger.debugv("Initialized mongo model. host: %s, port: %d, db: %s", host, port, dbName);
            return client;
        }
    }

    protected MongoClientOptions getClientOptions() {
        MongoClientOptions.Builder builder = MongoClientOptions.builder();
        checkIntOption("connectionsPerHost", builder);
        checkIntOption("threadsAllowedToBlockForConnectionMultiplier", builder);
        checkIntOption("maxWaitTime", builder);
        checkIntOption("connectTimeout", builder);
        checkIntOption("socketTimeout", builder);
        checkBooleanOption("socketKeepAlive", builder);
        checkBooleanOption("autoConnectRetry", builder);
        if(config.getBoolean("ssl", false)) {
            builder.socketFactory(SSLSocketFactory.getDefault());
        }

        return builder.build();
    }

    protected void checkBooleanOption(String optionName, MongoClientOptions.Builder builder) {
        Boolean val = config.getBoolean(optionName);
        if (val != null) {
            try {
                Method m = MongoClientOptions.Builder.class.getMethod(optionName, boolean.class);
                m.invoke(builder, val);
            } catch (Exception e) {
                throw new IllegalStateException("Problem configuring boolean option " + optionName + " for mongo client. Ensure you used correct value true or false and if this option is supported by mongo driver", e);
            }
        }
    }

    protected void checkIntOption(String optionName, MongoClientOptions.Builder builder) {
        Integer val = config.getInt(optionName);
        if (val != null) {
            try {
                Method m = MongoClientOptions.Builder.class.getMethod(optionName, int.class);
                m.invoke(builder, val);
            } catch (Exception e) {
                throw new IllegalStateException("Problem configuring int option " + optionName + " for mongo client. Ensure you used correct value (number) and if this option is supported by mongo driver", e);
            }
        }
    }
    
    @Override
  	public Map<String,String> getOperationalInfo() {
  		return operationalInfo;
  	}

    private MigrationStrategy getMigrationStrategy() {
        String migrationStrategy = config.get("migrationStrategy");
        if (migrationStrategy == null) {
            // Support 'databaseSchema' for backwards compatibility
            migrationStrategy = config.get("databaseSchema");
        }

        if (migrationStrategy != null) {
            return MigrationStrategy.valueOf(migrationStrategy.toUpperCase());
        } else {
            return MigrationStrategy.UPDATE;
        }
    }

}
