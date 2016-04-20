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
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.ImpersonationConstants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.CredentialBuilder;
import org.keycloak.testsuite.util.OAuthClient.AccessTokenResponse;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
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

    private String createAdminToken(String username, String realm) {
        try {
            String password = username.equals("admin") ? "admin" : "password";
            String clientId = realm.equals("master") ? Constants.ADMIN_CLI_CLIENT_ID : "myclient";
            AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest(realm, username, password, null, clientId, null);
            if (tokenResponse.getStatusCode() != 200) {
                throw new RuntimeException("Failed to get token: " + tokenResponse.getErrorDescription());
            }
            events.clear();
            return tokenResponse.getAccessToken();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        Client client = createClient(admin, adminRealm);
        WebTarget impersonate = createImpersonateTarget(client);
        Map data = impersonate.request().post(null, Map.class);
        Assert.assertNotNull(data);
        Assert.assertNotNull(data.get("redirect"));

        // TODO Events not working
        events.expect(EventType.IMPERSONATE)
                .session(AssertEvents.isUUID())
                .user(impersonatedUserId)
                .detail(Details.IMPERSONATOR, admin)
                .detail(Details.IMPERSONATOR_REALM, adminRealm)
                .client((String) null).assertEvent();

        client.close();
    }

    protected void testForbiddenImpersonation(String admin, String adminRealm) {
        Client client = createClient(admin, adminRealm);
        WebTarget impersonate = createImpersonateTarget(client);
        Response response = impersonate.request().post(null);
        response.close();
        client.close();
    }


    protected WebTarget createImpersonateTarget(Client client) {
        UriBuilder authBase = UriBuilder.fromUri(getAuthServerRoot());
        WebTarget adminRealms = client.target(AdminRoot.realmsUrl(authBase));
        WebTarget realmTarget = adminRealms.path("test");
        return realmTarget.path("users").path(impersonatedUserId).path("impersonation");
    }

    protected Client createClient(String admin, String adminRealm) {
        String token = createAdminToken(admin, adminRealm);
        final String authHeader = "Bearer " + token;
        ClientRequestFilter authFilter = new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext requestContext) throws IOException {
                requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, authHeader);
            }
        };
        return javax.ws.rs.client.ClientBuilder.newBuilder().register(authFilter).build();
    }

}
