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
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractSessionCacheCommand extends AbstractCommand {

    private static final Set<String> SUPPORTED_CACHE_NAMES = new TreeSet<>(Arrays.asList(
      InfinispanConnectionProvider.USER_SESSION_CACHE_NAME,
      InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME,
      InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME,
      InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME
    ));

    @Override
    protected void doRunCommand(KeycloakSession session) {
        InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
        String cacheName = getArg(0);
        if (! SUPPORTED_CACHE_NAMES.contains(cacheName)) {
            log.errorf("Invalid cache name: '%s', Only cache names '%s' are supported", cacheName, SUPPORTED_CACHE_NAMES);
            throw new HandledException();
        }

        Cache<String, SessionEntityWrapper> ispnCache = provider.getCache(cacheName);
        doRunCacheCommand(session, ispnCache);

        ispnCache.entrySet().stream().skip(0).limit(10).collect(java.util.stream.Collectors.toMap(new java.util.function.Function() {

            public Object apply(Object entry) {
                return ((java.util.Map.Entry) entry).getKey();
            }
        }, new java.util.function.Function() {

            public Object apply(Object entry) {
                return ((java.util.Map.Entry) entry).getValue();
            }
        }));
    }

    protected void printSession(String id, UserSessionEntity userSession) {
        if (userSession == null) {
            log.info("Not found session with Id: " + id);
        } else {
            log.info("Found session. ID: " + toString(userSession));
        }
    }

    protected String toString(UserSessionEntity userSession) {
        int clientSessionsSize = userSession.getAuthenticatedClientSessions()==null ? 0 : userSession.getAuthenticatedClientSessions().size();
        return "ID: " + userSession.getId() + ", realm: " + userSession.getRealmId()+ ", lastAccessTime: " + Time.toDate(userSession.getLastSessionRefresh()) +
                ", authenticatedClientSessions: " + clientSessionsSize;
    }

    @Override
    public String printUsage() {
        return getName() + " <cache-name>";
    }

    protected abstract void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntityWrapper> cache);


    // IMPLS

    public static class PutCommand extends AbstractSessionCacheCommand {

        @Override
        public String getName() {
            return "put";
        }

        @Override
        protected void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntityWrapper> cache) {
            UserSessionEntity userSession = new UserSessionEntity();
            String id = getArg(1);

            userSession.setId(id);
            userSession.setRealmId(getArg(2));

            userSession.setLastSessionRefresh(Time.currentTime());
            cache.put(id, new SessionEntityWrapper(userSession));
        }

        @Override
        public String printUsage() {
            return getName() + " <cache-name> <user-session-id> <realm-name>";
        }
    }


    public static class GetCommand extends AbstractSessionCacheCommand {

        @Override
        public String getName() {
            return "get";
        }

        @Override
        protected void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntityWrapper> cache) {
            String id = getArg(1);
            UserSessionEntity userSession = (UserSessionEntity) cache.get(id).getEntity();
            printSession(id, userSession);
        }

        @Override
        public String printUsage() {
            return getName() + " <cache-name> <user-session-id>";
        }
    }

    // Just to check performance of multiple get calls. And comparing what's the change between the case when item is available locally or not.
    public static class GetMultipleCommand extends AbstractSessionCacheCommand {

        @Override
        public String getName() {
            return "getMulti";
        }

        @Override
        protected void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntityWrapper> cache) {
            String id = getArg(1);
            int count = getIntArg(2);

            long start = System.currentTimeMillis();
            for (int i=0 ; i<count ; i++) {
                UserSessionEntity userSession = (UserSessionEntity) cache.get(id).getEntity();
                //printSession(id, userSession);
            }
            long took = System.currentTimeMillis() - start;
            log.infof("Took %d milliseconds", took);
        }

        @Override
        public String printUsage() {
            return getName() + " <cache-name> <user-session-id> <count-of-gets>";
        }
    }


    public static class RemoveCommand extends AbstractSessionCacheCommand {

        @Override
        public String getName() {
            return "remove";
        }

        @Override
        protected void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntityWrapper> cache) {
            String id = getArg(1);
            cache.remove(id);
        }

        @Override
        public String printUsage() {
            return getName() + " <cache-name> <user-session-id>";
        }
    }


    public static class ClearCommand extends AbstractSessionCacheCommand {

        @Override
        public String getName() {
            return "clear";
        }

        @Override
        protected void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntityWrapper> cache) {
            cache.clear();
        }
    }


    public static class SizeCommand extends AbstractSessionCacheCommand {

        @Override
        public String getName() {
            return "size";
        }

        @Override
        protected void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntityWrapper> cache) {
            log.info("Size: " + cache.size());
        }
    }


    public static class ListCommand extends AbstractSessionCacheCommand {

        @Override
        public String getName() {
            return "list";
        }

        @Override
        protected void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntityWrapper> cache) {
            for (String id : cache.keySet()) {
                SessionEntity entity = cache.get(id).getEntity();
                if (!(entity instanceof UserSessionEntity)) {
                    continue;
                }
                UserSessionEntity userSession = (UserSessionEntity) cache.get(id).getEntity();
                log.info("list: key=" + id + ", value=" + toString(userSession));
            }
        }
    }


    public static class GetLocalCommand extends AbstractSessionCacheCommand {

        @Override
        public String getName() {
            return "getLocal";
        }


        @Override
        protected void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntityWrapper> cache) {
            String id = getArg(1);
            cache = ((AdvancedCache) cache).withFlags(Flag.CACHE_MODE_LOCAL);
            UserSessionEntity userSession = (UserSessionEntity) cache.get(id).getEntity();
            printSession(id, userSession);
        }

        @Override
        public String printUsage() {
            return getName() + " <cache-name> <user-session-id>";
        }
    }


    public static class SizeLocalCommand extends AbstractSessionCacheCommand {

        @Override
        public String getName() {
            return "sizeLocal";
        }

        @Override
        protected void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntityWrapper> cache) {
            log.info("Size local: " + cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL).size());
        }
    }


    public static class CreateManySessionsCommand extends AbstractSessionCacheCommand {

        @Override
        public String getName() {
            return "createManySessions";
        }

        @Override
        protected void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntityWrapper> cache) {
            String realmName = getArg(1);
            int count = getIntArg(2);
            int batchCount = getIntArg(3);

            BatchTaskRunner.runInBatches(0, count, batchCount, session.getKeycloakSessionFactory(), (KeycloakSession batchSession, int firstInIteration, int countInIteration) -> {
                for (int i=0 ; i<countInIteration ; i++) {
                    UserSessionEntity userSession = new UserSessionEntity();
                    String id = KeycloakModelUtils.generateId();

                    userSession.setId(id);
                    userSession.setRealmId(realmName);

                    userSession.setLastSessionRefresh(Time.currentTime());
                    cache.put(id, new SessionEntityWrapper(userSession));
                }

                log.infof("Created '%d' sessions started from offset '%d'", countInIteration, firstInIteration);
            });

            log.infof("Created all '%d' sessions", count);
        }

        @Override
        public String printUsage() {
            return getName() + " <cache-name> <realm-name> <count> <count-in-batch>";
        }

    }


    // This will propagate creating sessions to remoteCache too
    public static class CreateManySessionsProviderCommand extends AbstractSessionCacheCommand {

        @Override
        public String getName() {
            return "createManySessionsProvider";
        }

        @Override
        protected void doRunCacheCommand(KeycloakSession session, Cache<String, SessionEntityWrapper> cache) {
            String realmName = getArg(1);
            String clientId = getArg(2);
            String username = getArg(3);
            int count = getIntArg(4);
            int batchCount = getIntArg(5);

            BatchTaskRunner.runInBatches(0, count, batchCount, session.getKeycloakSessionFactory(), (KeycloakSession batchSession, int firstInIteration, int countInIteration) -> {
                RealmModel realm = batchSession.realms().getRealmByName(realmName);
                ClientModel client = realm.getClientByClientId(clientId);
                UserModel user = batchSession.users().getUserByUsername(realm, username);

                for (int i=0 ; i<countInIteration ; i++) {
                    UserSessionModel userSession = session.sessions().createUserSession(realm, user, username, "127.0.0.1", "form", false, null, null);

                    session.sessions().createClientSession(userSession.getRealm(), client, userSession);
                }

                log.infof("Created '%d' sessions started from offset '%d'", countInIteration, firstInIteration);
            });

            log.infof("Created all '%d' sessions", count);
        }

        @Override
        public String printUsage() {
            return getName() + " <cache-name> <realm-name> <client-id> <user-name> <count> <count-in-batch>";
        }

    }

}
