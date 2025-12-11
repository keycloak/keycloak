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
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Test;

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

        oauthClient.doPasswordGrantRequest(testUser.getUsername(), "invalid");
        oauthClient.doPasswordGrantRequest(testUser.getUsername(), "invalid");
        oauthClient.doPasswordGrantRequest(testUser.getUsername(), "invalid");

        oauthClient.doPasswordGrantRequest(testUser2.getUsername(), "invalid");
        oauthClient.doPasswordGrantRequest(testUser2.getUsername(), "invalid");
        oauthClient.doPasswordGrantRequest("nosuchuser", "invalid");

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
        assertEquals(6, status.size());
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

    private static class AttackDetectionResourceRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.bruteForceProtected(true);
            realm.failureFactor(2);

            return realm;
        }
    }

}
