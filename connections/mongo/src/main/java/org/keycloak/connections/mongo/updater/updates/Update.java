package org.keycloak.connections.mongo.updater.updates;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import org.jboss.logging.Logger;

import java.util.Arrays;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class Update {

    protected DB db;

    protected Logger log;

    public abstract String getId();

    public abstract void update() throws ClassNotFoundException;

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

    public void setLog(Logger log) {
        this.log = log;
    }

    public void setDb(DB db) {
        this.db = db;
    }

}
