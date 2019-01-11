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

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.Retry;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.CrossDCTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.InitialDcState;
import org.keycloak.testsuite.util.OAuthClient;
import java.util.Set;
import org.hamcrest.Matchers;

/**
 * Tests userSessions and offline sessions preloading at startup
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@InitialDcState(authServers = ServerSetup.ALL_NODES_IN_FIRST_DC_NO_NODES_IN_SECOND_DC)
public class SessionsPreloadCrossDCTest extends AbstractAdminCrossDCTest {

    private static final int SESSIONS_COUNT = 10;

    @Before
    public void beforeSessionsPreloadCrossDCTest() throws Exception {
        disableDcOnLoadBalancer(DC.SECOND);
    }

    private void stopAllCacheServersAndAuthServers() {
        log.infof("Going to stop all auth servers");

        CrossDCTestEnricher.forAllBackendNodes(CrossDCTestEnricher::stopAuthServerBackendNode);
        loadBalancerCtrl.disableAllBackendNodes();

        log.infof("Auth servers stopped successfully. Going to stop all cache servers");

        DC.validDcsStream().forEach(CrossDCTestEnricher::stopCacheServer);
        log.infof("Cache servers stopped successfully");
    }

    @Test
    public void sessionsPreloadTest() throws Exception {
        int sessionsBefore = getTestingClientForStartedNodeInDc(0).testing().cache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME).size();
        log.infof("sessionsBefore: %d", sessionsBefore);

        // Create initial sessions
        List<OAuthClient.AccessTokenResponse> tokenResponses = createInitialSessions(false);

        // Start 2nd DC.
        CrossDCTestEnricher.startAuthServerBackendNode(DC.SECOND, 0);
        enableLoadBalancerNode(DC.SECOND, 0);

        // Ensure sessions are loaded in both 1st DC and 2nd DC
        Set<String> sessions01keys = getTestingClientForStartedNodeInDc(0).testing().cache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME).enumerateKeys();
        Set<String> sessions02keys = getTestingClientForStartedNodeInDc(1).testing().cache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME).enumerateKeys();
        log.infof("sessions01keys: %s, sessions02keys: %s", sessions01keys, sessions02keys);
        Assert.assertThat(sessions01keys, Matchers.equalTo(sessions02keys));

        // On DC2 sessions were preloaded from remoteCache
        Assert.assertTrue(getTestingClientForStartedNodeInDc(1).testing().cache(InfinispanConnectionProvider.WORK_CACHE_NAME).contains("distributed::remoteCacheLoad::sessions"));

        // Assert refreshing works
        for (OAuthClient.AccessTokenResponse resp : tokenResponses) {
            OAuthClient.AccessTokenResponse newResponse = oauth.doRefreshTokenRequest(resp.getRefreshToken(), "password");
            Assert.assertNull(newResponse.getError());
            Assert.assertNotNull(newResponse.getAccessToken());
        }
    }


    @Test
    public void offlineSessionsPreloadTest() throws Exception {
        int offlineSessionsBefore = getTestingClientForStartedNodeInDc(0).testing().cache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME).size();
        log.infof("offlineSessionsBefore: %d", offlineSessionsBefore);

        // Create initial sessions
        List<OAuthClient.AccessTokenResponse> tokenResponses = createInitialSessions(true);

        int offlineSessions01 = getTestingClientForStartedNodeInDc(0).testing().cache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME).size();
        Assert.assertEquals(offlineSessions01, offlineSessionsBefore + SESSIONS_COUNT);
        log.infof("offlineSessions01: %d", offlineSessions01);

        // Stop Everything
        stopAllCacheServersAndAuthServers();

        // Start cache containers on both DC1 and DC2
        DC.validDcsStream().forEach(CrossDCTestEnricher::startCacheServer);

        // Start Keycloak on DC1. Sessions should be preloaded from DB
        CrossDCTestEnricher.startAuthServerBackendNode(DC.FIRST, 0);
        enableLoadBalancerNode(DC.FIRST, 0);

        // Start Keycloak on DC2. Sessions should be preloaded from remoteCache
        CrossDCTestEnricher.startAuthServerBackendNode(DC.SECOND, 0);
        enableLoadBalancerNode(DC.SECOND, 0);

        // Ensure sessions are loaded in both 1st DC and 2nd DC
        Set<String> offlineSessions11keys = getTestingClientForStartedNodeInDc(0).testing().cache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME).enumerateKeys();
        Set<String> offlineSessions12keys = getTestingClientForStartedNodeInDc(1).testing().cache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME).enumerateKeys();
        log.infof("offlineSessions11keys: %s, offlineSessions12keys: %s", offlineSessions11keys, offlineSessions12keys);
        Assert.assertThat(offlineSessions11keys, Matchers.equalTo(offlineSessions12keys));

        // On DC1 sessions were preloaded from DB. On DC2 sessions were preloaded from remoteCache
        Assert.assertTrue(getTestingClientForStartedNodeInDc(0).testing().cache(InfinispanConnectionProvider.WORK_CACHE_NAME).contains("distributed::offlineUserSessions"));
        Assert.assertFalse(getTestingClientForStartedNodeInDc(0).testing().cache(InfinispanConnectionProvider.WORK_CACHE_NAME).contains("distributed::remoteCacheLoad::offlineSessions"));

        Assert.assertFalse(getTestingClientForStartedNodeInDc(1).testing().cache(InfinispanConnectionProvider.WORK_CACHE_NAME).contains("distributed::offlineUserSessions"));
        Assert.assertTrue(getTestingClientForStartedNodeInDc(1).testing().cache(InfinispanConnectionProvider.WORK_CACHE_NAME).contains("distributed::remoteCacheLoad::offlineSessions"));

        // Assert refreshing with offline tokens work
        for (OAuthClient.AccessTokenResponse resp : tokenResponses) {
            OAuthClient.AccessTokenResponse newResponse = oauth.doRefreshTokenRequest(resp.getRefreshToken(), "password");
            Assert.assertNull(newResponse.getError());
            Assert.assertNotNull(newResponse.getAccessToken());
        }
    }


    @Test
    public void loginFailuresPreloadTest() throws Exception {
        // Enable brute force protector
        RealmRepresentation realmRep = getAdminClientForStartedNodeInDc(0).realms().realm("test").toRepresentation();
        realmRep.setBruteForceProtected(true);
        getAdminClientForStartedNodeInDc(0).realms().realm("test").update(realmRep);

        String userId = ApiUtil.findUserByUsername(getAdminClientForStartedNodeInDc(0).realms().realm("test"), "test-user@localhost").getId();

        int loginFailuresBefore = (Integer) getAdminClientForStartedNodeInDc(0).realm("test").attackDetection().bruteForceUserStatus(userId).get("numFailures");
        log.infof("loginFailuresBefore: %d", loginFailuresBefore);

        // Create initial brute force records
        for (int i=0 ; i<SESSIONS_COUNT ; i++) {
            OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "bad-password");
            Assert.assertNull(response.getAccessToken());
            Assert.assertNotNull(response.getError());
        }

        // Start 2nd DC.
        CrossDCTestEnricher.startAuthServerBackendNode(DC.SECOND, 0);
        enableLoadBalancerNode(DC.SECOND, 0);

        Retry.execute(() -> {
            // Ensure loginFailures are loaded in both 1st DC and 2nd DC
            Set<String> keys1 = getTestingClientForStartedNodeInDc(0).testing().cache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME).enumerateKeys();
            Set<String> keys2 = getTestingClientForStartedNodeInDc(1).testing().cache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME).enumerateKeys();
            int loginFailures1 = (Integer) getAdminClientForStartedNodeInDc(0).realm("test").attackDetection().bruteForceUserStatus(userId).get("numFailures");
            int loginFailures2 = (Integer) getAdminClientForStartedNodeInDc(1).realm("test").attackDetection().bruteForceUserStatus(userId).get("numFailures");
            log.infof("keys1: %d, keys2: %d, loginFailures1: %d, loginFailures2: %d", keys1, keys2, loginFailures1, loginFailures2);
            Assert.assertThat(keys1, Matchers.equalTo(keys2));
            Assert.assertEquals(loginFailuresBefore + SESSIONS_COUNT, loginFailures1);
            Assert.assertEquals(loginFailuresBefore + SESSIONS_COUNT, loginFailures2);
        }, 3, 400);

        // On DC2 sessions were preloaded from remoteCache
        Assert.assertTrue(getTestingClientForStartedNodeInDc(1).testing().cache(InfinispanConnectionProvider.WORK_CACHE_NAME).contains("distributed::remoteCacheLoad::loginFailures"));

        // Disable brute force protector
        realmRep = getAdminClientForStartedNodeInDc(0).realms().realm("test").toRepresentation();
        realmRep.setBruteForceProtected(true);
        getAdminClientForStartedNodeInDc(0).realms().realm("test").update(realmRep);
    }



    private List<OAuthClient.AccessTokenResponse> createInitialSessions(boolean offline) throws Exception {
        if (offline) {
            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        }

        List<OAuthClient.AccessTokenResponse> responses = new LinkedList<>();

        for (int i=0 ; i<SESSIONS_COUNT ; i++) {
            OAuthClient.AccessTokenResponse resp = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");
            Assert.assertNull(resp.getError());
            Assert.assertNotNull(resp.getAccessToken());
            responses.add(resp);
        }

        return responses;
    }
}
