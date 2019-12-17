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
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.Retry;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.concurrency.AbstractConcurrencyTest;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BruteForceCrossDCTest extends AbstractAdminCrossDCTest {

    private static final String REALM_NAME = "brute-force-test";

    @Before
    public void beforeTest() {
        log.debug("--DC: creating test realm");
        try {
            adminClient.realm(REALM_NAME).remove();
        } catch (NotFoundException ignore) {
        }

        UserRepresentation user = UserBuilder.create()
                .id("login-test-1")
                .username("login-test-1")
                .email("login-1@test.com")
                .enabled(true)
                .password("password")
                .addRoles(Constants.OFFLINE_ACCESS_ROLE)
                .build();

        UserRepresentation user2 = UserBuilder.create()
                .id("login-test-2")
                .username("login-test-2")
                .email("login-2@test.com")
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
                .user(user2)
                .client(client)
                .bruteForceProtected(true)
                .build();

        adminClient.realms().create(realmRep);
    }


    @Test
    public void testBruteForceWithUserOperations() throws Exception {
        // Enable 1st node on each DC only
        enableDcOnLoadBalancer(DC.FIRST);
        enableDcOnLoadBalancer(DC.SECOND);

        // Clear all
        adminClient.realms().realm(REALM_NAME).attackDetection().clearAllBruteForce();
        assertStatistics("After brute force cleared", 0, 0, 0);

        // Create 10 brute force statuses for user1. Assert available on both DC1 and DC2
        createBruteForceFailures(10, "login-test-1");
        assertStatistics("After brute force for user1 created", 10, 0, 1);

        // Create 10 brute force statuses for user2. Assert available on both DC1 and DC2createBruteForceFailures(10, "login-test-2");createBruteForceFailures(10, "login-test-2");
        createBruteForceFailures(10, "login-test-2");
        assertStatistics("After brute force for user2 created", 10, 10, 2);

        // Remove brute force for user1
        adminClient.realms().realm(REALM_NAME).attackDetection().clearBruteForceForUser("login-test-1");
        assertStatistics("After brute force for user1 cleared", 0, 10, 1);

        // Re-add 10 brute force statuses for user1
        createBruteForceFailures(10, "login-test-1");
        assertStatistics("After brute force for user1 re-created", 10, 10, 2);

        // Remove user1
        adminClient.realms().realm(REALM_NAME).users().get("login-test-1").remove();
        assertStatistics("After user1 removed", 0, 10, 1);
    }


    @Test
    public void testBruteForceWithRealmOperations() throws Exception {
        // Enable 1st node on each DC only
        enableDcOnLoadBalancer(DC.FIRST);
        enableDcOnLoadBalancer(DC.SECOND);

//        log.infof("Sleeping");
//        Thread.sleep(3600000);

        // Clear all
        adminClient.realms().realm(REALM_NAME).attackDetection().clearAllBruteForce();
        assertStatistics("After brute force cleared", 0, 0, 0);

        // Create 10 brute force statuses for user1 and user2.
        createBruteForceFailures(10, "login-test-1");
        createBruteForceFailures(10, "login-test-2");
        assertStatistics("After brute force for users created", 10, 10, 2);

        // Clear all
        adminClient.realms().realm(REALM_NAME).attackDetection().clearAllBruteForce();
        assertStatistics("After brute force cleared for realm", 0, 0, 0);

        // Re-add 10 brute force statuses for users
        createBruteForceFailures(10, "login-test-1");
        createBruteForceFailures(10, "login-test-2");
        assertStatistics("After brute force for users re-created", 10, 10, 2);

        // Remove realm
        adminClient.realms().realm(REALM_NAME).remove();

        Retry.execute(() -> {
            int dc0CacheSize = getTestingClientForStartedNodeInDc(0).testing().cache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME).size();
            int dc1CacheSize = getTestingClientForStartedNodeInDc(1).testing().cache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME).size();
            Assert.assertEquals(0, dc0CacheSize);
            Assert.assertEquals(0, dc1CacheSize);
        }, 50, 50);

    }


    @Test
    public void testDuplicatedPutIfAbsentOperation() throws Exception {
        // Enable 1st node on each DC only
        enableDcOnLoadBalancer(DC.FIRST);
        enableDcOnLoadBalancer(DC.SECOND);

        // Clear all
        adminClient.realms().realm(REALM_NAME).attackDetection().clearAllBruteForce();
        assertStatistics("After brute force cleared", 0, 0, 0);

        // create the entry manually in DC0
        addUserLoginFailure(getTestingClientForStartedNodeInDc(0));
        assertStatistics("After create entry1", 1, 0, 1);

        // try to create the entry manually in DC1 (not use real concurrency for now). It should still update the numFailures in existing entry rather then override it
        addUserLoginFailure(getTestingClientForStartedNodeInDc(1));
        assertStatistics("After create entry2", 2, 0, 1);

    }


    @Test
    public void testBruteForceConcurrentUpdate() throws Exception {
        //Thread.sleep(120000);

        // Enable 1st node on each DC only
        enableDcOnLoadBalancer(DC.FIRST);
        enableDcOnLoadBalancer(DC.SECOND);

        // Clear all
        adminClient.realms().realm(REALM_NAME).attackDetection().clearAllBruteForce();
        assertStatistics("After brute force cleared", 0, 0, 0);

        // create the entry manually in DC0
        addUserLoginFailure(getTestingClientForStartedNodeInDc(0));
        assertStatistics("After create entry1", 1, 0, 1);

        AbstractConcurrencyTest.KeycloakRunnable runnable = (int threadIndex, Keycloak keycloak, RealmResource realm1) -> {
            createBruteForceFailures(1, "login-test-1");
        };

        AbstractConcurrencyTest.run(2, 20, this, runnable);

        Retry.execute(() -> {
            int dc0user1 = (Integer) getAdminClientForStartedNodeInDc(0).realm(REALM_NAME).attackDetection().bruteForceUserStatus("login-test-1").get("numFailures");
            int dc1user1 = (Integer) getAdminClientForStartedNodeInDc(1).realm(REALM_NAME).attackDetection().bruteForceUserStatus("login-test-1").get("numFailures");

            log.infof("After concurrent update entry1: dc0User1=%d, dc1user1=%d", dc0user1, dc1user1);

            // TODO: The number of failures should be ideally exactly 21 in both DCs. Once we improve cross-dc, then improve this test and rather check for "Assert.assertEquals(dc0user1, 21)" and "Assert.assertEquals(dc1user1, 21)"
            Assert.assertThat(dc0user1, Matchers.greaterThan(11));
            Assert.assertThat(dc1user1, Matchers.greaterThan(11));
        }, 50, 50);
    }


    private void assertStatistics(String prefixMessage, int expectedUser1, int expectedUser2, int expectedCacheSize) {
        Retry.execute(() -> {
            int dc0user1 = (Integer) getAdminClientForStartedNodeInDc(0).realm(REALM_NAME).attackDetection().bruteForceUserStatus("login-test-1").get("numFailures");
            int dc1user1 = (Integer) getAdminClientForStartedNodeInDc(1).realm(REALM_NAME).attackDetection().bruteForceUserStatus("login-test-1").get("numFailures");
            int dc0user2 = (Integer) getAdminClientForStartedNodeInDc(0).realm(REALM_NAME).attackDetection().bruteForceUserStatus("login-test-2").get("numFailures");
            int dc1user2 = (Integer) getAdminClientForStartedNodeInDc(1).realm(REALM_NAME).attackDetection().bruteForceUserStatus("login-test-2").get("numFailures");

            int dc0CacheSize = getTestingClientForStartedNodeInDc(0).testing().cache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME).size();
            int dc1CacheSize = getTestingClientForStartedNodeInDc(1).testing().cache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME).size();

            log.infof("%s: dc0User1=%d, dc0user2=%d, dc1user1=%d, dc1user2=%d, dc0CacheSize=%d, dc1CacheSize=%d", prefixMessage, dc0user1, dc0user2, dc1user1, dc1user2, dc0CacheSize, dc1CacheSize);

            Assert.assertEquals(expectedUser1, dc0user1);
            Assert.assertEquals(expectedUser2, dc0user2);
            Assert.assertEquals(expectedUser1, dc1user1);
            Assert.assertEquals(expectedUser2, dc1user2);

            Assert.assertEquals(expectedCacheSize, dc0CacheSize);
            Assert.assertEquals(expectedCacheSize, dc1CacheSize);
        }, 50, 50);
    }





    private void createBruteForceFailures(int count, String username) throws Exception {
        oauth.realm(REALM_NAME);

        for (int i=0 ; i<count ; i++) {
            OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", username, "bad-password");
            Assert.assertNull(response.getAccessToken());
            Assert.assertNotNull(response.getError());
        }

    }


    // resolution on Wildfly: make deployment available on both dc0_1 and dc1_1, see @Deployment methods
    private void addUserLoginFailure(KeycloakTestingClient testingClient) throws URISyntaxException, IOException {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName(REALM_NAME);
            UserLoginFailureModel loginFailure = session.sessions().addUserLoginFailure(realm, "login-test-1");
            loginFailure.incrementFailures();
        });
    }

}
