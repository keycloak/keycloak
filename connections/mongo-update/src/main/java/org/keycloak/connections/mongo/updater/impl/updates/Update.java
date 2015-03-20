package org.keycloak.connections.mongo.updater.impl.updates;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.Arrays;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class Update {

    protected DB db;

    protected Logger log;

    public abstract String getId();

    public abstract void update(KeycloakSession session) throws ClassNotFoundException;

    protected DBCollection createCollection(String name) {
        if (db.collectionExists(name)) {
            throw new RuntimeException("Failed to create collection {0}: collection already exists");
        }

        DBCollection col = db.getCollection(name);
        log.debugv("Created collection {0}", name);
        return col;
    }

    protected void ensureIndex(String name, String field, boolean unique, boolean sparse) {
        ensureIndex(name, new String[]{field}, unique, sparse);
    }

    protected void ensureIndex(String name, String[] fields, boolean unique, boolean sparse) {
        DBCollection col = db.getCollection(name);

        BasicDBObject o = new BasicDBObject();
        for (String f : fields) {
            o.append(f, 1);
        }

        col.ensureIndex(o, new BasicDBObject("unique", unique).append("sparse", sparse));
        log.debugv("Created index {0}, fields={1}, unique={2}, sparse={3}", name, Arrays.toString(fields), unique, sparse);
    }

    protected void deleteEntries(String collection) {
        db.getCollection(collection).remove(new BasicDBObject());
        log.debugv("Deleted entries from {0}", collection);
    }

    protected String insertApplicationRole(DBCollection roles, String roleName, String applicationId) {
        BasicDBObject role = new BasicDBObject();
        String roleId = KeycloakModelUtils.generateId();
        role.append("_id", roleId);
        role.append("name", roleName);
        role.append("applicationId", applicationId);
        role.append("nameIndex", applicationId + "//" + roleName);
        roles.insert(role);
        return roleId;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    public void setDb(DB db) {
        this.db = db;
    }

}
