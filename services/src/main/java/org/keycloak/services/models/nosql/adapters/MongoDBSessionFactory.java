package org.keycloak.services.models.nosql.adapters;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.KeycloakSessionFactory;
import org.keycloak.services.models.nosql.api.NoSQL;
import org.keycloak.services.models.nosql.impl.MongoDBImpl;

/**
 * NoSQL implementation based on MongoDB
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoDBSessionFactory implements KeycloakSessionFactory {

    private final MongoClient mongoClient;
    private final NoSQL mongoDB;

    public MongoDBSessionFactory(String host, int port, String dbName) {
        try {
            // TODO: authentication support
            mongoClient = new MongoClient(host, port);

            DB db = mongoClient.getDB(dbName);
            mongoDB = new MongoDBImpl(db);
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
        mongoClient.close();
    }
}
