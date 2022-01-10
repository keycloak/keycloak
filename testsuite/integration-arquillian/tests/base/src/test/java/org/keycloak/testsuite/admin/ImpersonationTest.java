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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.ImpersonationConstants;
import org.keycloak.models.ImpersonationSessionNote;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.*;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.*;
import org.openqa.selenium.Cookie;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.keycloak.testsuite.util.OAuthClient.AUTH_SERVER_ROOT;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

/**
 * Tests Undertow Adapter
 *
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class ImpersonationTest extends AbstractKeycloakTest {

    static class UserSessionNotesHolder {
        private Map<String, String> notes = new HashMap<>();

        public UserSessionNotesHolder() {
        }

        public UserSessionNotesHolder(final Map<String, String> notes) {
            this.notes = notes;
        }

        public void setNotes(final Map<String, String> notes) {
            this.notes = notes;
        }

        public Map<String, String> getNotes() {
            return notes;
        }
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    private String impersonatedUserId;

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

    @BeforeClass
    public static void enabled() {
        Assume.assumeFalse("impersonation".equals(System.getProperty("feature.name"))
                && "disabled".equals(System.getProperty("feature.value")));
    }

    @Before
    public void beforeTest() {
        impersonatedUserId = ApiUtil.findUserByUsername(adminClient.realm("test"), "test-user@localhost").getId();
    }

    @Test
    public void testImpersonateByMasterAdmin() {
        // test that composite is set up right for impersonation role
        testSuccessfulImpersonation("admin", Config.getAdminRealm());
    }

    @Test
    public void testImpersonateByMasterImpersonator() {
        String userId;
        try (Response response = adminClient.realm("master").users().create(UserBuilder.create().username("master-impersonator").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

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
        String userId;
        try (Response response = adminClient.realm("master").users().create(UserBuilder.create().username("master-bad-impersonator").build())) {
            userId = ApiUtil.getCreatedId(response);
        }
        adminClient.realm("master").users().get(userId).resetPassword(CredentialBuilder.create().password("password").build());

        testForbiddenImpersonation("master-bad-impersonator", Config.getAdminRealm());

        adminClient.realm("master").users().get(userId).remove();
    }


    // KEYCLOAK-5981
    @Test
    public void testImpersonationWorksWhenAuthenticationSessionExists() throws Exception {
        // Create test client
        RealmResource realm = adminClient.realms().realm("test");
        Response resp = realm.clients().create(ClientBuilder.create().clientId("test-app").addRedirectUri(OAuthClient.APP_ROOT + "/*").build());
        resp.close();

        // Open the URL for the client (will redirect to Keycloak server AuthorizationEndpoint and create authenticationSession)
        String loginFormUrl = oauth.getLoginFormUrl();
        driver.navigate().to(loginFormUrl);
        loginPage.assertCurrent();

        // Impersonate and get SSO cookie. Setup that cookie for webDriver
        for (Cookie cookie : testSuccessfulImpersonation("realm-admin", "test")) {
            driver.manage().addCookie(cookie);
        }

        // Open the URL again - should be directly redirected to the app due the SSO login
        driver.navigate().to(loginFormUrl);
        appPage.assertCurrent();
        //KEYCLOAK-12783
        Assert.assertEquals("/auth/realms/master/app/auth", new URL(DroneUtils.getCurrentDriver().getCurrentUrl()).getPath());

        // Remove test client
        ApiUtil.findClientByClientId(realm, "test-app").remove();
    }

    // KEYCLOAK-17655
    @Test
    public void testImpersonationBySameRealmServiceAccount() throws Exception {
        // Create test client service account
        RealmResource realm = adminClient.realms().realm("test");
        ClientRepresentation clientApp = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("service-account-cl")
                .secret("password")
                .serviceAccountsEnabled(true)
                .build();
        clientApp.setServiceAccountsEnabled(true);
        realm.clients().create(clientApp);

        UserRepresentation user = ClientManager.realm(adminClient.realm("test")).clientId("service-account-cl").getServiceAccountUser();
        user.setServiceAccountClientId("service-account-cl");

        // add impersonation roles
        ApiUtil.assignClientRoles(realm, user.getId(), Constants.REALM_MANAGEMENT_CLIENT_ID, ImpersonationConstants.IMPERSONATION_ROLE);

        // Impersonation
        testSuccessfulServiceAccountImpersonation(user, "test");

        // Remove test client
        ApiUtil.findClientByClientId(realm, "service-account-cl").remove();
    }
    @Test
    public void testImpersonationByMasterRealmServiceAccount() throws Exception {
        // Create test client service account
        RealmResource realm = adminClient.realms().realm("master");
        ClientRepresentation clientApp = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("service-account-cl")
                .secret("password")
                .serviceAccountsEnabled(true)
                .build();
        clientApp.setServiceAccountsEnabled(true);
        realm.clients().create(clientApp);

        UserRepresentation user = ClientManager.realm(adminClient.realm("master")).clientId("service-account-cl").getServiceAccountUser();
        user.setServiceAccountClientId("service-account-cl");

        // add impersonation roles
        ApiUtil.assignRealmRoles(realm, user.getId(), "admin");

        // Impersonation
        testSuccessfulServiceAccountImpersonation(user, "master");

        // Remove test client
        ApiUtil.findClientByClientId(realm, "service-account-cl").remove();
    }

    // Return the SSO cookie from the impersonated session
    protected Set<Cookie> testSuccessfulImpersonation(String admin, String adminRealm) {
        ResteasyClientBuilder resteasyClientBuilder = new ResteasyClientBuilder();
        resteasyClientBuilder.connectionPoolSize(10);
        resteasyClientBuilder.httpEngine(AdminClientUtil.getCustomClientHttpEngine(resteasyClientBuilder, 10, null));
        ResteasyClient resteasyClient = resteasyClientBuilder.build();

        // Login adminClient
        try (Keycloak client = login(admin, adminRealm, resteasyClient)) {
            // Impersonate
            return impersonate(client, admin, adminRealm);
        }
    }

    private Set<Cookie> impersonate(Keycloak adminClient, String admin, String adminRealm) {
        BasicCookieStore cookieStore = new BasicCookieStore();
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build()) {

            HttpUriRequest req = RequestBuilder.post()
                    .setUri(AUTH_SERVER_ROOT + "/admin/realms/test/users/" + impersonatedUserId + "/impersonation")
                    .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + adminClient.tokenManager().getAccessTokenString())
                    .build();

            HttpResponse res = httpClient.execute(req);
            String resBody = EntityUtils.toString(res.getEntity());

            Assert.assertNotNull(resBody);
            Assert.assertTrue(resBody.contains("redirect"));

            events.expect(EventType.IMPERSONATE)
                    .session(AssertEvents.isUUID())
                    .user(impersonatedUserId)
                    .detail(Details.IMPERSONATOR, admin)
                    .detail(Details.IMPERSONATOR_REALM, adminRealm)
                    .client((String) null).assertEvent();

            // Fetch user session notes
            final String userId = impersonatedUserId;
            final UserSessionNotesHolder notesHolder = testingClient.server("test").fetch(session -> {
                final RealmModel realm = session.realms().getRealmByName("test");
                final UserModel user = session.users().getUserById(realm, userId);
                final UserSessionModel userSession = session.sessions().getUserSessionsStream(realm, user).findFirst().get();
                return new UserSessionNotesHolder(userSession.getNotes());
            }, UserSessionNotesHolder.class);

            // Check impersonation details
            final Map<String, String> notes = notesHolder.getNotes();
            Assert.assertNotNull(notes.get(ImpersonationSessionNote.IMPERSONATOR_ID.toString()));
            Assert.assertEquals(admin, notes.get(ImpersonationSessionNote.IMPERSONATOR_USERNAME.toString()));

            Set<Cookie> cookies = cookieStore.getCookies().stream()
                    .filter(c -> c.getName().startsWith(AuthenticationManager.KEYCLOAK_IDENTITY_COOKIE))
                    .map(c -> new Cookie(c.getName(), c.getValue(), c.getDomain(), c.getPath(), c.getExpiryDate(), c.isSecure(), true))
                    .collect(Collectors.toSet());

            Assert.assertNotNull(cookies);
            Assert.assertThat(cookies, is(not(empty())));

            return cookies;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void testForbiddenImpersonation(String admin, String adminRealm) {
        try (Keycloak client = createAdminClient(adminRealm, establishClientId(adminRealm), admin)) {
            client.realms().realm("test").users().get(impersonatedUserId).impersonate();
            Assert.fail("Expected ClientErrorException wasn't thrown.");
        } catch (ClientErrorException e) {
            Assert.assertThat(e.getMessage(), containsString("403 Forbidden"));
        }
    }

    Keycloak createAdminClient(String realm, String clientId, String username) {
        return createAdminClient(realm, clientId, username, null, null);
    }

    String establishClientId(String realm) {
        return realm.equals("master") ? Constants.ADMIN_CLI_CLIENT_ID : "myclient";
    }

    Keycloak createAdminClient(String realm, String clientId, String username, String password, ResteasyClient resteasyClient) {
        if (password == null) {
            password = username.equals("admin") ? "admin" : "password";
        }

        return KeycloakBuilder.builder().serverUrl(getAuthServerContextRoot() + "/auth")
                .realm(realm)
                .username(username)
                .password(password)
                .clientId(clientId)
                .resteasyClient(resteasyClient)
                .build();
    }

    private Keycloak login(String username, String realm, ResteasyClient resteasyClient) {
        String clientId = establishClientId(realm);
        Keycloak client = createAdminClient(realm, clientId, username, null, resteasyClient);

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


    // Return the SSO cookie from the impersonated session
    protected Set<Cookie> testSuccessfulServiceAccountImpersonation(UserRepresentation serviceAccount, String serviceAccountRealm) {
        ResteasyClientBuilder resteasyClientBuilder = new ResteasyClientBuilder();
        resteasyClientBuilder.connectionPoolSize(10);
        resteasyClientBuilder.httpEngine(AdminClientUtil.getCustomClientHttpEngine(resteasyClientBuilder, 10, null));
        ResteasyClient resteasyClient = resteasyClientBuilder.build();

        // Login adminClient
        try (Keycloak client = loginServiceAccount(serviceAccount, serviceAccountRealm, resteasyClient)) {
            // Impersonate test-user with service account
            return impersonateServiceAccount(client);
        }
    }

    private Keycloak loginServiceAccount(UserRepresentation serviceAccount, String serviceAccountRealm, ResteasyClient resteasyClient) {
        Keycloak client = createServiceAccountClient(serviceAccountRealm, serviceAccount, resteasyClient);
        // get token
        client.tokenManager().getAccessToken();
        return client;
    }

    Keycloak createServiceAccountClient(String serviceAccountRealm, UserRepresentation serviceAccount, ResteasyClient resteasyClient) {
        return KeycloakBuilder.builder().serverUrl(getAuthServerContextRoot() + "/auth")
                .realm(serviceAccountRealm)
                .clientId(serviceAccount.getServiceAccountClientId())
                .clientSecret("password")
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .resteasyClient(resteasyClient)
                .build();
    }

    private Set<Cookie> impersonateServiceAccount(Keycloak adminClient) {
        BasicCookieStore cookieStore = new BasicCookieStore();
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build()) {

            HttpUriRequest req = RequestBuilder.post()
                    .setUri(AUTH_SERVER_ROOT + "/admin/realms/test/users/" + impersonatedUserId + "/impersonation")
                    .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + adminClient.tokenManager().getAccessTokenString())
                    .build();

            HttpResponse res = httpClient.execute(req);
            String resBody = EntityUtils.toString(res.getEntity());

            Assert.assertNotNull(resBody);
            Assert.assertTrue(resBody.contains("redirect"));
            Set<Cookie> cookies = cookieStore.getCookies().stream()
                    .filter(c -> c.getName().startsWith(AuthenticationManager.KEYCLOAK_IDENTITY_COOKIE))
                    .map(c -> new Cookie(c.getName(), c.getValue(), c.getDomain(), c.getPath(), c.getExpiryDate(), c.isSecure(), true))
                    .collect(Collectors.toSet());

            Assert.assertNotNull(cookies);
            Assert.assertThat(cookies, is(not(empty())));

            return cookies;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
