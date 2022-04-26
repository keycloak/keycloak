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

package org.keycloak.testsuite.admin.event;

import org.junit.Before;
import org.junit.ComparisonFailure;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.AssertAdminEvents;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.utils.tls.TLSUtils;

import java.util.Collections;
import java.util.List;
import org.junit.After;

import static org.keycloak.testsuite.auth.page.AuthRealm.ADMIN;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

/**
 * Test authDetails in admin events
 * 
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AdminEventAuthDetailsTest extends AbstractAuthTest {

    @Rule
    public AssertAdminEvents assertAdminEvents = new AssertAdminEvents(this);

    private String masterAdminCliUuid;
    private String masterAdminUserId;
    private String masterAdminUser2Id;

    private String testRealmId;
    private String masterRealmId;
    private String client1Uuid;
    private String adminCliUuid;
    private String admin1Id;
    private String admin2Id;
    private String appUserId;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmBuilder realm = RealmBuilder.create().name("test").testEventListener();
        client1Uuid = KeycloakModelUtils.generateId();
        realm.client(ClientBuilder.create().id(client1Uuid).clientId("client1").publicClient().directAccessGrants());

        admin1Id =  KeycloakModelUtils.generateId();
        realm.user(UserBuilder.create().id(admin1Id).username("admin1").password("password").role(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN));

        admin2Id =  KeycloakModelUtils.generateId();
        realm.user(UserBuilder.create().id(admin2Id).username("admin2").password("password").role(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN));

        appUserId =  KeycloakModelUtils.generateId();
        realm.user(UserBuilder.create().id(appUserId).username("app-user").password("password"));

        testRealms.add(realm.build());
    }

    @Before
    public void initConfig() {
        RealmResource masterRealm = adminClient.realm(MASTER);
        masterRealmId = masterRealm.toRepresentation().getId();
        masterAdminCliUuid = ApiUtil.findClientByClientId(masterRealm, Constants.ADMIN_CLI_CLIENT_ID).toRepresentation().getId();
        masterAdminUserId = ApiUtil.findUserByUsername(masterRealm, "admin").getId();
        masterAdminUser2Id = ApiUtil.createUserAndResetPasswordWithAdminClient(masterRealm, UserBuilder.create().username("admin2").build(), "password");
        masterRealm.users().get(masterAdminUser2Id).roles().realmLevel().add(Collections.singletonList(masterRealm.roles().get("admin").toRepresentation()));

        RealmResource testRealm = adminClient.realm("test");
        testRealmId = testRealm.toRepresentation().getId();
        adminCliUuid = ApiUtil.findClientByClientId(testRealm, Constants.ADMIN_CLI_CLIENT_ID).toRepresentation().getId();
    }

    @After
    public void cleanUp() {
        adminClient.realm(MASTER).users().get(masterAdminUser2Id).remove();
    }

    @Test
    public void testAuth() {
        testClient(MASTER, ADMIN, ADMIN, Constants.ADMIN_CLI_CLIENT_ID, masterRealmId, masterAdminCliUuid, masterAdminUserId);
        testClient(MASTER, "admin2", "password", Constants.ADMIN_CLI_CLIENT_ID, masterRealmId, masterAdminCliUuid, masterAdminUser2Id);

        testClient("test", "admin1", "password", Constants.ADMIN_CLI_CLIENT_ID, testRealmId, adminCliUuid, admin1Id);
        testClient("test", "admin2", "password", Constants.ADMIN_CLI_CLIENT_ID, testRealmId, adminCliUuid, admin2Id);
        testClient("test", "admin1", "password", "client1", testRealmId, client1Uuid, admin1Id);
        testClient("test", "admin2", "password", "client1", testRealmId, client1Uuid, admin2Id);

        // Should fail due to different client UUID
        try {
            testClient("test", "admin1", "password", "client1", testRealmId, adminCliUuid, admin1Id);
            Assert.fail("Not expected to pass");
        } catch (ComparisonFailure expected) {
            // expected
        }

        // Should fail due to different user ID
        try {
            testClient("test", "admin1", "password", "client1", testRealmId, client1Uuid, admin2Id);
            Assert.fail("Not expected to pass");
        } catch (ComparisonFailure expected) {
            // expected
        }

    }

    private void testClient(String realmName, String username, String password, String clientId, String expectedRealmId, String expectedClientUuid, String expectedUserId) {
        try (Keycloak keycloak = Keycloak.getInstance(getAuthServerContextRoot() + "/auth",
                realmName, username, password, clientId, TLSUtils.initializeTLS())) {
            UserRepresentation rep = UserBuilder.create().id(appUserId).username("app-user").email("foo@email.org").build();
            keycloak.realm("test").users().get(appUserId).update(rep);

            assertAdminEvents.expect()
                    .realmId(testRealmId)
                    .operationType(OperationType.UPDATE)
                    .resourcePath(AdminEventPaths.userResourcePath(appUserId))
                    .resourceType(ResourceType.USER)
                    .representation(rep)
                    .authDetails(expectedRealmId, expectedClientUuid, expectedUserId)
                    .assertEvent();
        }
    }
}
