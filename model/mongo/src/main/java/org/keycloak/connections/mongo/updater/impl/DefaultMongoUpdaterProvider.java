/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.keycloak.connections.mongo.updater.impl.updates.Update1_2_0_CR1;
import org.keycloak.connections.mongo.updater.impl.updates.Update1_3_0;
import org.keycloak.connections.mongo.updater.impl.updates.Update1_4_0;
import org.keycloak.connections.mongo.updater.impl.updates.Update1_7_0;
import org.keycloak.connections.mongo.updater.impl.updates.Update1_8_0;
import org.keycloak.connections.mongo.updater.impl.updates.Update1_9_2;
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
            Update1_2_0_Beta1.class,
            Update1_2_0_CR1.class,
            Update1_3_0.class,
            Update1_4_0.class,
            Update1_7_0.class,
            Update1_8_0.class,
            Update1_9_2.class
    };

    @Override
    public void update(KeycloakSession session, DB db) {
        log.debug("Starting database update");
        try {
            boolean changeLogExists = db.collectionExists(CHANGE_LOG_COLLECTION);
            DBCollection changeLog = db.getCollection(CHANGE_LOG_COLLECTION);

            List<String> executed = getExecuted(db, changeLogExists, changeLog);
            List<Update> updatesToRun = getUpdatesToRun(executed);

            if (!updatesToRun.isEmpty()) {
                if (executed.isEmpty()) {
                    log.info("Initializing database schema");
                } else {
                    if (log.isDebugEnabled()) {
                        log.debugv("Updating database from {0} to {1}", executed.get(executed.size() - 1), updatesToRun.get(updatesToRun.size() - 1).getId());
                    } else {
                        log.info("Updating database");
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


    @Override
    public void validate(KeycloakSession session, DB db) {
        log.debug("Validating database");

        boolean changeLogExists = db.collectionExists(CHANGE_LOG_COLLECTION);
        DBCollection changeLog = db.getCollection(CHANGE_LOG_COLLECTION);

        List<String> executed = getExecuted(db, changeLogExists, changeLog);
        List<Update> updatesToRun = getUpdatesToRun(executed);

        if (!updatesToRun.isEmpty()) {
            String errorMessage = (executed.isEmpty())
                    ? "Failed to validate Mongo database schema. Database is empty. Please change databaseSchema to 'update'"
                    : String.format("Failed to validate Mongo database schema. Schema needs updating database from %s to %s. Please change databaseSchema to 'update'",
                    executed.get(executed.size() - 1), updatesToRun.get(updatesToRun.size() - 1).getId());

            throw new RuntimeException(errorMessage);
        } else {
            log.debug("Validation passed. Database is up to date");
        }
    }


    private List<String> getExecuted(DB db, boolean changeLogExists, DBCollection changeLog) {
        boolean realmExists = db.collectionExists("realms");

        List<String> executed = new LinkedList<>();
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
        return executed;
    }


    private List<Update> getUpdatesToRun(List<String> executed) {
        try {
            List<Update> updatesToRun = new LinkedList<>();
            for (Class<? extends Update> updateClass : updates) {
                Update u = updateClass.newInstance();
                if (!executed.contains(u.getId())) {
                    updatesToRun.add(u);
                }
            }
            return updatesToRun;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void createLog(DBCollection changeLog, Update update, int orderExecuted) {
        changeLog.insert(new BasicDBObject("_id", update.getId()).append("dateExecuted", new Date()).append("orderExecuted", orderExecuted));
    }


    @Override
    public void close() {
    }

}
