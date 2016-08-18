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

package org.keycloak.connections.mongo.lock;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.WriteResult;
import org.jboss.logging.Logger;
import org.keycloak.common.util.HostUtils;
import org.keycloak.common.util.Time;
import org.keycloak.models.dblock.DBLockProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoDBLockProvider implements DBLockProvider {

    private static final String DB_LOCK_COLLECTION = "dblock";
    private static final Logger logger = Logger.getLogger(MongoDBLockProvider .class);

    private final MongoDBLockProviderFactory factory;
    private final DB db;

    public MongoDBLockProvider(MongoDBLockProviderFactory factory, DB db) {
        this.factory = factory;
        this.db = db;
    }


    @Override
    public void waitForLock() {
        boolean locked = false;
        long startTime = Time.toMillis(Time.currentTime());
        long timeToGiveUp = startTime + (factory.getLockWaitTimeoutMillis());

        while (!locked && Time.toMillis(Time.currentTime()) < timeToGiveUp) {
            locked = acquireLock();
            if (!locked) {
                int remainingTime = ((int)(timeToGiveUp / 1000)) - Time.currentTime();
                logger.debugf("Waiting for changelog lock... Remaining time: %d seconds", remainingTime);
                try {
                    Thread.sleep(factory.getLockRecheckTimeMillis());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (!locked) {
            DBObject query = new BasicDBObject("_id", 1);
            DBCursor cursor = db.getCollection(DB_LOCK_COLLECTION).find(query);
            String lockedBy;
            if (cursor.hasNext()) {
                DBObject dbObj = cursor.next();
                lockedBy = dbObj.get("lockedBy") + " since " + Time.toDate(((int)((long) dbObj.get("lockedSince") / 1000)));
            } else {
                lockedBy = "UNKNOWN";
            }
            throw new IllegalStateException("Could not acquire change log lock.  Currently locked by " + lockedBy);
        }
    }


    private boolean acquireLock() {
        DBObject query = new BasicDBObject("locked", false);

        BasicDBObject update = new BasicDBObject("locked", true);
        update.append("_id", 1);
        update.append("lockedSince", Time.toMillis(Time.currentTime()));
        update.append("lockedBy", HostUtils.getHostName()); // Maybe replace with something better, but doesn't matter for now

        try {
            WriteResult wr = db.getCollection(DB_LOCK_COLLECTION).update(query, update, true, false);
            if (wr.getN() == 1) {
                logger.debugf("Successfully acquired DB lock");
                factory.setHasLock(true);
                return true;
            } else {
                return false;
            }
        } catch (DuplicateKeyException dke) {
            logger.debugf("Failed acquire lock. Reason: %s", dke.getMessage());
        }

        return false;
    }


    @Override
    public void releaseLock() {
        DBObject query = new BasicDBObject("locked", true);

        BasicDBObject update = new BasicDBObject("locked", false);
        update.append("_id", 1);
        update.append("lockedBy", null);
        update.append("lockedSince", null);

        try {
            WriteResult wr = db.getCollection(DB_LOCK_COLLECTION).update(query, update, true, false);
            if (wr.getN() > 0) {
                factory.setHasLock(false);
                logger.debugf("Successfully released DB lock");
            } else {
                logger.warnf("Attempt to release DB lock, but nothing was released");
            }
        } catch (DuplicateKeyException dke) {
            logger.debugf("Failed release lock. Reason: %s", dke.getMessage());
        }
    }

    @Override
    public boolean hasLock() {
        return factory.hasLock();
    }

    @Override
    public boolean supportsForcedUnlock() {
        return true;
    }

    @Override
    public void destroyLockInfo() {
        db.getCollection(DB_LOCK_COLLECTION).remove(new BasicDBObject());
        logger.debugf("Destroyed lock collection");
    }

    @Override
    public void close() {

    }
}
