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

package org.keycloak.testsuite.crossdc;


import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.Retry;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.InfinispanStatistics;
import org.keycloak.testsuite.arquillian.annotation.JmxInfinispanCacheStatistics;
import org.keycloak.testsuite.util.OAuthClient;

import javax.ws.rs.NotFoundException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LastSessionRefreshCrossDCTest extends AbstractAdminCrossDCTest {

    @Test
    public void testRevokeRefreshToken(@JmxInfinispanCacheStatistics(dc=DC.FIRST, managementPortProperty = "cache.server.management.port", cacheName=InfinispanConnectionProvider.USER_SESSION_CACHE_NAME) InfinispanStatistics sessionCacheDc1Stats,
                                       @JmxInfinispanCacheStatistics(dc=DC.SECOND, managementPortProperty = "cache.server.2.management.port", cacheName=InfinispanConnectionProvider.USER_SESSION_CACHE_NAME) InfinispanStatistics sessionCacheDc2Stats,
                                       @JmxInfinispanCacheStatistics(dc=DC.FIRST, managementPortProperty = "cache.server.management.port", cacheName=InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME) InfinispanStatistics clientSessionCacheDc1Stats,
                                       @JmxInfinispanCacheStatistics(dc=DC.SECOND, managementPortProperty = "cache.server.2.management.port", cacheName=InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME) InfinispanStatistics clientSessionCacheDc2Stats

    ) {
        // Enable revokeRefreshToken
        RealmRepresentation realmRep = testRealm().toRepresentation();
        realmRep.setRevokeRefreshToken(true);
        testRealm().update(realmRep);

        // Enable second DC
        enableDcOnLoadBalancer(DC.SECOND);

        sessionCacheDc1Stats.reset();
        sessionCacheDc2Stats.reset();
        clientSessionCacheDc1Stats.reset();
        clientSessionCacheDc2Stats.reset();

        // Get statistics
        AtomicLong sessionStoresDc1 = new AtomicLong(getStores(sessionCacheDc1Stats));
        AtomicLong sessionStoresDc2 = new AtomicLong(getStores(sessionCacheDc2Stats));
        AtomicLong clientSessionStoresDc1 = new AtomicLong(getStores(clientSessionCacheDc1Stats));
        AtomicLong clientSessionStoresDc2 = new AtomicLong(getStores(clientSessionCacheDc2Stats));
        AtomicInteger lsrDc1 = new AtomicInteger(-1);
        AtomicInteger lsrDc2 = new AtomicInteger(-1);

        // Login
        OAuthClient.AuthorizationEndpointResponse response1 = oauth.doLogin("test-user@localhost", "password");
        String code = response1.getCode();
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        Assert.assertNotNull(tokenResponse.getAccessToken());
        String sessionId = oauth.verifyToken(tokenResponse.getAccessToken()).getSessionState();
        String refreshToken1 = tokenResponse.getRefreshToken();


        // Assert statistics - sessions created on both DCs and created on remoteCaches too
        assertStatistics("After session created", sessionId, sessionCacheDc1Stats, sessionCacheDc2Stats, clientSessionCacheDc1Stats, clientSessionCacheDc2Stats,
                sessionStoresDc1, sessionStoresDc2, clientSessionStoresDc1, clientSessionStoresDc2,
                lsrDc1, lsrDc2, true, true, true, false);


        // Set time offset to some point in future. TODO This won't be needed once we have single-use cache based solution for refresh tokens
        setTimeOffset(10);

        // refresh token on DC1
        disableDcOnLoadBalancer(DC.SECOND);
        tokenResponse = oauth.doRefreshTokenRequest(refreshToken1, "password");
        String refreshToken2 = tokenResponse.getRefreshToken();

        // Assert statistics - sessions updated on both DCs and on remoteCaches too
        assertStatistics("After time offset 10", sessionId, sessionCacheDc1Stats, sessionCacheDc2Stats, clientSessionCacheDc1Stats, clientSessionCacheDc2Stats,
                sessionStoresDc1, sessionStoresDc2, clientSessionStoresDc1, clientSessionStoresDc2,
                lsrDc1, lsrDc2, true, true, true, false);

        // try refresh with old token on DC2. It should fail.
        disableDcOnLoadBalancer(DC.FIRST);
        enableDcOnLoadBalancer(DC.SECOND);
        tokenResponse = oauth.doRefreshTokenRequest(refreshToken1, "password");
        Assert.assertNull("Expecting no access token present", tokenResponse.getAccessToken());
        Assert.assertNotNull(tokenResponse.getError());

        // try refresh with new token on DC2. It should pass.
        tokenResponse = oauth.doRefreshTokenRequest(refreshToken2, "password");
        Assert.assertNotNull(tokenResponse.getAccessToken());
        Assert.assertNull(tokenResponse.getError());

        // Revert
        realmRep = testRealm().toRepresentation();
        realmRep.setRevokeRefreshToken(false);
        testRealm().update(realmRep);
    }


    @Test
    public void testLastSessionRefreshUpdate(@JmxInfinispanCacheStatistics(dc=DC.FIRST, managementPortProperty = "cache.server.management.port", cacheName=InfinispanConnectionProvider.USER_SESSION_CACHE_NAME) InfinispanStatistics sessionCacheDc1Stats,
                                             @JmxInfinispanCacheStatistics(dc=DC.SECOND, managementPortProperty = "cache.server.2.management.port", cacheName=InfinispanConnectionProvider.USER_SESSION_CACHE_NAME) InfinispanStatistics sessionCacheDc2Stats,
                                             @JmxInfinispanCacheStatistics(dc=DC.FIRST, managementPortProperty = "cache.server.management.port", cacheName=InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME) InfinispanStatistics clientSessionCacheDc1Stats,
                                             @JmxInfinispanCacheStatistics(dc=DC.SECOND, managementPortProperty = "cache.server.2.management.port", cacheName=InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME) InfinispanStatistics clientSessionCacheDc2Stats

    ) {

        // Ensure to remove all current sessions and offline sessions
        setTimeOffset(10000000);
        getTestingClientForStartedNodeInDc(0).testing("test").removeExpired("test");
        setTimeOffset(0);

        sessionCacheDc1Stats.reset();
        sessionCacheDc2Stats.reset();
        clientSessionCacheDc1Stats.reset();
        clientSessionCacheDc2Stats.reset();

        // Disable DC2 on loadbalancer
        disableDcOnLoadBalancer(DC.SECOND);

        // Get statistics
        AtomicLong sessionStoresDc1 = new AtomicLong(getStores(sessionCacheDc1Stats));
        AtomicLong sessionStoresDc2 = new AtomicLong(getStores(sessionCacheDc2Stats));
        AtomicLong clientSessionStoresDc1 = new AtomicLong(getStores(clientSessionCacheDc1Stats));
        AtomicLong clientSessionStoresDc2 = new AtomicLong(getStores(clientSessionCacheDc2Stats));
        AtomicInteger lsrDc1 = new AtomicInteger(-1);
        AtomicInteger lsrDc2 = new AtomicInteger(-1);

        // Login
        OAuthClient.AuthorizationEndpointResponse response1 = oauth.doLogin("test-user@localhost", "password");
        String code = response1.getCode();
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        Assert.assertNotNull(tokenResponse.getAccessToken());
        String sessionId = oauth.verifyToken(tokenResponse.getAccessToken()).getSessionState();
        String refreshToken1 = tokenResponse.getRefreshToken();

        // Assert statistics - sessions created on both DCs and created on remoteCaches too
        assertStatistics("After session created", sessionId, sessionCacheDc1Stats, sessionCacheDc2Stats, clientSessionCacheDc1Stats, clientSessionCacheDc2Stats,
                sessionStoresDc1, sessionStoresDc2, clientSessionStoresDc1, clientSessionStoresDc2,
                lsrDc1, lsrDc2, true, true, true, false);


        // Set time offset
        setTimeOffset(100);

        // refresh token on DC1
        tokenResponse = oauth.doRefreshTokenRequest(refreshToken1, "password");
        String refreshToken3 = tokenResponse.getRefreshToken();
        Assert.assertNotNull(refreshToken3);

        // Assert statistics - sessions updated on both DC1 and DC2. RemoteCaches not updated
        assertStatistics("After refresh at time 100", sessionId, sessionCacheDc1Stats, sessionCacheDc2Stats, clientSessionCacheDc1Stats, clientSessionCacheDc2Stats,
                sessionStoresDc1, sessionStoresDc2, clientSessionStoresDc1, clientSessionStoresDc2,
                lsrDc1, lsrDc2, true, true, false, false);


        // Set time offset
        setTimeOffset(110);

        // refresh token on DC1
        tokenResponse = oauth.doRefreshTokenRequest(refreshToken1, "password");
        String refreshToken2 = tokenResponse.getRefreshToken();
        Assert.assertNotNull(refreshToken2);

        // Assert statistics - sessions updated just on DC1.
        // Update of DC2 is postponed (It's just 10 seconds since last message). RemoteCaches not updated
        assertStatistics("After refresh at time 110", sessionId, sessionCacheDc1Stats, sessionCacheDc2Stats, clientSessionCacheDc1Stats, clientSessionCacheDc2Stats,
                sessionStoresDc1, sessionStoresDc2, clientSessionStoresDc1, clientSessionStoresDc2,
                lsrDc1, lsrDc2, true, false, false, false);


        // 31 minutes after "100". Session should be still valid and not yet expired (RefreshToken will be invalid due the expiration on the JWT itself. Hence not testing refresh here)
        setTimeOffset(1960);

        boolean sessionValid = getTestingClientForStartedNodeInDc(1).server("test").fetch((KeycloakSession session) -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserSessionModel userSession = session.sessions().getUserSession(realm, sessionId);
            return AuthenticationManager.isSessionValid(realm, userSession);
        }, Boolean.class);

        Assert.assertTrue(sessionValid);

        getTestingClientForStartedNodeInDc(1).testing("test").removeExpired("test");

        // Assert statistics - nothing was updated. No refresh happened and nothing was cleared during "removeExpired"
        assertStatistics("After checking valid at time 1960", sessionId, sessionCacheDc1Stats, sessionCacheDc2Stats, clientSessionCacheDc1Stats, clientSessionCacheDc2Stats,
                sessionStoresDc1, sessionStoresDc2, clientSessionStoresDc1, clientSessionStoresDc2,
                lsrDc1, lsrDc2, false, false, false, false);


        // 35 minutes after "100". Session not valid and will be expired by the cleaner
        setTimeOffset(2200);

        sessionValid = getTestingClientForStartedNodeInDc(1).server("test").fetch((KeycloakSession session) -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserSessionModel userSession = session.sessions().getUserSession(realm, sessionId);
            return AuthenticationManager.isSessionValid(realm, userSession);
        }, Boolean.class);

        Assert.assertFalse(sessionValid);

        getTestingClientForStartedNodeInDc(1).testing("test").removeExpired("test");

        // Session should be removed on both DCs
        try {
            getTestingClientForStartedNodeInDc(0).testing("test").getLastSessionRefresh("test", sessionId, false);
            Assert.fail("It wasn't expected to find the session " + sessionId);
        } catch (NotFoundException nfe) {
            // Expected
        }
        try {
            getTestingClientForStartedNodeInDc(1).testing("test").getLastSessionRefresh("test", sessionId, false);
            Assert.fail("It wasn't expected to find the session " + sessionId);
        } catch (NotFoundException nfe) {
            // Expected
        }
    }


    @Test
    public void testOfflineSessionsLastSessionRefreshUpdate(@JmxInfinispanCacheStatistics(dc=DC.FIRST, managementPortProperty = "cache.server.management.port", cacheName=InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME) InfinispanStatistics sessionCacheDc1Stats,
                                             @JmxInfinispanCacheStatistics(dc=DC.SECOND, managementPortProperty = "cache.server.2.management.port", cacheName=InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME) InfinispanStatistics sessionCacheDc2Stats,
                                             @JmxInfinispanCacheStatistics(dc=DC.FIRST, managementPortProperty = "cache.server.management.port", cacheName=InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME) InfinispanStatistics clientSessionCacheDc1Stats,
                                             @JmxInfinispanCacheStatistics(dc=DC.SECOND, managementPortProperty = "cache.server.2.management.port", cacheName=InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME) InfinispanStatistics clientSessionCacheDc2Stats

    ) throws Exception {

        // Ensure to remove all current sessions and offline sessions
        setTimeOffset(10000000);
        getTestingClientForStartedNodeInDc(0).testing("test").removeExpired("test");
        setTimeOffset(0);

        sessionCacheDc1Stats.reset();
        sessionCacheDc2Stats.reset();
        clientSessionCacheDc1Stats.reset();
        clientSessionCacheDc2Stats.reset();

        // Disable DC2 on loadbalancer
        disableDcOnLoadBalancer(DC.SECOND);

        // Get statistics
        AtomicLong sessionStoresDc1 = new AtomicLong(getStores(sessionCacheDc1Stats));
        AtomicLong sessionStoresDc2 = new AtomicLong(getStores(sessionCacheDc2Stats));
        AtomicLong clientSessionStoresDc1 = new AtomicLong(getStores(clientSessionCacheDc1Stats));
        AtomicLong clientSessionStoresDc2 = new AtomicLong(getStores(clientSessionCacheDc2Stats));
        AtomicInteger lsrDc1 = new AtomicInteger(-1);
        AtomicInteger lsrDc2 = new AtomicInteger(-1);

        // Login
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        OAuthClient.AuthorizationEndpointResponse response1 = oauth.doLogin("test-user@localhost", "password");
        String code = response1.getCode();
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        Assert.assertNotNull(tokenResponse.getAccessToken());
        String sessionId = oauth.verifyToken(tokenResponse.getAccessToken()).getSessionState();
        String refreshToken1 = tokenResponse.getRefreshToken();

        // Assert statistics - sessions created on both DCs and created on remoteCaches too
        assertStatistics("After session created", sessionId, sessionCacheDc1Stats, sessionCacheDc2Stats, clientSessionCacheDc1Stats, clientSessionCacheDc2Stats,
                sessionStoresDc1, sessionStoresDc2, clientSessionStoresDc1, clientSessionStoresDc2,
                lsrDc1, lsrDc2, true, true, true, true);


        // Set time offset
        setTimeOffset(100);

        // refresh token on DC1
        tokenResponse = oauth.doRefreshTokenRequest(refreshToken1, "password");
        String refreshToken3 = tokenResponse.getRefreshToken();
        Assert.assertNotNull(refreshToken3);

        // Assert statistics - sessions updated on both DC1 and DC2. RemoteCaches not updated
        assertStatistics("After refresh at time 100", sessionId, sessionCacheDc1Stats, sessionCacheDc2Stats, clientSessionCacheDc1Stats, clientSessionCacheDc2Stats,
                sessionStoresDc1, sessionStoresDc2, clientSessionStoresDc1, clientSessionStoresDc2,
                lsrDc1, lsrDc2, true, true, false, true);



        // Set time offset
        setTimeOffset(110);

        // refresh token on DC1
        tokenResponse = oauth.doRefreshTokenRequest(refreshToken1, "password");
        String refreshToken2 = tokenResponse.getRefreshToken();
        Assert.assertNotNull(refreshToken2);

        // Assert statistics - sessions updated just on DC1.
        // Update of DC2 is postponed (It's just 10 seconds since last message). RemoteCaches not updated
        assertStatistics("After refresh at time 110", sessionId, sessionCacheDc1Stats, sessionCacheDc2Stats, clientSessionCacheDc1Stats, clientSessionCacheDc2Stats,
                sessionStoresDc1, sessionStoresDc2, clientSessionStoresDc1, clientSessionStoresDc2,
                lsrDc1, lsrDc2, true, false, false, true);


        // Set time offset to 20 days
        setTimeOffset(1728000);

        // refresh token on DC1
        tokenResponse = oauth.doRefreshTokenRequest(refreshToken1, "password");
        String refreshToken4 = tokenResponse.getRefreshToken();
        Assert.assertNotNull(refreshToken4);

        // Assert statistics - sessions updated on both DC1 and DC2. RemoteCaches updated as well.
        assertStatistics("After refresh at time 1728000", sessionId, sessionCacheDc1Stats, sessionCacheDc2Stats, clientSessionCacheDc1Stats, clientSessionCacheDc2Stats,
                sessionStoresDc1, sessionStoresDc2, clientSessionStoresDc1, clientSessionStoresDc2,
                lsrDc1, lsrDc2, true, true, true, true);

        // Set time offset to 30 days
        setTimeOffset(2592000);

        // refresh token on DC1
        tokenResponse = oauth.doRefreshTokenRequest(refreshToken1, "password");
        String refreshToken5 = tokenResponse.getRefreshToken();
        Assert.assertNotNull(refreshToken5);

        // Assert statistics - sessions updated on both DC1 and DC2. RemoteCaches won't be updated now due it's just 10 days from the last remoteCache update
        assertStatistics("After refresh at time 2592000", sessionId, sessionCacheDc1Stats, sessionCacheDc2Stats, clientSessionCacheDc1Stats, clientSessionCacheDc2Stats,
                sessionStoresDc1, sessionStoresDc2, clientSessionStoresDc1, clientSessionStoresDc2,
                lsrDc1, lsrDc2, true, true, false, true);

        // Set time offset to 40 days
        setTimeOffset(3456000);

        // refresh token on DC1
        tokenResponse = oauth.doRefreshTokenRequest(refreshToken1, "password");
        String refreshToken6 = tokenResponse.getRefreshToken();
        Assert.assertNotNull(refreshToken6);

        // Assert statistics - sessions updated on both DC1 and DC2. RemoteCaches will be updated too due it's 20 days from the last remoteCache update
        assertStatistics("After refresh at time 3456000", sessionId, sessionCacheDc1Stats, sessionCacheDc2Stats, clientSessionCacheDc1Stats, clientSessionCacheDc2Stats,
                sessionStoresDc1, sessionStoresDc2, clientSessionStoresDc1, clientSessionStoresDc2,
                lsrDc1, lsrDc2, true, true, true, true);

    }


    private void assertStatistics(String messagePrefix, String sessionId,
                                  InfinispanStatistics sessionCacheDc1Stats, InfinispanStatistics sessionCacheDc2Stats, InfinispanStatistics clientSessionCacheDc1Stats, InfinispanStatistics clientSessionCacheDc2Stats,
                                  AtomicLong sessionStoresDc1, AtomicLong sessionStoresDc2, AtomicLong clientSessionStoresDc1, AtomicLong clientSessionStoresDc2,
                                  AtomicInteger lsrDc1, AtomicInteger lsrDc2,
                                  boolean expectedUpdatedLsrDc1, boolean expectedUpdatedLsrDc2, boolean expectedUpdatedRemoteCache, boolean offline) {
        Retry.execute(() -> {
            long newSessionStoresDc1 = getStores(sessionCacheDc1Stats);
            long newSessionStoresDc2 = getStores(sessionCacheDc2Stats);
            long newClientSessionStoresDc1 = getStores(clientSessionCacheDc1Stats);
            long newClientSessionStoresDc2 = getStores(clientSessionCacheDc2Stats);

            int newLsrDc1 = getTestingClientForStartedNodeInDc(0).testing("test").getLastSessionRefresh("test", sessionId, offline);
            int newLsrDc2 = getTestingClientForStartedNodeInDc(1).testing("test").getLastSessionRefresh("test", sessionId, offline);

            log.infof(messagePrefix + ": sessionStoresDc1: %d, sessionStoresDc2: %d, clientSessionStoresDc1: %d, clientSessionStoresDc2: %d, lsrDc1: %d, lsrDc2: %d",
                    newSessionStoresDc1, newSessionStoresDc2, newClientSessionStoresDc1, newClientSessionStoresDc2, newLsrDc1, newLsrDc2);

            // Check lastSessionRefresh updated on DC1
            if (expectedUpdatedLsrDc1) {
                Assert.assertThat(newLsrDc1, Matchers.greaterThan(lsrDc1.get()));
            } else {
                Assert.assertEquals(newLsrDc1, lsrDc1.get());
            }

            // Check lastSessionRefresh updated on DC2
            if (expectedUpdatedLsrDc2) {
                Assert.assertThat(newLsrDc2, Matchers.greaterThan(lsrDc2.get()));
            } else {
                Assert.assertEquals(newLsrDc2, lsrDc2.get());
            }

            // Check store statistics updated on JDG side
            if (expectedUpdatedRemoteCache) {
                Assert.assertThat(newSessionStoresDc1, Matchers.greaterThan(sessionStoresDc1.get()));
                Assert.assertThat(newSessionStoresDc2, Matchers.greaterThan(sessionStoresDc2.get()));
                Assert.assertThat(newClientSessionStoresDc1, Matchers.greaterThan(clientSessionStoresDc1.get()));
                Assert.assertThat(newClientSessionStoresDc2, Matchers.greaterThan(clientSessionStoresDc2.get()));
            } else {
                Assert.assertEquals(newSessionStoresDc1, sessionStoresDc1.get());
                Assert.assertEquals(newSessionStoresDc2, sessionStoresDc2.get());
                Assert.assertEquals(newClientSessionStoresDc1, clientSessionStoresDc1.get());
                Assert.assertEquals(newClientSessionStoresDc2, clientSessionStoresDc2.get());
            }

            // Update counter references
            sessionStoresDc1.set(newSessionStoresDc1);
            sessionStoresDc2.set(newSessionStoresDc2);
            clientSessionStoresDc1.set(newClientSessionStoresDc1);
            clientSessionStoresDc2.set(newClientSessionStoresDc2);
            lsrDc1.set(newLsrDc1);
            lsrDc2.set(newLsrDc2);
        }, 50, 50);

    }

    private long getStores(InfinispanStatistics cacheStats) {
        return (long) cacheStats.getSingleStatistics(InfinispanStatistics.Constants.STAT_CACHE_STORES);
    }

}
