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

import java.util.concurrent.atomic.AtomicBoolean;

import com.mongodb.DB;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.util.Time;
import org.keycloak.connections.mongo.MongoConnectionProvider;
import org.keycloak.connections.mongo.MongoConnectionProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.dblock.DBLockProviderFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoDBLockProviderFactory implements DBLockProviderFactory {

    private static final Logger logger = Logger.getLogger(MongoDBLockProviderFactory.class);

    private long lockRecheckTimeMillis;
    private long lockWaitTimeoutMillis;

    // True if this node has a lock acquired
    private AtomicBoolean hasLock = new AtomicBoolean(false);

    protected long getLockRecheckTimeMillis() {
        return lockRecheckTimeMillis;
    }

    protected long getLockWaitTimeoutMillis() {
        return lockWaitTimeoutMillis;
    }

    @Override
    public void init(Config.Scope config) {
        int lockRecheckTime = config.getInt("lockRecheckTime", 2);
        int lockWaitTimeout = config.getInt("lockWaitTimeout", 900);
        this.lockRecheckTimeMillis = Time.toMillis(lockRecheckTime);
        this.lockWaitTimeoutMillis = Time.toMillis(lockWaitTimeout);
        logger.debugf("Mongo lock provider configured with lockWaitTime: %d seconds, lockRecheckTime: %d seconds", lockWaitTimeout, lockRecheckTime);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public MongoDBLockProvider create(KeycloakSession session) {
        MongoConnectionProviderFactory mongoConnectionFactory = (MongoConnectionProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(MongoConnectionProvider.class);
        DB db = mongoConnectionFactory.getDBBeforeUpdate();
        return new MongoDBLockProvider(this, db);
    }

    @Override
    public void setTimeouts(long lockRecheckTimeMillis, long lockWaitTimeoutMillis) {
        this.lockRecheckTimeMillis = lockRecheckTimeMillis;
        this.lockWaitTimeoutMillis = lockWaitTimeoutMillis;
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "mongo";
    }

    public boolean hasLock() {
        return hasLock.get();
    }

    public void setHasLock(boolean hasLock) {
        this.hasLock.set(hasLock);
    }

}
