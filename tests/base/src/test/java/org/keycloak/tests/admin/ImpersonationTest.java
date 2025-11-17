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

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.cookie.CookieType;
import org.keycloak.events.EventType;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.ImpersonationSessionNote;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventMatchers;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.TestApp;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectTestApp;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.CredentialBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Tests Undertow Adapter
 *
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
@KeycloakIntegrationTest(config = ImpersonationTest.ImpersonationTestServerConfig.class)
public class ImpersonationTest {

    @InjectRealm(ref = "test", config = ImpersonationTestRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectUser(ref = "test-user", realmRef = "test", config = TestUserConfig.class)
    ManagedUser managedUser;

    @InjectRealm(ref = "master", attachTo = "master")
    ManagedRealm masterRealm;

    @InjectAdminClientFactory
    AdminClientFactory clientFactory;

    @InjectRunOnServer(realmRef = "test")
    RunOnServerClient runOnServer;

    @InjectOAuthClient(realmRef = "test")
    OAuthClient oauth;

    @InjectTestApp
    TestApp testApp;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectWebDriver
    WebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectEvents(ref = "test-events", realmRef = "test")
    Events events;

    @Test
    public void testImpersonateByMasterAdmin() {
        // test that composite is set up right for impersonation role
        testSuccessfulImpersonation("admin", Config.getAdminRealm());
    }

    @Test
    public void testImpersonateByMasterImpersonator() {
        String userId;
        try (Response response = masterRealm.admin().users().create(UserConfigBuilder.create().username("master-impersonator").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        UserResource user = masterRealm.admin().users().get(userId);
        user.resetPassword(CredentialBuilder.create().password("password").build());

        ClientResource testRealmClient = AdminApiUtil.findClientByClientId(masterRealm.admin(), managedRealm.getName() + "-realm");

        List<RoleRepresentation> roles = new LinkedList<>();
        roles.add(AdminApiUtil.findClientRoleByName(testRealmClient, AdminRoles.VIEW_USERS).toRepresentation());
        roles.add(AdminApiUtil.findClientRoleByName(testRealmClient, AdminRoles.IMPERSONATION).toRepresentation());

        user.roles().clientLevel(testRealmClient.toRepresentation().getId()).add(roles);

        testSuccessfulImpersonation("master-impersonator", Config.getAdminRealm());

        masterRealm.admin().users().get(userId).remove();
    }

    @Test
    public void testImpersongetServiceAccountUserateByTestImpersonator() {
        testSuccessfulImpersonation("impersonator", managedRealm.getName());
    }

    @Test
    public void testImpersonateByTestAdmin() {
        // test that composite is set up right for impersonation role
        testSuccessfulImpersonation("realm-admin", managedRealm.getName());
    }

    @Test
    public void testImpersonateByTestBadImpersonator() {
        testForbiddenImpersonation("bad-impersonator", managedRealm.getName());
    }

    @Test
    public void testImpersonationFailsForDisabledUser() {
        UserResource impersonatedUserResource = managedRealm.admin().users().get(managedUser.getId());
        UserRepresentation impersonatedUserRepresentation = impersonatedUserResource.toRepresentation();
        impersonatedUserRepresentation.setEnabled(false);
        impersonatedUserResource.update(impersonatedUserRepresentation);
        try {
            testBadRequestImpersonation("impersonator", managedRealm.getName(), managedUser.getId(), managedRealm.getName(), "User is disabled");
        } finally {
            impersonatedUserRepresentation.setEnabled(true);
            impersonatedUserResource.update(impersonatedUserRepresentation);
        }
    }

    @Test
    public void testImpersonateByMastertBadImpersonator() {
        String userId;
        try (Response response = masterRealm.admin().users().create(UserConfigBuilder.create().username("master-bad-impersonator").build())) {
            userId = ApiUtil.getCreatedId(response);
        }
        masterRealm.admin().users().get(userId).resetPassword(CredentialBuilder.create().password("password").build());

        testForbiddenImpersonation("master-bad-impersonator", Config.getAdminRealm());

        masterRealm.admin().users().get(userId).remove();
    }


    // KEYCLOAK-5981
    @Test
    public void testImpersonationWorksWhenAuthenticationSessionExists() throws Exception {
        // Open the URL for the client (will redirect to Keycloak server AuthorizationEndpoint and create authenticationSession)
        oauth.openLoginForm();
        loginPage.assertCurrent();

        // Impersonate and get SSO cookie. Setup that cookie for webDriver
        for (Cookie cookie : testSuccessfulImpersonation("realm-admin", managedRealm.getName())) {
            driver.manage().addCookie(cookie);
        }

        // Open the URL again - should be directly redirected to the app due the SSO login
        oauth.openLoginForm();

        //KEYCLOAK-12783
        Assertions.assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains(testApp.getRedirectionUri()));
    }

    // KEYCLOAK-17655
    @Test
    public void testImpersonationBySameRealmServiceAccount() throws Exception {
        // Create test client service account
        ClientRepresentation clientApp = ClientConfigBuilder.create()
                .clientId("service-account-cl")
                .secret("password")
                .serviceAccountsEnabled(true)
                .build();
        clientApp.setServiceAccountsEnabled(true);
        managedRealm.admin().clients().create(clientApp);

        UserRepresentation user = AdminApiUtil.findClientByClientId(managedRealm.admin(), "service-account-cl").getServiceAccountUser();
        user.setServiceAccountClientId("service-account-cl");

        // add impersonation roles
        AdminApiUtil.assignClientRoles(managedRealm.admin(), user.getId(), Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.IMPERSONATION);

        // Impersonation
        testSuccessfulServiceAccountImpersonation(user, managedRealm.getName());

        // test impersonation over the service account fails
        testBadRequestImpersonation("impersonator", managedRealm.getName(), user.getId(), managedRealm.getName(), "Service accounts cannot be impersonated");

        // Remove test client
        AdminApiUtil.findClientByClientId(managedRealm.admin(), "service-account-cl").remove();
    }
    @Test
    public void testImpersonationByMasterRealmServiceAccount() throws Exception {
        // Create test client service account
        ClientRepresentation clientApp = ClientConfigBuilder.create()
                .clientId("service-account-cl")
                .secret("password")
                .serviceAccountsEnabled(true)
                .build();
        masterRealm.admin().clients().create(clientApp);

        UserRepresentation user = AdminApiUtil.findClientByClientId(masterRealm.admin(), "service-account-cl").getServiceAccountUser();
        user.setServiceAccountClientId("service-account-cl");

        // add impersonation roles
        AdminApiUtil.assignRealmRoles(masterRealm.admin(), user.getId(), "admin");

        // Impersonation
        testSuccessfulServiceAccountImpersonation(user, masterRealm.getName());

        // Remove test client
        AdminApiUtil.findClientByClientId(masterRealm.admin(), "service-account-cl").remove();
    }

    // Return the SSO cookie from the impersonated session
    private Set<Cookie> testSuccessfulImpersonation(String admin, String adminRealm) {
        // Login adminClient
        try (Keycloak client = login(admin, adminRealm)) {
            // Impersonate
            return impersonate(client, admin, adminRealm);
        }
    }

    private Set<Cookie> impersonate(Keycloak adminClient, String admin, String adminRealm) {
        BasicCookieStore cookieStore = new BasicCookieStore();
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build()) {

            HttpUriRequest req = RequestBuilder.post()
                    .setUri(keycloakUrls.getBase() + "/admin/realms/test/users/" + managedUser.getId() + "/impersonation")
                    .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + adminClient.tokenManager().getAccessTokenString())
                    .build();

            HttpResponse res = httpClient.execute(req);
            String resBody = EntityUtils.toString(res.getEntity());

            Assertions.assertNotNull(resBody);
            Assertions.assertTrue(resBody.contains("redirect"));

            EventRepresentation event = events.poll();
            Assertions.assertEquals(event.getType(), EventType.IMPERSONATE.toString());
            MatcherAssert.assertThat(event.getSessionId(), EventMatchers.isSessionId());
            Assertions.assertEquals(event.getUserId(), managedUser.getId());
            Assertions.assertTrue(event.getDetails().values().stream().anyMatch(f -> f.equals(admin)));
            Assertions.assertTrue(event.getDetails().values().stream().anyMatch(f -> f.equals(adminRealm)));

            String testRealm = managedRealm.getName();
            // Fetch user session notes
            final String userId = managedUser.getId();
            final UserSessionNotesHolder notesHolder = runOnServer.fetch(session -> {
                final RealmModel realm = session.realms().getRealmByName(testRealm);
                final UserModel user = session.users().getUserById(realm, userId);
                final UserSessionModel userSession = session.sessions().getUserSessionsStream(realm, user).filter(u -> u.getNotes().containsValue(admin)).findFirst().get();
                return new UserSessionNotesHolder(userSession.getNotes());
            }, UserSessionNotesHolder.class);

            // Check impersonation details
            final Map<String, String> notes = notesHolder.getNotes();
            Assertions.assertNotNull(notes.get(ImpersonationSessionNote.IMPERSONATOR_ID.toString()));
            Assertions.assertEquals(admin, notes.get(ImpersonationSessionNote.IMPERSONATOR_USERNAME.toString()));

            Set<Cookie> cookies = cookieStore.getCookies().stream()
                    .filter(c -> c.getName().startsWith(CookieType.IDENTITY.getName()))
                    .map(c -> new Cookie(c.getName(), c.getValue(), c.getDomain(), c.getPath(), c.getExpiryDate(), c.isSecure(), true))
                    .collect(Collectors.toSet());

            Assertions.assertNotNull(cookies);
            MatcherAssert.assertThat(cookies, is(not(empty())));
            httpClient.close();

            return cookies;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void testForbiddenImpersonation(String admin, String adminRealm) {
        try (Keycloak client = createAdminClient(adminRealm, establishClientId(adminRealm), admin)) {
            client.realms().realm(managedRealm.getName()).users().get(managedUser.getId()).impersonate();
            Assertions.fail("Expected ClientErrorException wasn't thrown.");
        } catch (ClientErrorException e) {
            MatcherAssert.assertThat(e.getMessage(), containsString("403 Forbidden"));
        }
    }

    private void testBadRequestImpersonation(String admin, String adminRealm, String impersonatedId,
            String impersonatedRealm, String errorExpected) {
        try (Keycloak client = createAdminClient(adminRealm, establishClientId(adminRealm), admin)) {
            client.realms().realm(impersonatedRealm).users().get(impersonatedId).impersonate();
            Assertions.fail("Expected ClientErrorException wasn't thrown.");
        } catch (ClientErrorException e) {
            Assertions.assertEquals(Response.Status.BAD_REQUEST, e.getResponse().getStatusInfo());
            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assertions.assertEquals(errorExpected, error.getErrorMessage());
        }
    }


    private String establishClientId(String realm) {
        return realm.equals("master") ? Constants.ADMIN_CLI_CLIENT_ID : "myclient";
    }

    private Keycloak createAdminClient(String realm, String clientId, String username) {
        String password = username.equals("admin") ? "admin" : "password";

        return clientFactory.create()
                .realm(realm)
                .username(username)
                .password(password)
                .clientId(clientId)
                .grantType(OAuth2Constants.PASSWORD).build();
    }

    private Keycloak login(String username, String realm) {
        String clientId = establishClientId(realm);
        Keycloak client = createAdminClient(realm, clientId, username);

        client.tokenManager().grantToken();
        // only poll for LOGIN event if realm is not master
        // - since for master testing event listener is not installed
        if (!realm.equals("master")) {
            EventRepresentation e = events.poll();
            Assertions.assertEquals(EventType.LOGIN.toString(), e.getType(), "Event type");
            Assertions.assertEquals(clientId, e.getClientId(), "Client ID");
        }
        return client;
    }


    // Return the SSO cookie from the impersonated session
    private Set<Cookie> testSuccessfulServiceAccountImpersonation(UserRepresentation serviceAccount, String serviceAccountRealm) {
        // Login adminClient
        try (Keycloak client = loginServiceAccount(serviceAccount, serviceAccountRealm)) {
            // Impersonate test-user with service account
            return impersonateServiceAccount(client);
        }
    }

    private Keycloak loginServiceAccount(UserRepresentation serviceAccount, String serviceAccountRealm) {
        Keycloak client = createServiceAccountClient(serviceAccountRealm, serviceAccount);
        // get token
        client.tokenManager().getAccessToken();
        return client;
    }

    private Keycloak createServiceAccountClient(String serviceAccountRealm, UserRepresentation serviceAccount) {
        return clientFactory.create().realm(serviceAccountRealm).clientId(serviceAccount.getServiceAccountClientId()).clientSecret("password").grantType(OAuth2Constants.CLIENT_CREDENTIALS).build();
    }

    private Set<Cookie> impersonateServiceAccount(Keycloak adminClient) {
        BasicCookieStore cookieStore = new BasicCookieStore();
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build()) {

            HttpUriRequest req = RequestBuilder.post()
                    .setUri(keycloakUrls.getBase() + "/admin/realms/test/users/" + managedUser.getId() + "/impersonation")
                    .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + adminClient.tokenManager().getAccessTokenString())
                    .build();

            HttpResponse res = httpClient.execute(req);
            String resBody = EntityUtils.toString(res.getEntity());

            Assertions.assertNotNull(resBody);
            Assertions.assertTrue(resBody.contains("redirect"));
            Set<Cookie> cookies = cookieStore.getCookies().stream()
                    .filter(c -> c.getName().startsWith(CookieType.IDENTITY.getName()))
                    .map(c -> new Cookie(c.getName(), c.getValue(), c.getDomain(), c.getPath(), c.getExpiryDate(), c.isSecure(), true))
                    .collect(Collectors.toSet());

            Assertions.assertNotNull(cookies);
            MatcherAssert.assertThat(cookies, is(not(empty())));
            httpClient.close();

            return cookies;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class UserSessionNotesHolder {
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

    public static class ImpersonationTestServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder server) {
            server.features(Profile.Feature.IMPERSONATION);
            return server;
        }
    }

    private static class ImpersonationTestRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder config) {
            config.addClient("myclient").clientId("myclient")
                    .publicClient(true).directAccessGrantsEnabled(true);

            config.addUser("realm-admin")
                    .password("password").name("My", "Test Admin")
                    .email("my-test-admin@email.org").emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);
            config.addUser("impersonator")
                    .password("password").name("My", "Test Impersonator")
                    .email("my-test-impersonator@email.org").emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.IMPERSONATION)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_USERS);
            config.addUser("bad-impersonator")
                    .password("password").name("My", "Test Bad Impersonator")
                    .email("my-test-bad-impersonator@email.org").emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_USERS);

            return config;
        }
    }

    private static class TestUserConfig implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            user.username("test-user");
            user.password("password");
            user.name("My", "Test");
            user.email("test@email.org");
            user.emailVerified(true);

            return user;
        }
    }
}
