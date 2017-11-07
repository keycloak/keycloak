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


import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.common.util.Retry;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.rest.representation.RemoteCacheStats;
import org.keycloak.testsuite.util.OAuthClient;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LastSessionRefreshCrossDCTest extends AbstractAdminCrossDCTest {

    @Test
    public void testRevokeRefreshToken() {
        // Enable revokeRefreshToken
        RealmRepresentation realmRep = testRealm().toRepresentation();
        realmRep.setRevokeRefreshToken(true);
        testRealm().update(realmRep);

        // Enable second DC
        enableDcOnLoadBalancer(DC.SECOND);

        // Login
        OAuthClient.AuthorizationEndpointResponse response1 = oauth.doLogin("test-user@localhost", "password");
        String code = response1.getCode();
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        Assert.assertNotNull(tokenResponse.getAccessToken());
        String sessionId = oauth.verifyToken(tokenResponse.getAccessToken()).getSessionState();
        String refreshToken1 = tokenResponse.getRefreshToken();


        // Get statistics
        int lsr00 = getTestingClientForStartedNodeInDc(0).testing("test").getLastSessionRefresh("test", sessionId);
        int lsr10 = getTestingClientForStartedNodeInDc(1).testing("test").getLastSessionRefresh("test", sessionId);
        int lsrr0 = getTestingClientForStartedNodeInDc(0).testing("test").cache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME).getRemoteCacheLastSessionRefresh(sessionId);
        log.infof("lsr00: %d, lsr10: %d, lsrr0: %d", lsr00, lsr10, lsrr0);

        Assert.assertEquals(lsr00, lsr10);
        Assert.assertEquals(lsr00, lsrr0);


        // Set time offset to some point in future. TODO This won't be needed once we have single-use cache based solution for refresh tokens
        setTimeOffset(10);

        // refresh token on DC0
        disableDcOnLoadBalancer(DC.SECOND);
        tokenResponse = oauth.doRefreshTokenRequest(refreshToken1, "password");
        String refreshToken2 = tokenResponse.getRefreshToken();

        // Assert times changed on DC0, DC1 and remoteCache
        Retry.execute(() -> {
            int lsr01 = getTestingClientForStartedNodeInDc(0).testing("test").getLastSessionRefresh("test", sessionId);
            int lsr11 = getTestingClientForStartedNodeInDc(1).testing("test").getLastSessionRefresh("test", sessionId);
            int lsrr1 = getTestingClientForStartedNodeInDc(0).testing("test").cache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME).getRemoteCacheLastSessionRefresh(sessionId);
            log.infof("lsr01: %d, lsr11: %d, lsrr1: %d", lsr01, lsr11, lsrr1);
            
            Assert.assertEquals(lsr01, lsr11);
            Assert.assertEquals(lsr01, lsrr1);
            Assert.assertTrue(lsr01 > lsr00);
        }, 50, 50);

        // try refresh with old token on DC1. It should fail.
        disableDcOnLoadBalancer(DC.FIRST);
        enableDcOnLoadBalancer(DC.SECOND);
        tokenResponse = oauth.doRefreshTokenRequest(refreshToken1, "password");
        Assert.assertNull("Expecting no access token present", tokenResponse.getAccessToken());
        Assert.assertNotNull(tokenResponse.getError());

        // try refresh with new token on DC1. It should pass.
        tokenResponse = oauth.doRefreshTokenRequest(refreshToken2, "password");
        Assert.assertNotNull(tokenResponse.getAccessToken());
        Assert.assertNull(tokenResponse.getError());

        // Revert
        realmRep = testRealm().toRepresentation();
        realmRep.setRevokeRefreshToken(false);
        testRealm().update(realmRep);
    }


    @Test
    public void testLastSessionRefreshUpdate() {
        // Disable DC1 on loadbalancer
        disableDcOnLoadBalancer(DC.SECOND);

        // Get statistics
        int stores0 = getRemoteCacheStats(0).getGlobalStores();

        // Login
        OAuthClient.AuthorizationEndpointResponse response1 = oauth.doLogin("test-user@localhost", "password");
        String code = response1.getCode();
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        Assert.assertNotNull(tokenResponse.getAccessToken());
        String sessionId = oauth.verifyToken(tokenResponse.getAccessToken()).getSessionState();
        String refreshToken1 = tokenResponse.getRefreshToken();


        // Get statistics
        this.suiteContext.getDcAuthServerBackendsInfo().get(0).stream()
                .filter(ContainerInfo::isStarted).findFirst().get();

        AtomicInteger stores1 = new AtomicInteger(-1);
        Retry.execute(() -> {
            stores1.set(getRemoteCacheStats(0).getGlobalStores());
            log.infof("stores0=%d, stores1=%d", stores0, stores1.get());
            Assert.assertTrue(stores1.get() > stores0);
        }, 50, 50);

        int lsr00 = getTestingClientForStartedNodeInDc(0).testing("test").getLastSessionRefresh("test", sessionId);
        int lsr10 = getTestingClientForStartedNodeInDc(1).testing("test").getLastSessionRefresh("test", sessionId);
        Assert.assertEquals(lsr00, lsr10);

        // Set time offset to some point in future.
        setTimeOffset(10);

        // refresh token on DC0
        tokenResponse = oauth.doRefreshTokenRequest(refreshToken1, "password");
        String refreshToken2 = tokenResponse.getRefreshToken();

        // assert that hotrod statistics were NOT updated
        AtomicInteger stores2 = new AtomicInteger(-1);

        // TODO: not sure why stores2 < stores1 at first run. Probably should be replaced with JMX statistics
        Retry.execute(() -> {
            stores2.set(getRemoteCacheStats(0).getGlobalStores());
            log.infof("stores1=%d, stores2=%d", stores1.get(), stores2.get());
            Assert.assertEquals(stores1.get(), stores2.get());
        }, 50, 50);

        // assert that lastSessionRefresh on DC0 updated, but on DC1 still the same
        AtomicInteger lsr01 = new AtomicInteger(-1);
        AtomicInteger lsr11 = new AtomicInteger(-1);
        Retry.execute(() -> {
            lsr01.set(getTestingClientForStartedNodeInDc(0).testing("test").getLastSessionRefresh("test", sessionId));
            lsr11.set(getTestingClientForStartedNodeInDc(1).testing("test").getLastSessionRefresh("test", sessionId));
            log.infof("lsr01: %d, lsr11: %d", lsr01.get(), lsr11.get());
            Assert.assertTrue(lsr01.get() > lsr00);
        }, 50, 100);
        Assert.assertEquals(lsr10, lsr11.get());

        // assert that lastSessionRefresh still the same on remoteCache
        int lsrr1 = getTestingClientForStartedNodeInDc(0).testing("test").cache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME).getRemoteCacheLastSessionRefresh(sessionId);
        Assert.assertEquals(lsr00, lsrr1);
        log.infof("lsrr1: %d", lsrr1);

        // setTimeOffset to greater value
        setTimeOffset(100);

        // refresh token
        tokenResponse = oauth.doRefreshTokenRequest(refreshToken1, "password");

        // assert that lastSessionRefresh on both DC0 and DC1 was updated, but on remoteCache still the same
        AtomicInteger lsr02 = new AtomicInteger(-1);
        AtomicInteger lsr12 = new AtomicInteger(-1);
        AtomicInteger lsrr2 = new AtomicInteger(-1);
        AtomicInteger stores3 = new AtomicInteger(-1);
        Retry.execute(() -> {
            lsr02.set(getTestingClientForStartedNodeInDc(0).testing("test").getLastSessionRefresh("test", sessionId));
            lsr12.set(getTestingClientForStartedNodeInDc(1).testing("test").getLastSessionRefresh("test", sessionId));
            log.infof("lsr02: %d, lsr12: %d", lsr02.get(), lsr12.get());
            Assert.assertEquals(lsr02.get(), lsr12.get());
            Assert.assertTrue(lsr02.get() > lsr01.get());
            Assert.assertTrue(lsr12.get() > lsr11.get());

            lsrr2.set(getTestingClientForStartedNodeInDc(0).testing("test").cache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME).getRemoteCacheLastSessionRefresh(sessionId));
            log.infof("lsrr2: %d", lsrr2.get());
            Assert.assertEquals(lsrr1, lsrr2.get());

            // assert that hotrod statistics were NOT updated on DC0
            stores3.set(getRemoteCacheStats(0).getGlobalStores());
            log.infof("stores2=%d, stores3=%d", stores2.get(), stores3.get());
            Assert.assertEquals(stores2.get(), stores3.get());
        }, 50, 100);

        // Increase time offset even more
        setTimeOffset(1500);

        // refresh token
        tokenResponse = oauth.doRefreshTokenRequest(refreshToken1, "password");
        Assert.assertNull("Error: " + tokenResponse.getError() + ", error description: " + tokenResponse.getErrorDescription(), tokenResponse.getError());
        Assert.assertNotNull(tokenResponse.getRefreshToken());

        // assert that lastSessionRefresh updated everywhere including remoteCache
        AtomicInteger lsr03 = new AtomicInteger(-1);
        AtomicInteger lsr13 = new AtomicInteger(-1);
        AtomicInteger lsrr3 = new AtomicInteger(-1);
        AtomicInteger stores4 = new AtomicInteger(-1);
        Retry.execute(() -> {
            lsr03.set(getTestingClientForStartedNodeInDc(0).testing("test").getLastSessionRefresh("test", sessionId));
            lsr13.set(getTestingClientForStartedNodeInDc(1).testing("test").getLastSessionRefresh("test", sessionId));
            log.infof("lsr03: %d, lsr13: %d", lsr03.get(), lsr13.get());
            Assert.assertEquals(lsr03.get(), lsr13.get());
            Assert.assertTrue(lsr03.get() > lsr02.get());
            Assert.assertTrue(lsr13.get() > lsr12.get());

            lsrr3.set(getTestingClientForStartedNodeInDc(0).testing("test").cache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME).getRemoteCacheLastSessionRefresh(sessionId));
            log.infof("lsrr3: %d", lsrr3.get());
            Assert.assertTrue(lsrr3.get() > lsrr2.get());

            // assert that hotrod statistics were NOT updated on DC0
            stores4.set(getRemoteCacheStats(0).getGlobalStores());
            log.infof("stores3=%d, stores4=%d", stores3.get(), stores4.get());
            Assert.assertTrue(stores4.get() > stores3.get());
        }, 50, 100);
    }


    private RemoteCacheStats getRemoteCacheStats(int dcIndex) {
        return getTestingClientForStartedNodeInDc(dcIndex).testing("test")
                .cache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME)
                .getRemoteCacheStats();
    }

}
