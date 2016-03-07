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

package org.keycloak.testsuite.util.cli;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractOfflineCacheCommand extends AbstractCommand {

    @Override
    protected void doRunCommand(KeycloakSession session) {
        InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
        Cache<String, SessionEntity> ispnCache = provider.getCache(InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME);
        doRunCacheCommand(session, ispnCache);
    }

    protected void printSession(String id, UserSessionEntity userSession) {
        if (userSession == null) {
            log.info("Not found session with Id: " + id);
        } else {
            log.info("Found session. ID: " + toString(userSession));
        }
    }

    protected String toString(UserSessionEntity userSession) {
        int clientSessionsSize = userSession.getClientSessions()==null ? 0 : userSession.getClientSessions().size();
        return "ID: " + userSession.getId() + ", realm: " + userSession.getRealm() + ", lastAccessTime: " + Time.toDate(userSession.getLastSessionRefresh()) +
                ", clientSessions: " + clientSessionsSize;
    }

    protected abstract void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntity> cache);


    // IMPLS

    public static class PutCommand extends AbstractOfflineCacheCommand {

        @Override
        public String getName() {
            return "put";
        }

        @Override
        protected void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntity> cache) {
            UserSessionEntity userSession = new UserSessionEntity();
            String id = getArg(0);

            userSession.setId(id);
            userSession.setRealm(getArg(1));

            userSession.setLastSessionRefresh(Time.currentTime());
            cache.put(id, userSession);
        }

        @Override
        public String printUsage() {
            return getName() + " <user-session-id> <realm-name>";
        }
    }


    public static class GetCommand extends AbstractOfflineCacheCommand {

        @Override
        public String getName() {
            return "get";
        }

        @Override
        protected void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntity> cache) {
            String id = getArg(0);
            UserSessionEntity userSession = (UserSessionEntity) cache.get(id);
            printSession(id, userSession);
        }

        @Override
        public String printUsage() {
            return getName() + " <user-session-id>";
        }
    }

    // Just to check performance of multiple get calls. And comparing what's the change between the case when item is available locally or not.
    public static class GetMultipleCommand extends AbstractOfflineCacheCommand {

        @Override
        public String getName() {
            return "getMulti";
        }

        @Override
        protected void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntity> cache) {
            String id = getArg(0);
            int count = getIntArg(1);

            long start = System.currentTimeMillis();
            for (int i=0 ; i<count ; i++) {
                UserSessionEntity userSession = (UserSessionEntity) cache.get(id);
                //printSession(id, userSession);
            }
            long took = System.currentTimeMillis() - start;
            log.infof("Took %d milliseconds", took);
        }

        @Override
        public String printUsage() {
            return getName() + " <user-session-id> <count-of-gets>";
        }
    }


    public static class RemoveCommand extends AbstractOfflineCacheCommand {

        @Override
        public String getName() {
            return "remove";
        }

        @Override
        protected void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntity> cache) {
            String id = getArg(0);
            cache.remove(id);
        }

        @Override
        public String printUsage() {
            return getName() + " <user-session-id>";
        }
    }


    public static class ClearCommand extends AbstractOfflineCacheCommand {

        @Override
        public String getName() {
            return "clear";
        }

        @Override
        protected void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntity> cache) {
            cache.clear();
        }
    }


    public static class SizeCommand extends AbstractOfflineCacheCommand {

        @Override
        public String getName() {
            return "size";
        }

        @Override
        protected void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntity> cache) {
            log.info("Size: " + cache.size());
        }
    }


    public static class ListCommand extends AbstractOfflineCacheCommand {

        @Override
        public String getName() {
            return "list";
        }

        @Override
        protected void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntity> cache) {
            for (String id : cache.keySet()) {
                SessionEntity entity = cache.get(id);
                if (!(entity instanceof UserSessionEntity)) {
                    continue;
                }
                UserSessionEntity userSession = (UserSessionEntity) cache.get(id);
                log.info("list: key=" + id + ", value=" + toString(userSession));
            }
        }
    }


    public static class GetLocalCommand extends AbstractOfflineCacheCommand {

        @Override
        public String getName() {
            return "getLocal";
        }


        @Override
        protected void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntity> cache) {
            String id = getArg(0);
            cache = ((AdvancedCache) cache).withFlags(Flag.CACHE_MODE_LOCAL);
            UserSessionEntity userSession = (UserSessionEntity) cache.get(id);
            printSession(id, userSession);
        }

        @Override
        public String printUsage() {
            return getName() + " <user-session-id>";
        }
    }

}
