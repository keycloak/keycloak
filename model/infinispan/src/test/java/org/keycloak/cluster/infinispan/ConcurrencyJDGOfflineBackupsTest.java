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

package org.keycloak.cluster.infinispan;

import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.context.Flag;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ConcurrencyJDGOfflineBackupsTest {

    protected static final Logger logger = Logger.getLogger(ConcurrencyJDGOfflineBackupsTest.class);

    public static void main(String[] args) throws Exception {

        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache1 = createManager(1).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);

        try {
            // Create initial item
            UserSessionEntity session = new UserSessionEntity();
            session.setId("123");
            session.setRealmId("foo");
            session.setBrokerSessionId("!23123123");
            session.setBrokerUserId(null);
            session.setUser("foo");
            session.setLoginUsername("foo");
            session.setIpAddress("123.44.143.178");
            session.setStarted(Time.currentTime());
            session.setLastSessionRefresh(Time.currentTime());

//        AuthenticatedClientSessionEntity clientSession = new AuthenticatedClientSessionEntity();
//        clientSession.setAuthMethod("saml");
//        clientSession.setAction("something");
//        clientSession.setTimestamp(1234);
//        clientSession.setProtocolMappers(new HashSet<>(Arrays.asList("mapper1", "mapper2")));
//        clientSession.setRoles(new HashSet<>(Arrays.asList("role1", "role2")));
//        session.getAuthenticatedClientSessions().put(CLIENT_1_UUID.toString(), clientSession.getId());

            SessionEntityWrapper<UserSessionEntity> wrappedSession = new SessionEntityWrapper<>(session);

            // Some dummy testing of remoteStore behaviour
            logger.info("Before put");


            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorsCount = new AtomicInteger(0);
            for (int i=0 ; i<100 ; i++) {
                try {
                    cache1
                            .getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL) // will still invoke remoteStore . Just doesn't propagate to cluster
                            .put("123", wrappedSession);
                    successCount.incrementAndGet();
                    Thread.sleep(1000);
                    logger.infof("Success in the iteration: %d", i);
                } catch (HotRodClientException hrce) {
                    logger.errorf("Failed to put the item in the iteration: %d ", i);
                    errorsCount.incrementAndGet();
                }
            }

            logger.infof("SuccessCount: %d, ErrorsCount: %d", successCount.get(), errorsCount.get());

//            logger.info("After put");
//
//            cache1.replace("123", wrappedSession);
//
//            logger.info("After replace");
//
//            cache1.get("123");
//
//            logger.info("After cache1.get");

//        cache2.get("123");
//
//        logger.info("After cache2.get");

        } finally {
            // Finish JVM
            cache1.getCacheManager().stop();
        }

    }

    private static EmbeddedCacheManager createManager(int threadId) {
        return new TestCacheManagerFactory().createManager(threadId, InfinispanConnectionProvider.USER_SESSION_CACHE_NAME, RemoteStoreConfigurationBuilder.class);
    }

}
