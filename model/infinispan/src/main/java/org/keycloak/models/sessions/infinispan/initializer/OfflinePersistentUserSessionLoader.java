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

import org.infinispan.Cache;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.util.Retry;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OfflinePersistentUserSessionLoader implements SessionLoader, Serializable {

    private static final Logger log = Logger.getLogger(OfflinePersistentUserSessionLoader.class);

    // Cross-DC aware flag
    public static final String PERSISTENT_SESSIONS_LOADED = "PERSISTENT_SESSIONS_LOADED";

    // Just local-DC aware flag
    public static final String PERSISTENT_SESSIONS_LOADED_IN_CURRENT_DC = "PERSISTENT_SESSIONS_LOADED_IN_CURRENT_DC";


    @Override
    public void init(KeycloakSession session) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);

        // TODO: check if update of timestamps in persister can be skipped entirely
        int clusterStartupTime = session.getProvider(ClusterProvider.class).getClusterStartupTime();

        log.debugf("Clearing detached sessions from persistent storage and updating timestamps to %d", clusterStartupTime);

        persister.clearDetachedUserSessions();
        persister.updateAllTimestamps(clusterStartupTime);
    }


    @Override
    public int getSessionsCount(KeycloakSession session) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        return persister.getUserSessionsCount(true);
    }


    @Override
    public boolean loadSessions(KeycloakSession session, int first, int max) {
        if (log.isTraceEnabled()) {
            log.tracef("Loading sessions - first: %d, max: %d", first, max);
        }

        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        List<UserSessionModel> sessions = persister.loadUserSessions(first, max, true);

        for (UserSessionModel persistentSession : sessions) {

            // Save to memory/infinispan
            UserSessionModel offlineUserSession = session.sessions().importUserSession(persistentSession, true, true);
        }

        return true;
    }


    @Override
    public boolean isFinished(BaseCacheInitializer initializer) {
        Cache<String, Serializable> workCache = initializer.getWorkCache();
        Boolean sessionsLoaded = (Boolean) workCache.get(PERSISTENT_SESSIONS_LOADED);

        if (sessionsLoaded != null && sessionsLoaded) {
            log.debugf("Persistent sessions loaded already.");
            return true;
        } else {
            log.debugf("Persistent sessions not yet loaded.");
            return false;
        }
    }


    @Override
    public void afterAllSessionsLoaded(BaseCacheInitializer initializer) {
        Cache<String, Serializable> workCache = initializer.getWorkCache();

        // Will retry few times for the case when backup site not available in cross-dc environment.
        // The site might be taken offline automatically if "take-offline" properly configured
        Retry.executeWithBackoff((int iteration) -> {

            try {
                // Cross-DC aware flag
                workCache
                        .getAdvancedCache().withFlags(Flag.SKIP_REMOTE_LOOKUP)
                        .put(PERSISTENT_SESSIONS_LOADED, true);

            } catch (HotRodClientException re) {
                log.warnf(re, "Failed to write flag PERSISTENT_SESSIONS_LOADED in iteration '%d' . Retrying", iteration);

                // Rethrow the exception. Retry will take care of handle the exception and eventually retry the operation.
                throw re;
            }

        }, 10, 10);

        // Just local-DC aware flag
        workCache
                .getAdvancedCache().withFlags(Flag.SKIP_REMOTE_LOOKUP, Flag.SKIP_CACHE_LOAD, Flag.SKIP_CACHE_STORE)
                .put(PERSISTENT_SESSIONS_LOADED_IN_CURRENT_DC, true);


        log.debugf("Persistent sessions loaded successfully!");
    }

}
