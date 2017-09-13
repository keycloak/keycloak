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


import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.NotFoundException;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.Retry;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.InfinispanStatistics;
import org.keycloak.testsuite.arquillian.annotation.JmxInfinispanCacheStatistics;
import org.keycloak.testsuite.arquillian.annotation.JmxInfinispanChannelStatistics;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * Tests the bulk removal of user sessions and expiration scenarios (eg. removing realm, removing user etc)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SessionExpirationCrossDCTest extends AbstractAdminCrossDCTest {

    private static final String REALM_NAME = "expiration-test";

    private static final int SESSIONS_COUNT = 20;

    private int sessions01;
    private int sessions02;
    private int remoteSessions01;
    private int remoteSessions02;

    private int authSessions01;
    private int authSessions02;


    @Before
    public void beforeTest() {
        try {
            adminClient.realm(REALM_NAME).remove();
        } catch (NotFoundException ignore) {
        }

        UserRepresentation user = UserBuilder.create()
                .id("login-test")
                .username("login-test")
                .email("login@test.com")
                .enabled(true)
                .password("password")
                .addRoles(Constants.OFFLINE_ACCESS_ROLE)
                .build();

        ClientRepresentation client = ClientBuilder.create()
                .clientId("test-app")
                .directAccessGrants()
                .redirectUris("http://localhost:8180/auth/realms/master/app/*")
                .addWebOrigin("http://localhost:8180")
                .secret("password")
                .build();

        RealmRepresentation realmRep = RealmBuilder.create()
                .name(REALM_NAME)
                .user(user)
                .client(client)
                .build();

        adminClient.realms().create(realmRep);
    }


    @Test
    public void testRealmRemoveSessions(
            @JmxInfinispanCacheStatistics(dc=DC.FIRST, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.SESSION_CACHE_NAME) InfinispanStatistics cacheDc1Statistics,
            @JmxInfinispanCacheStatistics(dc=DC.SECOND, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.SESSION_CACHE_NAME) InfinispanStatistics cacheDc2Statistics,
            @JmxInfinispanChannelStatistics() InfinispanStatistics channelStatisticsCrossDc) throws Exception {
        createInitialSessions(InfinispanConnectionProvider.SESSION_CACHE_NAME, false, cacheDc1Statistics, cacheDc2Statistics, true);

//        log.infof("Sleeping!");
//        Thread.sleep(10000000);

        channelStatisticsCrossDc.reset();

        // Remove test realm
        getAdminClient().realm(REALM_NAME).remove();

        // Assert sessions removed on node1 and node2 and on remote caches. Assert that count of messages sent between DCs is not too big.
        assertStatisticsExpected("After realm remove", InfinispanConnectionProvider.SESSION_CACHE_NAME, cacheDc1Statistics, cacheDc2Statistics, channelStatisticsCrossDc,
                sessions01, sessions02, remoteSessions01, remoteSessions02, 40l);
    }


    // Return last used accessTokenResponse
    private List<OAuthClient.AccessTokenResponse> createInitialSessions(String cacheName, boolean offline, InfinispanStatistics cacheDc1Statistics, InfinispanStatistics cacheDc2Statistics, boolean includeRemoteStats) throws Exception {

        // Enable second DC
        enableDcOnLoadBalancer(DC.SECOND);

        // Check sessions count before test
        sessions01 = getTestingClientForStartedNodeInDc(0).testing().cache(cacheName).size();
        sessions02 = getTestingClientForStartedNodeInDc(1).testing().cache(cacheName).size();
        remoteSessions01 = (Integer) cacheDc1Statistics.getSingleStatistics(InfinispanStatistics.Constants.STAT_CACHE_NUMBER_OF_ENTRIES);
        remoteSessions02 = (Integer) cacheDc2Statistics.getSingleStatistics(InfinispanStatistics.Constants.STAT_CACHE_NUMBER_OF_ENTRIES);
        log.infof("Before creating sessions: sessions01: %d, sessions02: %d, remoteSessions01: %d, remoteSessions02: %d", sessions01, sessions02, remoteSessions01, remoteSessions02);

        // Create 20 user sessions
        oauth.realm(REALM_NAME);

        if (offline) {
            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        }

        List<OAuthClient.AccessTokenResponse> responses = new ArrayList<>();
        for (int i=0 ; i<SESSIONS_COUNT ; i++) {
            responses.add(oauth.doGrantAccessTokenRequest("password", "login-test", "password"));
        }

        // Assert 20 sessions exists on node1 and node2 and on remote caches
        Retry.execute(() -> {
            int sessions11 = getTestingClientForStartedNodeInDc(0).testing().cache(cacheName).size();
            int sessions12 = getTestingClientForStartedNodeInDc(1).testing().cache(cacheName).size();
            int remoteSessions11 = (Integer) cacheDc1Statistics.getSingleStatistics(InfinispanStatistics.Constants.STAT_CACHE_NUMBER_OF_ENTRIES);
            int remoteSessions12 = (Integer) cacheDc2Statistics.getSingleStatistics(InfinispanStatistics.Constants.STAT_CACHE_NUMBER_OF_ENTRIES);
            log.infof("After creating sessions: sessions11: %d, sessions12: %d, remoteSessions11: %d, remoteSessions12: %d", sessions11, sessions12, remoteSessions11, remoteSessions12);

            Assert.assertEquals(sessions11, sessions01 + SESSIONS_COUNT);
            Assert.assertEquals(sessions12, sessions02 + SESSIONS_COUNT);

            if (includeRemoteStats) {
                Assert.assertEquals(remoteSessions11, remoteSessions01 + SESSIONS_COUNT);
                Assert.assertEquals(remoteSessions12, remoteSessions02 + SESSIONS_COUNT);
            }
        }, 50, 50);

        return responses;
    }


    private void assertStatisticsExpected(String messagePrefix, String cacheName, InfinispanStatistics cacheDc1Statistics, InfinispanStatistics cacheDc2Statistics, InfinispanStatistics channelStatisticsCrossDc,
                                  int sessions1Expected, int sessions2Expected, int remoteSessions1Expected, int remoteSessions2Expected, long sentMessagesHigherBound) {
        Retry.execute(() -> {
            int sessions1 = getTestingClientForStartedNodeInDc(0).testing().cache(cacheName).size();
            int sessions2 = getTestingClientForStartedNodeInDc(1).testing().cache(cacheName).size();
            int remoteSessions1 = (Integer) cacheDc1Statistics.getSingleStatistics(InfinispanStatistics.Constants.STAT_CACHE_NUMBER_OF_ENTRIES);
            int remoteSessions2 = (Integer) cacheDc2Statistics.getSingleStatistics(InfinispanStatistics.Constants.STAT_CACHE_NUMBER_OF_ENTRIES);
            long messagesCount = (Long) channelStatisticsCrossDc.getSingleStatistics(InfinispanStatistics.Constants.STAT_CHANNEL_SENT_MESSAGES);
            log.infof(messagePrefix + ": sessions1: %d, sessions2: %d, remoteSessions1: %d, remoteSessions2: %d, sentMessages: %d", sessions1, sessions2, remoteSessions1, remoteSessions2, messagesCount);

            Assert.assertEquals(sessions1, sessions1Expected);
            Assert.assertEquals(sessions2, sessions2Expected);
            Assert.assertEquals(remoteSessions1, remoteSessions1Expected);
            Assert.assertEquals(remoteSessions2, remoteSessions2Expected);

            // Workaround...
            if (sentMessagesHigherBound > 5) {
                Assert.assertThat(messagesCount, Matchers.greaterThan(0l));
            }

            Assert.assertThat(messagesCount, Matchers.lessThan(sentMessagesHigherBound));
        }, 50, 50);
    }


    @Test
    public void testRealmRemoveOfflineSessions(
            @JmxInfinispanCacheStatistics(dc=DC.FIRST, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME) InfinispanStatistics cacheDc1Statistics,
            @JmxInfinispanCacheStatistics(dc=DC.SECOND, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME) InfinispanStatistics cacheDc2Statistics,
            @JmxInfinispanChannelStatistics() InfinispanStatistics channelStatisticsCrossDc) throws Exception {

        createInitialSessions(InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME, true, cacheDc1Statistics, cacheDc2Statistics, true);

        channelStatisticsCrossDc.reset();

        // Remove test realm
        getAdminClient().realm(REALM_NAME).remove();

        // Assert sessions removed on node1 and node2 and on remote caches. Assert that count of messages sent between DCs is not too big.
        assertStatisticsExpected("After realm remove", InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME, cacheDc1Statistics, cacheDc2Statistics, channelStatisticsCrossDc,
                sessions01, sessions02, remoteSessions01, remoteSessions02, 70l); // Might be bigger messages as online sessions removed too.
    }


    @Test
    public void testLogoutAllInRealm(
            @JmxInfinispanCacheStatistics(dc=DC.FIRST, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.SESSION_CACHE_NAME) InfinispanStatistics cacheDc1Statistics,
            @JmxInfinispanCacheStatistics(dc=DC.SECOND, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.SESSION_CACHE_NAME) InfinispanStatistics cacheDc2Statistics,
            @JmxInfinispanChannelStatistics() InfinispanStatistics channelStatisticsCrossDc) throws Exception {

        createInitialSessions(InfinispanConnectionProvider.SESSION_CACHE_NAME, false, cacheDc1Statistics, cacheDc2Statistics, true);

        channelStatisticsCrossDc.reset();

        // Logout all in realm
        getAdminClient().realm(REALM_NAME).logoutAll();

        // Assert sessions removed on node1 and node2 and on remote caches. Assert that count of messages sent between DCs is not too big.
        assertStatisticsExpected("After realm logout", InfinispanConnectionProvider.SESSION_CACHE_NAME, cacheDc1Statistics, cacheDc2Statistics, channelStatisticsCrossDc,
                sessions01, sessions02, remoteSessions01, remoteSessions02, 40l);
    }


    @Test
    public void testPeriodicExpiration(
            @JmxInfinispanCacheStatistics(dc=DC.FIRST, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.SESSION_CACHE_NAME) InfinispanStatistics cacheDc1Statistics,
            @JmxInfinispanCacheStatistics(dc=DC.SECOND, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.SESSION_CACHE_NAME) InfinispanStatistics cacheDc2Statistics,
            @JmxInfinispanChannelStatistics() InfinispanStatistics channelStatisticsCrossDc) throws Exception {

        OAuthClient.AccessTokenResponse lastAccessTokenResponse = createInitialSessions(InfinispanConnectionProvider.SESSION_CACHE_NAME, false, cacheDc1Statistics, cacheDc2Statistics, true).get(SESSIONS_COUNT - 1);

        // Assert I am able to refresh
        OAuthClient.AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(lastAccessTokenResponse.getRefreshToken(), "password");
        Assert.assertNotNull(refreshResponse.getRefreshToken());
        Assert.assertNull(refreshResponse.getError());

        channelStatisticsCrossDc.reset();

        // Remove expired in DC0
        getTestingClientForStartedNodeInDc(0).testing().removeExpired(REALM_NAME);

        // Nothing yet expired. Limit 5 for sent_messages is just if "lastSessionRefresh" periodic thread happened
        assertStatisticsExpected("After remove expired - 1", InfinispanConnectionProvider.SESSION_CACHE_NAME, cacheDc1Statistics, cacheDc2Statistics, channelStatisticsCrossDc,
                sessions01 + SESSIONS_COUNT, sessions02 + SESSIONS_COUNT, remoteSessions01 + SESSIONS_COUNT, remoteSessions02 + SESSIONS_COUNT, 5l);


        // Set time offset
        setTimeOffset(10000000);

        // Assert I am not able to refresh anymore
        refreshResponse = oauth.doRefreshTokenRequest(lastAccessTokenResponse.getRefreshToken(), "password");
        Assert.assertNull(refreshResponse.getRefreshToken());
        Assert.assertNotNull(refreshResponse.getError());


        channelStatisticsCrossDc.reset();

        // Remove expired in DC0
        getTestingClientForStartedNodeInDc(0).testing().removeExpired(REALM_NAME);

        // Assert sessions removed on node1 and node2 and on remote caches. Assert that count of messages sent between DCs is not too big.
        assertStatisticsExpected("After remove expired - 2", InfinispanConnectionProvider.SESSION_CACHE_NAME, cacheDc1Statistics, cacheDc2Statistics, channelStatisticsCrossDc,
                sessions01, sessions02, remoteSessions01, remoteSessions02, 40l);
    }


    // USER OPERATIONS

    @Test
    public void testUserRemoveSessions(
            @JmxInfinispanCacheStatistics(dc=DC.FIRST, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.SESSION_CACHE_NAME) InfinispanStatistics cacheDc1Statistics,
            @JmxInfinispanCacheStatistics(dc=DC.SECOND, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.SESSION_CACHE_NAME) InfinispanStatistics cacheDc2Statistics,
            @JmxInfinispanChannelStatistics() InfinispanStatistics channelStatisticsCrossDc) throws Exception {
        createInitialSessions(InfinispanConnectionProvider.SESSION_CACHE_NAME, false, cacheDc1Statistics, cacheDc2Statistics, true);

//        log.infof("Sleeping!");
//        Thread.sleep(10000000);

        channelStatisticsCrossDc.reset();

        // Remove test user
        ApiUtil.findUserByUsernameId(getAdminClient().realm(REALM_NAME), "login-test").remove();


        // Assert sessions removed on node1 and node2 and on remote caches. Assert that count of messages sent between DCs is not too big.
        assertStatisticsExpected("After user remove", InfinispanConnectionProvider.SESSION_CACHE_NAME, cacheDc1Statistics, cacheDc2Statistics, channelStatisticsCrossDc,
                sessions01, sessions02, remoteSessions01, remoteSessions02, 40l);
    }


    @Test
    public void testUserRemoveOfflineSessions(
            @JmxInfinispanCacheStatistics(dc=DC.FIRST, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME) InfinispanStatistics cacheDc1Statistics,
            @JmxInfinispanCacheStatistics(dc=DC.SECOND, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME) InfinispanStatistics cacheDc2Statistics,
            @JmxInfinispanChannelStatistics() InfinispanStatistics channelStatisticsCrossDc) throws Exception {
        createInitialSessions(InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME, true, cacheDc1Statistics, cacheDc2Statistics, true);

//        log.infof("Sleeping!");
//        Thread.sleep(10000000);

        channelStatisticsCrossDc.reset();

        // Remove test user
        ApiUtil.findUserByUsernameId(getAdminClient().realm(REALM_NAME), "login-test").remove();


        // Assert sessions removed on node1 and node2 and on remote caches. Assert that count of messages sent between DCs is not too big.
        assertStatisticsExpected("After user remove", InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME, cacheDc1Statistics, cacheDc2Statistics, channelStatisticsCrossDc,
                sessions01, sessions02, remoteSessions01, remoteSessions02, 40l);
    }


    @Test
    public void testLogoutUser(
            @JmxInfinispanCacheStatistics(dc=DC.FIRST, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.SESSION_CACHE_NAME) InfinispanStatistics cacheDc1Statistics,
            @JmxInfinispanCacheStatistics(dc=DC.SECOND, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.SESSION_CACHE_NAME) InfinispanStatistics cacheDc2Statistics,
            @JmxInfinispanChannelStatistics() InfinispanStatistics channelStatisticsCrossDc) throws Exception {

        createInitialSessions(InfinispanConnectionProvider.SESSION_CACHE_NAME, false, cacheDc1Statistics, cacheDc2Statistics, true);

        channelStatisticsCrossDc.reset();

        // Logout single session of user first
        UserResource user = ApiUtil.findUserByUsernameId(getAdminClient().realm(REALM_NAME), "login-test");
        UserSessionRepresentation userSession = user.getUserSessions().get(0);
        getAdminClient().realm(REALM_NAME).deleteSession(userSession.getId());

        // Just one session expired. Limit 5 for sent_messages is just if "lastSessionRefresh" periodic thread happened
        assertStatisticsExpected("After logout single session", InfinispanConnectionProvider.SESSION_CACHE_NAME, cacheDc1Statistics, cacheDc2Statistics, channelStatisticsCrossDc,
                sessions01 + SESSIONS_COUNT - 1, sessions02 + SESSIONS_COUNT - 1, remoteSessions01 + SESSIONS_COUNT - 1, remoteSessions02 + SESSIONS_COUNT - 1, 5l);

        // Logout all sessions for user now
        user.logout();

        // Assert sessions removed on node1 and node2 and on remote caches. Assert that count of messages sent between DCs is not too big.
        assertStatisticsExpected("After user logout", InfinispanConnectionProvider.SESSION_CACHE_NAME, cacheDc1Statistics, cacheDc2Statistics, channelStatisticsCrossDc,
                sessions01, sessions02, remoteSessions01, remoteSessions02, 40l);
    }


    @Test
    public void testLogoutUserWithFailover(
            @JmxInfinispanCacheStatistics(dc=DC.FIRST, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.SESSION_CACHE_NAME) InfinispanStatistics cacheDc1Statistics,
            @JmxInfinispanCacheStatistics(dc=DC.SECOND, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.SESSION_CACHE_NAME) InfinispanStatistics cacheDc2Statistics,
            @JmxInfinispanChannelStatistics() InfinispanStatistics channelStatisticsCrossDc) throws Exception {

        // Start node2 on first DC
        startBackendNode(DC.FIRST, 1);

        // Don't include remote stats. Size is smaller because of distributed cache
        List<OAuthClient.AccessTokenResponse> responses = createInitialSessions(InfinispanConnectionProvider.SESSION_CACHE_NAME, false, cacheDc1Statistics, cacheDc2Statistics, false);

        // Kill node2 now. Around 10 sessions (half of SESSIONS_COUNT) will be lost on Keycloak side. But not on infinispan side
        stopBackendNode(DC.FIRST, 1);

        channelStatisticsCrossDc.reset();

        // Increase offset a bit to ensure logout happens later then token issued time
        setTimeOffset(10);

        // Logout user
        ApiUtil.findUserByUsernameId(getAdminClient().realm(REALM_NAME), "login-test").logout();

        // Assert it's not possible to refresh sessions. Works because user.notBefore
        int i = 0;
        for (OAuthClient.AccessTokenResponse response : responses) {
            i++;
            OAuthClient.AccessTokenResponse refreshTokenResponse = oauth.doRefreshTokenRequest(response.getRefreshToken(), "password");
            Assert.assertNull("Failed in iteration " + i, refreshTokenResponse.getRefreshToken());
            Assert.assertNotNull("Failed in iteration " + i, refreshTokenResponse.getError());
        }
    }



    // AUTH SESSIONS

    @Test
    public void testPeriodicExpirationAuthSessions(
            @JmxInfinispanCacheStatistics(dc=DC.FIRST, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME) InfinispanStatistics cacheDc1Statistics,
            @JmxInfinispanCacheStatistics(dc=DC.SECOND, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME) InfinispanStatistics cacheDc2Statistics,
            @JmxInfinispanChannelStatistics() InfinispanStatistics channelStatisticsCrossDc) throws Exception {
        createInitialAuthSessions();

        channelStatisticsCrossDc.reset();

        // Remove expired in DC0 and DC1
        getTestingClientForStartedNodeInDc(0).testing().removeExpired(REALM_NAME);
        getTestingClientForStartedNodeInDc(1).testing().removeExpired(REALM_NAME);

        // Nothing yet expired. Limit 5 for sent_messages is just if "lastSessionRefresh" periodic thread happened
        assertAuthSessionsStatisticsExpected("After remove expired auth sessions - 1", channelStatisticsCrossDc,
                SESSIONS_COUNT, 5l);

        // Set time offset
        setTimeOffset(10000000);

        channelStatisticsCrossDc.reset();

        // Remove expired in DC0 and DC1. Need to trigger it on both!
        getTestingClientForStartedNodeInDc(0).testing().removeExpired(REALM_NAME);
        getTestingClientForStartedNodeInDc(1).testing().removeExpired(REALM_NAME);

        // Assert sessions removed on node1 and node2 and on remote caches. Assert that count of messages sent between DCs is not too big.
        assertAuthSessionsStatisticsExpected("After remove expired auth sessions - 2", channelStatisticsCrossDc,
                0, 5l);

    }


    // Return last used accessTokenResponse
    private void createInitialAuthSessions() throws Exception {

        // Enable second DC
        enableDcOnLoadBalancer(DC.SECOND);

        // Check sessions count before test
        authSessions01 = getTestingClientForStartedNodeInDc(0).testing().cache(InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME).size();
        authSessions02 = getTestingClientForStartedNodeInDc(1).testing().cache(InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME).size();
        log.infof("Before creating authentication sessions: authSessions01: %d, authSessions02: %d", authSessions01, authSessions02);

        // Create 20 authentication sessions
        oauth.realm(REALM_NAME);

        for (int i=0 ; i<SESSIONS_COUNT ; i++) {
            oauth.openLoginForm();
            driver.manage().deleteAllCookies();
        }

        // Assert 20 authentication sessions exists on node1 and node2 and on remote caches
        Retry.execute(() -> {
            int authSessions11 = getTestingClientForStartedNodeInDc(0).testing().cache(InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME).size();
            int authSessions12 = getTestingClientForStartedNodeInDc(1).testing().cache(InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME).size();
            log.infof("After creating authentication sessions: sessions11: %d, authSessions12: %d", authSessions11, authSessions12);

            // There are 20 new authentication sessions created totally in both datacenters
            int diff1 = authSessions11 - authSessions01;
            int diff2 = authSessions12 - authSessions02;
            Assert.assertEquals(SESSIONS_COUNT, diff1 + diff2);
        }, 50, 50);
    }


    private void assertAuthSessionsStatisticsExpected(String messagePrefix, InfinispanStatistics channelStatisticsCrossDc,
                                          int expectedAuthSessionsCountDiff, long sentMessagesHigherBound) {
        Retry.execute(() -> {
            int authSessions1 = getTestingClientForStartedNodeInDc(0).testing().cache(InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME).size();
            int authSessions2 = getTestingClientForStartedNodeInDc(1).testing().cache(InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME).size();
            long messagesCount = (Long) channelStatisticsCrossDc.getSingleStatistics(InfinispanStatistics.Constants.STAT_CHANNEL_SENT_MESSAGES);
            log.infof(messagePrefix + ": authSessions1: %d, authSessions2: %d, sentMessages: %d", authSessions1, authSessions2, messagesCount);

            int diff1 = authSessions1 - authSessions01;
            int diff2 = authSessions2 - authSessions02;

            Assert.assertEquals(expectedAuthSessionsCountDiff, diff1 + diff2);

            // Workaround...
            if (sentMessagesHigherBound > 5) {
                Assert.assertThat(messagesCount, Matchers.greaterThan(0l));
            }

            Assert.assertThat(messagesCount, Matchers.lessThan(sentMessagesHigherBound));
        }, 50, 50);
    }


    @Test
    public void testRealmRemoveAuthSessions(
            @JmxInfinispanChannelStatistics() InfinispanStatistics channelStatisticsCrossDc) throws Exception {

        createInitialAuthSessions();

        channelStatisticsCrossDc.reset();

        // Remove test realm
        getAdminClient().realm(REALM_NAME).remove();

        // Assert sessions removed on node1 and node2 and on remote caches. Assert that count of messages sent between DCs is not too big, however there are some messages due to removed realm
        assertAuthSessionsStatisticsExpected("After realm removed", channelStatisticsCrossDc,
                0, 40l);
    }


    @Test
    public void testClientRemoveAuthSessions(
            @JmxInfinispanChannelStatistics() InfinispanStatistics channelStatisticsCrossDc) throws Exception {

        createInitialAuthSessions();

        channelStatisticsCrossDc.reset();

        // Remove test-app client
        ApiUtil.findClientByClientId(getAdminClient().realm(REALM_NAME), "test-app").remove();

        // Assert sessions removed on node1 and node2 and on remote caches. Assert that count of messages sent between DCs is not too big, however there are some messages due to removed client
        assertAuthSessionsStatisticsExpected("After client removed", channelStatisticsCrossDc,
                0, 5l);
    }




}
