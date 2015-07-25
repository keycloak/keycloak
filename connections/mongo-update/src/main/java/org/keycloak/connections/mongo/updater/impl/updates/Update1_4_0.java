package org.keycloak.connections.mongo.updater.impl.updates;

import java.util.HashSet;
import java.util.Map;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Update1_4_0 extends Update {

    @Override
    public String getId() {
        return "1.4.0";
    }

    @Override
    public void update(KeycloakSession session) throws ClassNotFoundException {
        deleteEntries("clientSessions");
        deleteEntries("sessions");

        // Remove warning
        removeField("realms", "authenticators");

        updateUserAttributes();
    }

    private void updateUserAttributes() {
        DBCollection users = db.getCollection("users");
        DBCursor usersCursor = users.find();

        try {
            while (usersCursor.hasNext()) {
                BasicDBObject user = (BasicDBObject) usersCursor.next();

                BasicDBObject attributes = (BasicDBObject) user.get("attributes");
                if (attributes != null) {
                    for (Map.Entry<String, Object> attr : new HashSet<>(attributes.entrySet())) {
                        String attrName = attr.getKey();
                        Object attrValue = attr.getValue();
                        if (attrValue != null && attrValue instanceof String) {
                            BasicDBList asList = new BasicDBList();
                            asList.add(attrValue);
                            attributes.put(attrName, asList);
                        }
                    }

                    user.put("attributes", attributes);

                    users.save(user);
                }
            }
        } finally {
            usersCursor.close();
        }
    }
}
