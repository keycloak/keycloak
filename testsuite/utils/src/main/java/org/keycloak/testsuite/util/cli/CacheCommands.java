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

import java.util.Map;
import java.util.Set;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import org.infinispan.Cache;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CacheCommands {

    public static class ListCachesCommand extends AbstractCommand {

        @Override
        public String getName() {
            return "listCaches";
        }

        @Override
        protected void doRunCommand(KeycloakSession session) {
            InfinispanConnectionProvider ispnProvider = session.getProvider(InfinispanConnectionProvider.class);
            Set<String> cacheNames = ispnProvider.getCache("realms").getCacheManager().getCacheNames();
            log.infof("Available caches: %s", cacheNames);
        }

    }


    public static class GetCacheCommand extends AbstractCommand {

        @Override
        public String getName() {
            return "getCache";
        }

        @Override
        protected void doRunCommand(KeycloakSession session) {
            String cacheName = getArg(0);
            InfinispanConnectionProvider ispnProvider = session.getProvider(InfinispanConnectionProvider.class);
            Cache<Object, Object> cache = ispnProvider.getCache(cacheName);
            if (cache == null) {
                log.errorf("Cache '%s' doesn't exist", cacheName);
                throw new HandledException();
            }

            printCache(cache);
        }

        private void printCache(Cache<Object, Object> cache) {
            int size = cache.size();
            log.infof("Cache %s, size: %d", cache.getName(), size);

            if (size > 50) {
                log.info("Skip printing cache records due to big size");
            } else {
                for (Map.Entry<Object, Object> entry : cache.entrySet()) {
                    log.infof("%s=%s", entry.getKey(), entry.getValue());
                }
            }
        }

        @Override
        public String printUsage() {
            return super.printUsage() + " <cache-name> . cache-name is name of the infinispan cache provided by InfinispanConnectionProvider";
        }

    }


    public static class CacheRealmObjectsCommand extends AbstractCommand {

        @Override
        public String getName() {
            return "cacheRealmObjects";
        }

        @Override
        protected void doRunCommand(KeycloakSession session) {
            String realmName = getArg(0);
            RealmModel realm = session.realms().getRealmByName(realmName);
            if (realm == null) {
                log.errorf("Realm not found: %s", realmName);
                throw new HandledException();
            }

            TestCacheUtils.cacheRealmWithEverything(session, realmName);
        }

        @Override
        public String printUsage() {
            return super.printUsage() + " <realm-name>";
        }
    }
}
