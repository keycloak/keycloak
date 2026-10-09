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

package org.keycloak.tests.admin;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.keycloak.admin.client.resource.AttackDetectionResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class AttackDetectionResourceTest {

    @InjectRealm(config = AttackDetectionResourceRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectUser
    ManagedUser testUser;

    @InjectUser(ref = "testUser2")
    ManagedUser testUser2;

    @InjectAdminEvents
    AdminEvents adminEvents;

    @InjectOAuthClient
    OAuthClient oauthClient;

    @Test
    public void test() {
        AttackDetectionResource detection = managedRealm.admin().attackDetection();

        assertBruteForce(detection.bruteForceUserStatus(testUser.getId()), 0, 0, false, false);

        // Wait for each failure to be processed before sending the next request for the same user.
        // DefaultBlockingBruteForceProtector blocks concurrent logins for the same user,
        // and the blocked request won't increment the failure counter.
        // These waits can be removed once brute force processing is synchronous and login-failures V1 is removed
        // https://github.com/keycloak/keycloak/pull/52128
        oauthClient.doPasswordGrantRequest(testUser.getUsername(), "invalid");
        awaitNumFailures(detection, testUser.getId(), 1);

        oauthClient.doPasswordGrantRequest(testUser.getUsername(), "invalid");
        awaitNumFailures(detection, testUser.getId(), 2);

        // Third attempt: user is now locked (failureFactor=2), won't increment numFailures
        oauthClient.doPasswordGrantRequest(testUser.getUsername(), "invalid");

        oauthClient.doPasswordGrantRequest(testUser2.getUsername(), "invalid");
        awaitNumFailures(detection, testUser2.getId(), 1);

        oauthClient.doPasswordGrantRequest(testUser2.getUsername(), "invalid");
        oauthClient.doPasswordGrantRequest("nosuchuser", "invalid");

        awaitNumFailures(detection, testUser2.getId(), 2);

        assertBruteForce(detection.bruteForceUserStatus(testUser.getId()), 2, 1, true, true);
        assertBruteForce(detection.bruteForceUserStatus(testUser2.getId()), 2, 1, true, true);
        assertBruteForce(detection.bruteForceUserStatus("nosuchuser"), 0, 0, false, false);

        detection.clearBruteForceForUser(testUser.getId());
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.attackDetectionClearBruteForceForUserPath(testUser.getId()), ResourceType.USER_LOGIN_FAILURE);

        assertBruteForce(detection.bruteForceUserStatus(testUser.getId()), 0, 0, false, false);
        assertBruteForce(detection.bruteForceUserStatus(testUser2.getId()), 2, 1, true, true);

        detection.clearAllBruteForce();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.attackDetectionClearAllBruteForcePath(), ResourceType.USER_LOGIN_FAILURE);

        assertBruteForce(detection.bruteForceUserStatus(testUser.getId()), 0, 0, false, false);
        assertBruteForce(detection.bruteForceUserStatus(testUser2.getId()), 0, 0, false, false);
    }

    private void assertBruteForce(Map<String, Object> status, Integer expectedNumFailures, Integer expectedNumTemporaryLockouts, Boolean expectedFailure, Boolean expectedDisabled) {
        assertEquals(7, status.size());
        assertEquals(expectedNumFailures, status.get("numFailures"));
        assertEquals(expectedNumTemporaryLockouts, status.get("numTemporaryLockouts"));
        assertEquals(expectedDisabled, status.get("disabled"));
        if (expectedFailure) {
            assertEquals("127.0.0.1", status.get("lastIPFailure"));
            Long lastFailure = (Long) status.get("lastFailure");
            assertTrue(lastFailure < (System.currentTimeMillis() + 1) && lastFailure > (System.currentTimeMillis() - 10000));
            assertNotEquals("0", status.get("failedLoginNotBefore").toString());
        } else {
            assertEquals("n/a", status.get("lastIPFailure"));
            assertEquals("0", status.get("lastFailure").toString());
            assertEquals("0", status.get("failedLoginNotBefore").toString());
        }
    }

    private void awaitNumFailures(AttackDetectionResource detection, String userId, int expected) {
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertEquals(expected, detection.bruteForceUserStatus(userId).get("numFailures")));
    }

    private static class AttackDetectionResourceRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.bruteForceProtected(true);
            realm.failureFactor(2);

            return realm;
        }
    }

}
