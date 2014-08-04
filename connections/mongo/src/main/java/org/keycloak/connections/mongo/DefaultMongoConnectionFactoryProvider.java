package org.keycloak.connections.mongo;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.impl.MongoStoreImpl;
import org.keycloak.connections.mongo.impl.context.TransactionMongoStoreInvocationContext;
import org.keycloak.models.KeycloakSession;

import java.util.Collections;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultMongoConnectionFactoryProvider implements MongoConnectionProviderFactory {

    // TODO Make configurable
    private String[] entities = new String[]{
            "org.keycloak.models.mongo.keycloak.entities.MongoRealmEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoUserEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity",
            "org.keycloak.models.entities.RequiredCredentialEntity",
            "org.keycloak.models.entities.CredentialEntity",
            "org.keycloak.models.entities.SocialLinkEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoApplicationEntity",
            "org.keycloak.models.mongo.keycloak.entities.MongoOAuthClientEntity",
            "org.keycloak.models.sessions.mongo.entities.MongoUsernameLoginFailureEntity",
            "org.keycloak.models.sessions.mongo.entities.MongoUserSessionEntity",
            "org.keycloak.models.sessions.mongo.entities.MongoClientSessionEntity",
            "org.keycloak.models.entities.UserFederationProviderEntity"
    };

    private static final Logger logger = Logger.getLogger(DefaultMongoConnectionFactoryProvider.class);

    private volatile MongoClient client;

    private MongoStore mongoStore;
    private DB db;
    private Config.Scope config;

    @Override
    public MongoConnectionProvider create(KeycloakSession session) {
        lazyInit();

        TransactionMongoStoreInvocationContext invocationContext = new TransactionMongoStoreInvocationContext(mongoStore);
        session.getTransaction().enlist(new MongoKeycloakTransaction(invocationContext));
        return new DefaultMongoConnectionProvider(db, mongoStore, invocationContext);
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    private void lazyInit() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    try {
                        String host = config.get("host", ServerAddress.defaultHost());
                        int port = config.getInt("port", ServerAddress.defaultPort());
                        String dbName = config.get("db", "keycloak");
                        boolean clearOnStartup = config.getBoolean("clearOnStartup", false);

                        String user = config.get("user");
                        String password = config.get("password");
                        if (user != null && password != null) {
                            MongoCredential credential = MongoCredential.createMongoCRCredential(user, dbName, password.toCharArray());
                            client = new MongoClient(new ServerAddress(host, port), Collections.singletonList(credential));
                        } else {
                            client = new MongoClient(host, port);
                        }

                        this.db = client.getDB(dbName);

                        this.mongoStore = new MongoStoreImpl(db, clearOnStartup, getManagedEntities());

                        logger.infof("Initialized mongo model. host: %s, port: %d, db: %s, clearOnStartup: %b", host, port, dbName, clearOnStartup);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private Class[] getManagedEntities() throws ClassNotFoundException {
       Class[] entityClasses = new Class[entities.length];
        for (int i = 0; i < entities.length; i++) {
            entityClasses[i] = Thread.currentThread().getContextClassLoader().loadClass(entities[i]);
        }
        return entityClasses;
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

}
