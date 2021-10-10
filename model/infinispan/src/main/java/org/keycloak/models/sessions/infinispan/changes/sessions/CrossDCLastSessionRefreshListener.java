/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.changes.sessions;

import java.util.Map;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.connections.infinispan.TopologyInfo;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.connections.infinispan.InfinispanUtil;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CrossDCLastSessionRefreshListener implements ClusterListener {

    public static final Logger logger = Logger.getLogger(CrossDCLastSessionRefreshListener.class);

    public static final String IGNORE_REMOTE_CACHE_UPDATE = "IGNORE_REMOTE_CACHE_UPDATE";

    private final boolean offline;

    private final KeycloakSessionFactory sessionFactory;
    private final Cache<String, SessionEntityWrapper<UserSessionEntity>> cache;
    private final TopologyInfo topologyInfo;

    public CrossDCLastSessionRefreshListener(KeycloakSession session, Cache<String, SessionEntityWrapper<UserSessionEntity>> cache, boolean offline) {
        this.sessionFactory = session.getKeycloakSessionFactory();
        this.cache = cache;
        this.offline = offline;

        this.topologyInfo = InfinispanUtil.getTopologyInfo(session);
    }

    @Override
    public void eventReceived(ClusterEvent event) {
        Map<String, SessionData> lastSessionRefreshes = ((LastSessionRefreshEvent) event).getLastSessionRefreshes();

        if (logger.isDebugEnabled()) {
            logger.debugf("Received refreshes. Offline %b, refreshes: %s", offline, lastSessionRefreshes);
        }

        lastSessionRefreshes.entrySet().stream().forEach((entry) -> {
            String sessionId = entry.getKey();
            String realmId = entry.getValue().getRealmId();
            int lastSessionRefresh = entry.getValue().getLastSessionRefresh();

            // All nodes will receive the message. So ensure that each node updates just lastSessionRefreshes owned by him.
            if (shouldUpdateLocalCache(sessionId)) {
                KeycloakModelUtils.runJobInTransaction(sessionFactory, (kcSession) -> {

                    RealmModel realm = kcSession.realms().getRealm(realmId);
                    UserSessionModel userSession = offline ? kcSession.sessions().getOfflineUserSession(realm, sessionId) : kcSession.sessions().getUserSession(realm, sessionId);
                    if (userSession == null) {
                        logger.debugf("User session '%s' not available on node '%s' offline '%b'", sessionId, topologyInfo.getMyNodeName(), offline);
                    } else {
                        // Update just if lastSessionRefresh from event is bigger than ours
                        if (lastSessionRefresh > userSession.getLastSessionRefresh()) {

                            // Ensure that remoteCache won't be updated due to this
                            kcSession.setAttribute(IGNORE_REMOTE_CACHE_UPDATE, true);

                            userSession.setLastSessionRefresh(lastSessionRefresh);
                        }
                    }
                });
            }

        });
    }


    // For distributed caches, ensure that local modification is executed just on owner
    protected boolean shouldUpdateLocalCache(String key) {
        return topologyInfo.amIOwner(cache, key);
    }
}
