package org.keycloak.connections.mongo.updater.impl.updates;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Update1_1_0_Beta1 extends Update {

    @Override
    public String getId() {
        return "1.1.0.Beta1";
    }

    @Override
    public void update(KeycloakSession session) {
        deleteEntries("clientSessions");
        deleteEntries("sessions");

        addRealmCodeSecret();
    }

    private void addRealmCodeSecret() {
        DBCollection realms = db.getCollection("realms");

        DBObject query = new QueryBuilder()
                .and("codeSecret").is(null).get();

        DBCursor objects = realms.find(query);
        while (objects.hasNext()) {
            DBObject object = objects.next();
            object.put("codeSecret", KeycloakModelUtils.generateCodeSecret());
            realms.save(object);

            log.debugv("Added realm.codeSecret, id={0}", object.get("id"));
        }
    }

}
