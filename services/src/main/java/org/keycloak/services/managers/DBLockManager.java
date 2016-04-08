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

package org.keycloak.services.managers;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RealmProviderFactory;
import org.keycloak.models.dblock.DBLockProvider;
import org.keycloak.models.dblock.DBLockProviderFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.ServicesLogger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DBLockManager {

    protected static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    private final KeycloakSession session;

    public DBLockManager(KeycloakSession session) {
        this.session = session;
    }


    public void checkForcedUnlock() {
        if (Boolean.getBoolean("keycloak.dblock.forceUnlock")) {
            DBLockProvider lock = getDBLock();
            if (lock.supportsForcedUnlock()) {
                logger.forcedReleaseDBLock();
                lock.releaseLock();
            } else {
                throw new IllegalStateException("Forced unlock requested, but provider " + lock + " doesn't support it");
            }
        }
    }


    // Try to detect ID from realmProvider
    public DBLockProvider getDBLock() {
        String realmProviderId = getRealmProviderId();
        return session.getProvider(DBLockProvider.class, realmProviderId);
    }

    public DBLockProviderFactory getDBLockFactory() {
        String realmProviderId = getRealmProviderId();
        return (DBLockProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(DBLockProvider.class, realmProviderId);
    }

    private String getRealmProviderId() {
        RealmProviderFactory realmProviderFactory = (RealmProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(RealmProvider.class);
        return realmProviderFactory.getId();
    }

}
