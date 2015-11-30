package org.keycloak.connections.mongo.updater.impl.updates;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Update1_7_0 extends Update {

    @Override
    public String getId() {
        return "1.7.0";
    }

    @Override
    public void update(KeycloakSession session) throws ClassNotFoundException {
        DBCollection clients = db.getCollection("clients");
        DBCursor clientsCursor = clients.find();

        try {
            while (clientsCursor.hasNext()) {
                BasicDBObject client = (BasicDBObject) clientsCursor.next();

                boolean directGrantsOnly = client.getBoolean("directGrantsOnly", false);
                client.append("standardFlowEnabled", !directGrantsOnly);
                client.append("implicitFlowEnabled", false);
                client.append("directAccessGrantsEnabled", directGrantsOnly);
                client.removeField("directGrantsOnly");

                clients.save(client);
            }
        } finally {
            clientsCursor.close();
        }
    }
}
