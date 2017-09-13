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

package org.keycloak.testsuite.crossdc.manual;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.crossdc.AbstractAdminCrossDCTest;
import org.keycloak.testsuite.crossdc.DC;
import org.keycloak.testsuite.util.OAuthClient;

/**
 * Tests userSessions and offline sessions preloading at startup
 *
 * This test requires that lifecycle of infinispan/JDG servers is managed by testsuite, so you need to run with:
 *
 * -Dmanual.mode=true
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SessionsPreloadCrossDCTest extends AbstractAdminCrossDCTest {

    private static final int SESSIONS_COUNT = 10;

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        // Doublecheck we are in manual mode
        Assert.assertTrue("The test requires to be executed with manual.mode=true", suiteContext.getCacheServersInfo().get(0).isManual());

        stopAllCacheServersAndAuthServers();

        // Start DC1 only
        containerController.start(getCacheServer(DC.FIRST).getQualifier());
        startBackendNode(DC.FIRST, 0);
        enableLoadBalancerNode(DC.FIRST, 0);

        super.beforeAbstractKeycloakTest();
    }


    // Override as we are in manual mode
    @Override
    public void enableOnlyFirstNodeInFirstDc() {
    }


    // Override as we are in manual mode
    @Override
    public void terminateManuallyStartedServers() {
    }




    @Override
    public void afterAbstractKeycloakTest() {
        super.afterAbstractKeycloakTest();

        // Remove realms now. In @AfterClass servers are already shutdown
        AuthServerTestEnricher.removeTestRealms(testContext, adminClient);
        testContext.setTestRealmReps(null);

        adminClient.close();
        adminClient = null;
        testContext.setAdminClient(null);

        stopAllCacheServersAndAuthServers();
    }

    private void stopAllCacheServersAndAuthServers() {
        log.infof("Going to stop all auth servers");

        stopBackendNode(DC.FIRST, 0);
        disableLoadBalancerNode(DC.FIRST, 0);
        stopBackendNode(DC.SECOND, 0);
        disableLoadBalancerNode(DC.SECOND, 0);

        log.infof("Auth servers stopped successfully. Going to stop all cache servers");

        suiteContext.getCacheServersInfo().stream()
                .filter(containerInfo -> containerInfo.isStarted())
                .forEach(containerInfo -> {
                    stopCacheServer(containerInfo);
                });

        log.infof("Cache servers stopped successfully");
    }


    @Test
    public void sessionsPreloadTest() throws Exception {
        int sessionsBefore = getTestingClientForStartedNodeInDc(0).testing().cache(InfinispanConnectionProvider.SESSION_CACHE_NAME).size();
        log.infof("sessionsBefore: %d", sessionsBefore);

        // Create initial sessions
        List<OAuthClient.AccessTokenResponse> tokenResponses = createInitialSessions(false);

        // Start 2nd DC.
        containerController.start(getCacheServer(DC.SECOND).getQualifier());
        startBackendNode(DC.SECOND, 0);
        enableLoadBalancerNode(DC.SECOND, 0);

        // Ensure sessions are loaded in both 1st DC and 2nd DC
        int sessions01 = getTestingClientForStartedNodeInDc(0).testing().cache(InfinispanConnectionProvider.SESSION_CACHE_NAME).size();
        int sessions02 = getTestingClientForStartedNodeInDc(1).testing().cache(InfinispanConnectionProvider.SESSION_CACHE_NAME).size();
        log.infof("sessions01: %d, sessions02: %d", sessions01, sessions02);
        Assert.assertEquals(sessions01, sessionsBefore + SESSIONS_COUNT);
        Assert.assertEquals(sessions02, sessionsBefore + SESSIONS_COUNT);

        // On DC2 sessions were preloaded from from remoteCache
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
        int offlineSessionsBefore = getTestingClientForStartedNodeInDc(0).testing().cache(InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME).size();
        log.infof("offlineSessionsBefore: %d", offlineSessionsBefore);

        // Create initial sessions
        List<OAuthClient.AccessTokenResponse> tokenResponses = createInitialSessions(true);

        int offlineSessions01 = getTestingClientForStartedNodeInDc(0).testing().cache(InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME).size();
        Assert.assertEquals(offlineSessions01, offlineSessionsBefore + SESSIONS_COUNT);
        log.infof("offlineSessions01: %d", offlineSessions01);

        // Stop Everything
        stopAllCacheServersAndAuthServers();

        // Start DC1. Sessions should be preloaded from DB
        containerController.start(getCacheServer(DC.FIRST).getQualifier());
        startBackendNode(DC.FIRST, 0);
        enableLoadBalancerNode(DC.FIRST, 0);

        // Start DC2. Sessions should be preloaded from remoteCache
        containerController.start(getCacheServer(DC.SECOND).getQualifier());
        startBackendNode(DC.SECOND, 0);
        enableLoadBalancerNode(DC.SECOND, 0);

        // Ensure sessions are loaded in both 1st DC and 2nd DC
        int offlineSessions11 = getTestingClientForStartedNodeInDc(0).testing().cache(InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME).size();
        int offlineSessions12 = getTestingClientForStartedNodeInDc(1).testing().cache(InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME).size();
        log.infof("offlineSessions11: %d, offlineSessions12: %d", offlineSessions11, offlineSessions12);
        Assert.assertEquals(offlineSessions11, offlineSessionsBefore + SESSIONS_COUNT);
        Assert.assertEquals(offlineSessions12, offlineSessionsBefore + SESSIONS_COUNT);

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
        containerController.start(getCacheServer(DC.SECOND).getQualifier());
        startBackendNode(DC.SECOND, 0);
        enableLoadBalancerNode(DC.SECOND, 0);

        // Ensure loginFailures are loaded in both 1st DC and 2nd DC
        int size1 = getTestingClientForStartedNodeInDc(0).testing().cache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME).size();
        int size2 = getTestingClientForStartedNodeInDc(1).testing().cache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME).size();
        int loginFailures1 = (Integer) getAdminClientForStartedNodeInDc(0).realm("test").attackDetection().bruteForceUserStatus(userId).get("numFailures");
        int loginFailures2 = (Integer) getAdminClientForStartedNodeInDc(1).realm("test").attackDetection().bruteForceUserStatus(userId).get("numFailures");
        log.infof("size1: %d, size2: %d, loginFailures1: %d, loginFailures2: %d", size1, size2, loginFailures1, loginFailures2);
        Assert.assertEquals(size1, 1);
        Assert.assertEquals(size2, 1);
        Assert.assertEquals(loginFailures1, loginFailuresBefore + SESSIONS_COUNT);
        Assert.assertEquals(loginFailures2, loginFailuresBefore + SESSIONS_COUNT);

        // On DC2 sessions were preloaded from from remoteCache
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
