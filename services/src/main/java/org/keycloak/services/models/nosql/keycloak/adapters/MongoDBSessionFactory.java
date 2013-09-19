package org.keycloak.services.models.nosql.keycloak.adapters;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.KeycloakSessionFactory;
import org.keycloak.services.models.nosql.api.NoSQL;
import org.keycloak.services.models.nosql.api.NoSQLObject;
import org.keycloak.services.models.nosql.api.query.NoSQLQuery;
import org.keycloak.services.models.nosql.api.query.NoSQLQueryBuilder;
import org.keycloak.services.models.nosql.keycloak.data.ApplicationData;
import org.keycloak.services.models.nosql.keycloak.data.RealmData;
import org.keycloak.services.models.nosql.keycloak.data.RequiredCredentialData;
import org.keycloak.services.models.nosql.keycloak.data.RoleData;
import org.keycloak.services.models.nosql.keycloak.data.SocialLinkData;
import org.keycloak.services.models.nosql.keycloak.data.UserData;
import org.keycloak.services.models.nosql.impl.MongoDBImpl;
import org.keycloak.services.models.nosql.impl.MongoDBQueryBuilder;
import org.keycloak.services.models.nosql.keycloak.data.credentials.OTPData;
import org.keycloak.services.models.nosql.keycloak.data.credentials.PasswordData;

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
            ApplicationData.class
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
