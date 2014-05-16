package org.keycloak.audit.mongo;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import org.keycloak.Config;
import org.keycloak.audit.AuditProvider;
import org.keycloak.audit.AuditProviderFactory;
import org.keycloak.provider.ProviderSession;

import java.net.UnknownHostException;
import java.util.Collections;

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
    public AuditProvider create(ProviderSession providerSession) {
        return new MongoAuditProvider(db.getCollection("audit"));
    }

    @Override
    public void init(Config.Scope config) {
        try {
            String host = config.get("host", ServerAddress.defaultHost());
            int port = config.getInt("port", ServerAddress.defaultPort());
            String dbName = config.get("db", "keycloak-audit");
            boolean clearOnStartup = config.getBoolean("clearOnStartup", false);

            String user = config.get("user");
            String password = config.get("password");
            if (user != null && password != null) {
                MongoCredential credential = MongoCredential.createMongoCRCredential(user, dbName, password.toCharArray());
                client = new MongoClient(new ServerAddress(host, port), Collections.singletonList(credential));
            } else {
                client = new MongoClient(host, port);
            }

            client.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
            db = client.getDB(dbName);

            if (clearOnStartup) {
                db.getCollection("audit").drop();
            }
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
