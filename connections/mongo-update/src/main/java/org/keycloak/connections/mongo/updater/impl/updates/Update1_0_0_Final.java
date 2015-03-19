package org.keycloak.connections.mongo.updater.impl.updates;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.keycloak.connections.mongo.updater.impl.DefaultMongoUpdaterProvider;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Update1_0_0_Final extends Update {

    @Override
    public String getId() {
        return "1.0.0.Final";
    }

    @Override
    public void update(KeycloakSession session) throws ClassNotFoundException {
        DBCollection realmsCollection = db.getCollection("realms");
        realmsCollection.ensureIndex(new BasicDBObject("name", 1), new BasicDBObject("unique", true));

        DefaultMongoUpdaterProvider.log.debugv("Created collection {0}", "realms");

        createCollection("users");
        ensureIndex("users", new String[] { "realmId", "username"}, true, false);
        ensureIndex("users", "emailIndex", true, true);

        createCollection("roles");
        ensureIndex("roles", "nameIndex", true, false);

        createCollection("applications");
        ensureIndex("applications", new String[]{"realmId", "name"}, true, false);

        createCollection("oauthClients");
        ensureIndex("oauthClients", new String[] { "realmId", "name"}, true, false);

        createCollection("userFailures");

        createCollection("sessions");

        createCollection("clientSessions");
    }

}
