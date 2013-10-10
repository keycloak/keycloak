package org.keycloak.models.mongo.keycloak.adapters;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.mongo.api.NoSQL;
import org.keycloak.models.mongo.api.NoSQLObject;
import org.keycloak.models.mongo.keycloak.data.ApplicationData;
import org.keycloak.models.mongo.keycloak.data.OAuthClientData;
import org.keycloak.models.mongo.keycloak.data.RealmData;
import org.keycloak.models.mongo.keycloak.data.RequiredCredentialData;
import org.keycloak.models.mongo.keycloak.data.RoleData;
import org.keycloak.models.mongo.keycloak.data.SocialLinkData;
import org.keycloak.models.mongo.keycloak.data.UserData;
import org.keycloak.models.mongo.impl.MongoDBImpl;
import org.keycloak.models.mongo.keycloak.data.credentials.OTPData;
import org.keycloak.models.mongo.keycloak.data.credentials.PasswordData;

/**
 * NoSQL implementation based on MongoDB
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoDBSessionFactory implements KeycloakSessionFactory {
    protected static final Logger logger = Logger.getLogger(MongoDBSessionFactory.class);

    private static final Class<? extends NoSQLObject>[] MANAGED_DATA_TYPES = (Class<? extends NoSQLObject>[])new Class<?>[] {
            RealmData.class,
            UserData.class,
            RoleData.class,
            RequiredCredentialData.class,
            PasswordData.class,
            OTPData.class,
            SocialLinkData.class,
            ApplicationData.class,
            OAuthClientData.class
    };

    private final MongoClient mongoClient;
    private final NoSQL mongoDB;

    public MongoDBSessionFactory(String host, int port, String dbName, boolean dropDatabaseOnStartup) {
        logger.info(String.format("Going to use MongoDB database. host: %s, port: %d, databaseName: %s, removeAllObjectsAtStartup: %b", host, port, dbName, dropDatabaseOnStartup));
        try {
            // TODO: authentication support
            mongoClient = new MongoClient(host, port);

            DB db = mongoClient.getDB(dbName);
            mongoDB = new MongoDBImpl(db, dropDatabaseOnStartup, MANAGED_DATA_TYPES);

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public KeycloakSession createSession() {
        return new NoSQLSession(mongoDB);
    }

    @Override
    public void close() {
        logger.info("Closing MongoDB client");
        mongoClient.close();
    }
}
