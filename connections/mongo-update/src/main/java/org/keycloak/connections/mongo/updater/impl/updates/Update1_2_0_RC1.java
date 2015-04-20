package org.keycloak.connections.mongo.updater.impl.updates;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Update1_2_0_RC1 extends Update {

    @Override
    public String getId() {
        return "1.2.0.RC1";
    }

    @Override
    public void update(KeycloakSession session) {
        convertApplicationsToClients();
        convertOAuthClientsToClients();

        db.getCollection("realms").update(new BasicDBObject(), new BasicDBObject("$rename", new BasicDBObject("adminAppId", "clientId")), false, true);

        ensureIndex("userConsents", new String[]{"clientId", "userId"}, true, false);
    }

    private void convertApplicationsToClients() {
        DBCollection applications = db.getCollection("applications");
        applications.update(new BasicDBObject(), new BasicDBObject("$set", new BasicDBObject("consentRequired", false)), false, true);
        applications.update(new BasicDBObject(), new BasicDBObject("$rename", new BasicDBObject("name", "clientId")), false, true);
        renameCollection("applications", "clients");
        log.debugv("Converted applications to clients");

        DBCollection roles = db.getCollection("roles");
        roles.update(new BasicDBObject(), new BasicDBObject("$rename", new BasicDBObject("applicationId", "clientId")), false, true);
        log.debugv("Renamed roles.applicationId to roles.clientId");

        db.getCollection("clients").dropIndex("realmId_1_name_1");
        ensureIndex("clients", new String[]{"realmId", "clientId"}, true, false);

    }

    private void convertOAuthClientsToClients() {
        DBCollection clients = db.getCollection("clients");
        DBCollection oauthClients = db.getCollection("oauthClients");
        oauthClients.update(new BasicDBObject(), new BasicDBObject("$rename", new BasicDBObject("name", "clientId")), false, true);
        oauthClients.update(new BasicDBObject(), new BasicDBObject("$set", new BasicDBObject("consentRequired", true)), false, true);

        DBCursor curs = oauthClients.find();
        while (curs.hasNext()) {
            clients.insert(curs.next());
        }

        oauthClients.drop();
        log.debugv("Converted oauthClients to clients");
    }

}
