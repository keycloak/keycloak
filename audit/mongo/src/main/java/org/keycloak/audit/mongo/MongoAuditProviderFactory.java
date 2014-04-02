package org.keycloak.audit.mongo;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import org.keycloak.audit.AuditProvider;
import org.keycloak.audit.AuditProviderFactory;

import java.net.UnknownHostException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MongoAuditProviderFactory implements AuditProviderFactory {

    private static final String MONGO_HOST = "keycloak.audit.mongo.host";
    private static final String MONGO_PORT = "keycloak.audit.mongo.port";
    private static final String MONGO_DB_NAME = "keycloak.audit.mongo.db";

    public static final String ID = "mongo";
    private MongoClient client;
    private DB db;

    @Override
    public AuditProvider create() {
        return new MongoAuditProvider(db.getCollection("audit"));
    }

    @Override
    public void init() {
        try {
            client = new MongoClient(System.getProperty(MONGO_HOST, "localhost"), Integer.parseInt(System.getProperty(MONGO_PORT, "27017")));
            client.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
            db = client.getDB(System.getProperty(MONGO_DB_NAME, "keycloak-audit"));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public String getId() {
        return ID;
    }

}
