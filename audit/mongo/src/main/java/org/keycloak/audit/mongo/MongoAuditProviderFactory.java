package org.keycloak.audit.mongo;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import org.keycloak.Config;
import org.keycloak.audit.AuditProvider;
import org.keycloak.audit.AuditProviderFactory;
import org.keycloak.audit.EventType;
import org.keycloak.provider.ProviderSession;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MongoAuditProviderFactory implements AuditProviderFactory {

    public static final String ID = "mongo";
    private MongoClient client;
    private DB db;

    private Set<EventType> includedEvents = new HashSet<EventType>();

    @Override
    public AuditProvider create(ProviderSession providerSession) {
        return new MongoAuditProvider(db.getCollection("audit"), includedEvents);
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

        String[] include = config.getArray("include-events");
        if (include != null) {
            for (String i : include) {
                includedEvents.add(EventType.valueOf(i.toUpperCase()));
            }
        } else {
            for (EventType i : EventType.values()) {
                includedEvents.add(i);
            }
        }

        String[] exclude = config.getArray("exclude-events");
        if (exclude != null) {
            for (String e : exclude) {
                includedEvents.remove(EventType.valueOf(e.toUpperCase()));
            }
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
