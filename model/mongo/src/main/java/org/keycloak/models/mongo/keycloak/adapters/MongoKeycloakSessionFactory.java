package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.entities.AuthenticationLinkEntity;
import org.keycloak.models.entities.AuthenticationProviderEntity;
import org.keycloak.models.entities.CredentialEntity;
import org.keycloak.models.entities.RequiredCredentialEntity;
import org.keycloak.models.entities.SocialLinkEntity;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.impl.MongoStoreImpl;
import org.keycloak.models.mongo.keycloak.entities.MongoApplicationEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoOAuthClientEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRealmEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserSessionEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUsernameLoginFailureEntity;
import org.keycloak.provider.ProviderSession;

import java.net.UnknownHostException;
import java.util.Collections;

/**
 * KeycloakSessionFactory implementation based on MongoDB
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoKeycloakSessionFactory implements KeycloakSessionFactory {
    protected static final Logger logger = Logger.getLogger(MongoKeycloakSessionFactory.class);

    private static final Class<?>[] MANAGED_ENTITY_TYPES = (Class<?>[]) new Class<?>[]{
            MongoRealmEntity.class,
            MongoUserEntity.class,
            MongoRoleEntity.class,
            RequiredCredentialEntity.class,
            AuthenticationProviderEntity.class,
            CredentialEntity.class,
            SocialLinkEntity.class,
            AuthenticationLinkEntity.class,
            MongoApplicationEntity.class,
            MongoOAuthClientEntity.class,
            MongoUsernameLoginFailureEntity.class,
            MongoUserSessionEntity.class
    };

    private MongoClient client;

    private MongoStore mongoStore;

    @Override
    public String getId() {
        return "mongo";
    }

    @Override
    public void init(Config.Scope config) {
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

            DB db = client.getDB(dbName);

            this.mongoStore = new MongoStoreImpl(db, clearOnStartup, MANAGED_ENTITY_TYPES);

            logger.infof("Initialized mongo model. host: %s, port: %d, db: %s, clearOnStartup: %b", host, port, dbName, clearOnStartup);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public KeycloakSession create(ProviderSession providerSession) {
        return new MongoKeycloakSession(mongoStore);
    }

    @Override
    public void close() {
        this.client.close();
    }

}

