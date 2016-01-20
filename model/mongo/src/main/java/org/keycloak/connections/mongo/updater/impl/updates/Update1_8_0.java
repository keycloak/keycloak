package org.keycloak.connections.mongo.updater.impl.updates;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;
import org.keycloak.hash.Pbkdf2PasswordHashProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserCredentialModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Update1_8_0 extends Update {

    @Override
    public String getId() {
        return "1.8.0";
    }

    @Override
    public void update(KeycloakSession session) {
        BasicDBList orArgs = new BasicDBList();
        orArgs.add(new BasicDBObject("type", UserCredentialModel.PASSWORD));
        orArgs.add(new BasicDBObject("type", UserCredentialModel.PASSWORD_HISTORY));

        BasicDBObject elemMatch = new BasicDBObject("$or", orArgs);
        elemMatch.put("algorithm", new BasicDBObject("$exists", false));

        BasicDBObject query = new BasicDBObject("credentials", new BasicDBObject("$elemMatch", elemMatch));

        BasicDBObject update = new BasicDBObject("$set", new BasicDBObject("credentials.$.algorithm", Pbkdf2PasswordHashProvider.ID));

        DBCollection users = db.getCollection("users");

        // Not sure how to do in single query
        int countModified = 1;
        while (countModified > 0) {
            WriteResult wr = users.update(query, update, false, true);
            countModified = wr.getN();
            log.debugf("%d credentials modified in current iteration during upgrade to 1.8", countModified);
        }
    }
}
