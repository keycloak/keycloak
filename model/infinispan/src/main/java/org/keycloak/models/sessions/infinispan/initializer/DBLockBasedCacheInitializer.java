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

package org.keycloak.models.sessions.infinispan.initializer;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.dblock.DBLockManager;
import org.keycloak.models.dblock.DBLockProvider;

/**
 * Encapsulates preloading of sessions within the DB Lock. This DB-aware lock ensures that "startLoading" is done on single DC and the other DCs need to wait.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DBLockBasedCacheInitializer extends CacheInitializer {

    private static final Logger log = Logger.getLogger(DBLockBasedCacheInitializer.class);

    private final KeycloakSession session;
    private final CacheInitializer delegate;

    public DBLockBasedCacheInitializer(KeycloakSession session, CacheInitializer delegate) {
        this.session = session;
        this.delegate = delegate;
    }


    @Override
    public void initCache() {
        delegate.initCache();
    }


    @Override
    protected boolean isFinished() {
        return delegate.isFinished();
    }


    @Override
    protected boolean isCoordinator() {
        return delegate.isCoordinator();
    }


    /**
     * Just coordinator will run this. And there is DB-lock, so the delegate.startLoading() will be permitted just by the single DC
     */
    @Override
    protected void startLoading() {
        DBLockManager dbLockManager = new DBLockManager(session);
        dbLockManager.checkForcedUnlock();
        DBLockProvider dbLock = dbLockManager.getDBLock();
        dbLock.waitForLock(DBLockProvider.Namespace.OFFLINE_SESSIONS);
        try {

            if (isFinished()) {
                log.infof("Task already finished when DBLock retrieved");
            } else {
                delegate.startLoading();
            }
        } finally {
            dbLock.releaseLock();
        }
    }
}
