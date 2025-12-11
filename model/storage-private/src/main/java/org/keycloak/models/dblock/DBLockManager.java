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

package org.keycloak.models.dblock;

import org.keycloak.models.KeycloakSession;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DBLockManager {

    protected static final Logger logger = Logger.getLogger(DBLockManager.class);

    private final KeycloakSession session;

    public DBLockManager(KeycloakSession session) {
        this.session = session;
    }

    public void checkForcedUnlock() {
        if (Boolean.getBoolean("keycloak.dblock.forceUnlock")) {
            DBLockProvider lock = getDBLock();
            if (lock.supportsForcedUnlock()) {
                logger.warn("Forced release of DB lock at startup requested by System property. Make sure to not use this in production environment! And especially when more cluster nodes are started concurrently.");
                lock.releaseLock();
            } else {
                throw new IllegalStateException("Forced unlock requested, but provider " + lock + " doesn't support it");
            }
        }
    }

    public DBLockProvider getDBLock() {
        return session.getProvider(DBLockProvider.class);
    }

    public DBLockProviderFactory getDBLockFactory() {
        return (DBLockProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(DBLockProvider.class);
    }
}
