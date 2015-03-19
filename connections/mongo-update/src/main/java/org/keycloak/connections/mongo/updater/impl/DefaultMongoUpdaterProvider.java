package org.keycloak.connections.mongo.updater.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import org.jboss.logging.Logger;
import org.keycloak.connections.mongo.updater.MongoUpdaterProvider;
import org.keycloak.connections.mongo.updater.impl.updates.Update;
import org.keycloak.connections.mongo.updater.impl.updates.Update1_0_0_Final;
import org.keycloak.connections.mongo.updater.impl.updates.Update1_1_0_Beta1;
import org.keycloak.connections.mongo.updater.impl.updates.Update1_2_0_Beta1;
import org.keycloak.models.KeycloakSession;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultMongoUpdaterProvider implements MongoUpdaterProvider {

    public static final Logger log = Logger.getLogger(DefaultMongoUpdaterProvider.class);

    public static final String CHANGE_LOG_COLLECTION = "databaseChangeLog";

    private Class<? extends Update>[] updates = new Class[]{
            Update1_0_0_Final.class,
            Update1_1_0_Beta1.class,
            Update1_2_0_Beta1.class
    };

    @Override
    public void update(KeycloakSession session, DB db) {
        log.debug("Starting database update");
        try {
            boolean changeLogExists = db.collectionExists(CHANGE_LOG_COLLECTION);
            boolean realmExists = db.collectionExists("realms");

            DBCollection changeLog = db.getCollection(CHANGE_LOG_COLLECTION);

            List<String> executed = new LinkedList<String>();
            if (!changeLogExists && realmExists) {
                Update1_0_0_Final u = new Update1_0_0_Final();
                executed.add(u.getId());
                createLog(changeLog, u, 1);
            } else if (changeLogExists) {
                DBCursor cursor = changeLog.find().sort(new BasicDBObject("orderExecuted", 1));
                while (cursor.hasNext()) {
                    executed.add((String) cursor.next().get("_id"));
                }
            }

            List<Update> updatesToRun = new LinkedList<Update>();
            for (Class<? extends Update> updateClass : updates) {
                Update u = updateClass.newInstance();
                if (!executed.contains(u.getId())) {
                    updatesToRun.add(u);
                }
            }

            if (!updatesToRun.isEmpty()) {
                if (executed.isEmpty()) {
                    log.info("Initializing database schema");
                } else {
                    if (log.isDebugEnabled()) {
                        log.infov("Updating database from {0} to {1}", executed.get(executed.size() - 1), updatesToRun.get(updatesToRun.size() - 1).getId());
                    } else {
                        log.debugv("Updating database");
                    }
                }

                int order = executed.size();
                for (Update u : updatesToRun) {
                    log.debugv("Executing updates for {0}", u.getId());

                    u.setLog(log);
                    u.setDb(db);
                    u.update(session);

                    createLog(changeLog, u, ++order);

                    log.debugv("Completed updates for {0}", u.getId());
                }
                log.debug("Completed database update");
            } else {
                log.debug("Skip database update. Database is already up to date");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to update database", e);
        }
    }

    private void createLog(DBCollection changeLog, Update update, int orderExecuted) {
        changeLog.insert(new BasicDBObject("_id", update.getId()).append("dateExecuted", new Date()).append("orderExecuted", orderExecuted));
    }

    @Override
    public void close() {
    }

}
