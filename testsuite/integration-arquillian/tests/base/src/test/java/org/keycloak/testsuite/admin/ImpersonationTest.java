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

package org.keycloak.testsuite.admin;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.Config;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.ImpersonationConstants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.CredentialBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Tests Undertow Adapter
 *
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
public class ImpersonationTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    private String impersonatedUserId;

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmBuilder realm = RealmBuilder.create().name("test").testEventListener();

        realm.client(ClientBuilder.create().clientId("myclient").publicClient().directAccessGrants());

        impersonatedUserId = KeycloakModelUtils.generateId();

        realm.user(UserBuilder.create().id(impersonatedUserId).username("test-user@localhost"));
        realm.user(UserBuilder.create().username("realm-admin").password("password").role(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN));
        realm.user(UserBuilder.create().username("impersonator").password("password").role(Constants.REALM_MANAGEMENT_CLIENT_ID, ImpersonationConstants.IMPERSONATION_ROLE).role(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_USERS));
        realm.user(UserBuilder.create().username("bad-impersonator").password("password").role(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_USERS));

        testRealms.add(realm.build());
    }

    @Test
    public void testImpersonateByMasterAdmin() {
        // test that composite is set up right for impersonation role
        testSuccessfulImpersonation("admin", Config.getAdminRealm());
    }

    @Test
    public void testImpersonateByMasterImpersonator() {
        Response response = adminClient.realm("master").users().create(UserBuilder.create().username("master-impersonator").build());
        String userId = ApiUtil.getCreatedId(response);
        response.close();

        UserResource user = adminClient.realm("master").users().get(userId);
        user.resetPassword(CredentialBuilder.create().password("password").build());

        ClientResource testRealmClient = ApiUtil.findClientResourceByClientId(adminClient.realm("master"), "test-realm");

        List<RoleRepresentation> roles = new LinkedList<>();
        roles.add(ApiUtil.findClientRoleByName(testRealmClient, AdminRoles.VIEW_USERS).toRepresentation());
        roles.add(ApiUtil.findClientRoleByName(testRealmClient, ImpersonationConstants.IMPERSONATION_ROLE).toRepresentation());

        user.roles().clientLevel(testRealmClient.toRepresentation().getId()).add(roles);

        testSuccessfulImpersonation("master-impersonator", Config.getAdminRealm());

        adminClient.realm("master").users().get(userId).remove();
    }

    @Test
    public void testImpersonateByTestImpersonator() {
        testSuccessfulImpersonation("impersonator", "test");
    }

    @Test
    public void testImpersonateByTestAdmin() {
        // test that composite is set up right for impersonation role
        testSuccessfulImpersonation("realm-admin", "test");
    }

    @Test
    public void testImpersonateByTestBadImpersonator() {
        testForbiddenImpersonation("bad-impersonator", "test");
    }

    @Test
    public void testImpersonateByMastertBadImpersonator() {
        Response response = adminClient.realm("master").users().create(UserBuilder.create().username("master-bad-impersonator").build());
        String userId = ApiUtil.getCreatedId(response);
        response.close();
        adminClient.realm("master").users().get(userId).resetPassword(CredentialBuilder.create().password("password").build());

        testForbiddenImpersonation("master-bad-impersonator", Config.getAdminRealm());

        adminClient.realm("master").users().get(userId).remove();
    }


    protected void testSuccessfulImpersonation(String admin, String adminRealm) {

        Keycloak client = login(admin, adminRealm);
        try {
            Map data = client.realms().realm("test").users().get(impersonatedUserId).impersonate();
            Assert.assertNotNull(data);
            Assert.assertNotNull(data.get("redirect"));

            events.expect(EventType.IMPERSONATE)
                    .session(AssertEvents.isUUID())
                    .user(impersonatedUserId)
                    .detail(Details.IMPERSONATOR, admin)
                    .detail(Details.IMPERSONATOR_REALM, adminRealm)
                    .client((String) null).assertEvent();
        } finally {
            client.close();
        }
    }

    protected void testForbiddenImpersonation(String admin, String adminRealm) {
        Keycloak client = createAdminClient(adminRealm, establishClientId(adminRealm), admin);
        try {
            client.realms().realm("test").users().get(impersonatedUserId).impersonate();
        } catch (ClientErrorException e) {
            Assert.assertTrue(e.getMessage().indexOf("403 Forbidden") != -1);
        } finally {
            client.close();
        }
    }

    Keycloak createAdminClient(String realm, String clientId, String username) {
        return createAdminClient(realm, clientId, username, null);
    }

    String establishClientId(String realm) {
        return realm.equals("master") ? Constants.ADMIN_CLI_CLIENT_ID : "myclient";
    }

    Keycloak createAdminClient(String realm, String clientId, String username, String password) {
        if (password == null) {
            password = username.equals("admin") ? "admin" : "password";
        }
        return Keycloak.getInstance(AuthServerTestEnricher.getAuthServerContextRoot() + "/auth",
                realm, username, password, clientId);
    }

    private Keycloak login(String username, String realm) {
        String clientId = establishClientId(realm);
        Keycloak client = createAdminClient(realm, clientId, username);

        client.tokenManager().grantToken();
        // only poll for LOGIN event if realm is not master
        // - since for master testing event listener is not installed
        if (!AuthRealm.MASTER.equals(realm)) {
            EventRepresentation e = events.poll();
            Assert.assertEquals("Event type", EventType.LOGIN.toString(), e.getType());
            Assert.assertEquals("Client ID", clientId, e.getClientId());
            Assert.assertEquals("Username", username, e.getDetails().get("username"));
        }
        return client;
    }
}
