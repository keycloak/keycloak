package org.keycloak.testsuite.springboot;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Base64Url;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.*;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.testsuite.ActionURIUtils;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.broker.BrokerTestTools;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.UriBuilder;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT;
import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT_LINKS;
import static org.keycloak.testsuite.admin.ApiUtil.createUserAndResetPasswordWithAdminClient;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.pause;

@DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
public class AccountLinkSpringBootTest extends AbstractSpringBootTest {

    private static final String PARENT_REALM = "parent-realm";

    private static final String LINKING_URL = BASE_URL + "/LinkServlet";

    private static final String PARENT_USERNAME = "parent-username";
    private static final String PARENT_PASSWORD = "parent-password";

    private static final String CHILD_USERNAME_1 = "child-username-1";
    private static final String CHILD_PASSWORD_1 = "child-password-1";

    private static final String CHILD_USERNAME_2 = "child-username-2";
    private static final String CHILD_PASSWORD_2 = "child-password-2";

    @Page
    private LinkingPage linkingPage;

    @Page
    private AccountUpdateProfilePage profilePage;

    @Page
    private LoginUpdateProfilePage loginUpdateProfilePage;

    @Page
    private ErrorPage errorPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(REALM_NAME);
        realm.setEnabled(true);
        realm.setPublicKey(REALM_PUBLIC_KEY);
        realm.setPrivateKey(REALM_PRIVATE_KEY);
        realm.setAccessTokenLifespan(600);
        realm.setAccessCodeLifespan(10);
        realm.setAccessCodeLifespanUserAction(6000);
        realm.setSslRequired("external");
        ClientRepresentation servlet = new ClientRepresentation();
        servlet.setClientId(CLIENT_ID);
        servlet.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        servlet.setAdminUrl(LINKING_URL);
        servlet.setDirectAccessGrantsEnabled(true);
        servlet.setBaseUrl(LINKING_URL);
        servlet.setRedirectUris(new LinkedList<>());
        servlet.getRedirectUris().add(LINKING_URL + "/*");
        servlet.setSecret(SECRET);
        servlet.setFullScopeAllowed(true);
        realm.setClients(new LinkedList<>());
        realm.getClients().add(servlet);
        testRealms.add(realm);

        realm = new RealmRepresentation();
        realm.setRealm(PARENT_REALM);
        realm.setEnabled(true);

        testRealms.add(realm);
    }

    @Override
    public void addUsers() {
        addIdpUser();
        addChildUser();
    }

    @Override
    public void cleanupUsers() {
    }

    @Override
    public void createRoles() {
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    public void addIdpUser() {
        RealmResource realm = adminClient.realms().realm(PARENT_REALM);
        UserRepresentation user = new UserRepresentation();
        user.setUsername(PARENT_USERNAME);
        user.setEnabled(true);
        createUserAndResetPasswordWithAdminClient(realm, user, PARENT_PASSWORD);
    }

    private String childUserId = null;

    public void addChildUser() {
        RealmResource realm = adminClient.realms().realm(REALM_NAME);
        UserRepresentation user = new UserRepresentation();
        user.setUsername(CHILD_USERNAME_1);
        user.setEnabled(true);
        childUserId = createUserAndResetPasswordWithAdminClient(realm, user, CHILD_PASSWORD_1);
        UserRepresentation user2 = new UserRepresentation();
        user2.setUsername(CHILD_USERNAME_2);
        user2.setEnabled(true);
        String user2Id = createUserAndResetPasswordWithAdminClient(realm, user2, CHILD_PASSWORD_2);

        // have to add a role as undertow default auth manager doesn't like "*". todo we can remove this eventually as undertow fixes this in later versions
        realm.roles().create(new RoleRepresentation(CORRECT_ROLE, null, false));
        RoleRepresentation role = realm.roles().get(CORRECT_ROLE).toRepresentation();
        List<RoleRepresentation> roles = new LinkedList<>();
        roles.add(role);
        realm.users().get(childUserId).roles().realmLevel().add(roles);
        realm.users().get(user2Id).roles().realmLevel().add(roles);
        ClientRepresentation brokerService = realm.clients().findByClientId(Constants.BROKER_SERVICE_CLIENT_ID).get(0);
        role = realm.clients().get(brokerService.getId()).roles().get(Constants.READ_TOKEN_ROLE).toRepresentation();
        roles.clear();
        roles.add(role);
        realm.users().get(childUserId).roles().clientLevel(brokerService.getId()).add(roles);
        realm.users().get(user2Id).roles().clientLevel(brokerService.getId()).add(roles);
    }

    @Before
    public void createParentChild() {
        BrokerTestTools.createKcOidcBroker(adminClient, REALM_NAME, PARENT_REALM);

        testRealmLoginPage.setAuthRealm(REALM_NAME);
    }


    @Test
    public void testErrorConditions() throws Exception {
        RealmResource realm = adminClient.realms().realm(REALM_NAME);
        List<FederatedIdentityRepresentation> links = realm.users().get(childUserId).getFederatedIdentity();
        assertThat(links, is(empty()));

        ClientRepresentation client = adminClient.realms().realm(REALM_NAME).clients().findByClientId(CLIENT_ID).get(0);

        UriBuilder redirectUri = UriBuilder.fromUri(LINKING_URL).queryParam("response", "true");

        UriBuilder directLinking = UriBuilder.fromUri(getAuthServerContextRoot() + "/auth")
                .path("realms/{child-realm}/broker/{provider}/link")
                .queryParam("client_id", CLIENT_ID)
                .queryParam("redirect_uri", redirectUri.build())
                .queryParam("hash", Base64Url.encode("crap".getBytes()))
                .queryParam("nonce", UUID.randomUUID().toString());

        String linkUrl = directLinking
                .build(REALM_NAME, PARENT_REALM).toString();

        // test that child user cannot log into parent realm
        navigateTo(linkUrl);
        assertCurrentUrlStartsWith(testRealmLoginPage);
        testRealmLoginPage.form().login(CHILD_USERNAME_1, CHILD_PASSWORD_1);
        assertThat(driver.getCurrentUrl(), containsString("link_error=not_logged_in"));

        logoutAll();

        // now log in
        navigateTo(LINKING_URL + "?response=true");
        assertCurrentUrlStartsWith(testRealmLoginPage);
        testRealmLoginPage.form().login(CHILD_USERNAME_1, CHILD_PASSWORD_1);

        linkingPage.assertIsCurrent();

        assertThat(linkingPage.getErrorMessage().toLowerCase(), containsString("account linked"));

        // now test CSRF with bad hash.
        navigateTo(linkUrl);

        assertThat(driver.getPageSource(), containsString("We are sorry..."));

        logoutAll();

        // now log in again with client that does not have scope

        String accountId = adminClient.realms().realm(REALM_NAME).clients().findByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation manageAccount = adminClient.realms().realm(REALM_NAME).clients().get(accountId).roles().get(MANAGE_ACCOUNT).toRepresentation();
        RoleRepresentation manageLinks = adminClient.realms().realm(REALM_NAME).clients().get(accountId).roles().get(MANAGE_ACCOUNT_LINKS).toRepresentation();
        RoleRepresentation userRole = adminClient.realms().realm(REALM_NAME).roles().get(CORRECT_ROLE).toRepresentation();

        client.setFullScopeAllowed(false);
        ClientResource clientResource = adminClient.realms().realm(REALM_NAME).clients().get(client.getId());
        clientResource.update(client);

        List<RoleRepresentation> roles = new LinkedList<>();
        roles.add(userRole);
        clientResource.getScopeMappings().realmLevel().add(roles);

        navigateTo(LINKING_URL + "?response=true");
        assertCurrentUrlStartsWith(testRealmLoginPage);
        testRealmLoginPage.form().login(CHILD_USERNAME_1, CHILD_PASSWORD_1);

        linkingPage.assertIsCurrent();
        assertThat(linkingPage.getErrorMessage().toLowerCase(), containsString("account linked"));

        UriBuilder linkBuilder = UriBuilder.fromUri(LINKING_URL);
        String clientLinkUrl = linkBuilder.clone()
                .queryParam("realm", REALM_NAME)
                .queryParam("provider", PARENT_REALM).build().toString();

        navigateTo(clientLinkUrl);
        assertThat(driver.getCurrentUrl(), containsString("error=not_allowed"));

        logoutAll();

        // add MANAGE_ACCOUNT_LINKS scope should pass.

        links = realm.users().get(childUserId).getFederatedIdentity();
        assertThat(links, is(empty()));

        roles = new LinkedList<>();
        roles.add(manageLinks);
        clientResource.getScopeMappings().clientLevel(accountId).add(roles);

        navigateTo(clientLinkUrl);
        assertCurrentUrlStartsWith(testRealmLoginPage);
        testRealmLoginPage.form().login(CHILD_USERNAME_1, CHILD_PASSWORD_1);

        testRealmLoginPage.setAuthRealm(PARENT_REALM);
        assertCurrentUrlStartsWith(testRealmLoginPage);
        testRealmLoginPage.form().login(PARENT_USERNAME, PARENT_PASSWORD);

        testRealmLoginPage.setAuthRealm(REALM_NAME); // clean

        assertThat(driver.getCurrentUrl(), startsWith(linkBuilder.toTemplate()));
        assertThat(driver.getPageSource(), containsString("Account linked"));

        links = realm.users().get(childUserId).getFederatedIdentity();
        assertThat(links, is(not(empty())));

        realm.users().get(childUserId).removeFederatedIdentity(PARENT_REALM);
        links = realm.users().get(childUserId).getFederatedIdentity();
        assertThat(links, is(empty()));

        clientResource.getScopeMappings().clientLevel(accountId).remove(roles);

        logoutAll();

        navigateTo(clientLinkUrl);
        assertCurrentUrlStartsWith(testRealmLoginPage);
        testRealmLoginPage.form().login(CHILD_USERNAME_1, CHILD_PASSWORD_1);

        assertThat(driver.getCurrentUrl(), containsString("link_error=not_allowed"));

        logoutAll();

        // add MANAGE_ACCOUNT scope should pass

        links = realm.users().get(childUserId).getFederatedIdentity();
        assertThat(links, is(empty()));

        roles = new LinkedList<>();
        roles.add(manageAccount);
        clientResource.getScopeMappings().clientLevel(accountId).add(roles);

        navigateTo(clientLinkUrl);
        assertCurrentUrlStartsWith(testRealmLoginPage);
        testRealmLoginPage.form().login(CHILD_USERNAME_1, CHILD_PASSWORD_1);

        testRealmLoginPage.setAuthRealm(PARENT_REALM);
        assertCurrentUrlStartsWith(testRealmLoginPage);
        testRealmLoginPage.form().login(PARENT_USERNAME, PARENT_PASSWORD);

        testRealmLoginPage.setAuthRealm(REALM_NAME); // clean


        assertThat(driver.getCurrentUrl(), startsWith(linkBuilder.toTemplate()));
        assertThat(driver.getPageSource(), containsString("Account linked"));

        links = realm.users().get(childUserId).getFederatedIdentity();
        assertThat(links, is(not(empty())));

        realm.users().get(childUserId).removeFederatedIdentity(PARENT_REALM);
        links = realm.users().get(childUserId).getFederatedIdentity();
        assertThat(links, is(empty()));

        clientResource.getScopeMappings().clientLevel(accountId).remove(roles);

        logoutAll();

        navigateTo(clientLinkUrl);
        assertCurrentUrlStartsWith(testRealmLoginPage);
        testRealmLoginPage.form().login(CHILD_USERNAME_1, CHILD_PASSWORD_1);

        assertThat(driver.getCurrentUrl(), containsString("link_error=not_allowed"));

        logoutAll();

        // undo fullScopeAllowed

        client = adminClient.realms().realm(REALM_NAME).clients().findByClientId(CLIENT_ID).get(0);
        client.setFullScopeAllowed(true);
        clientResource.update(client);

        links = realm.users().get(childUserId).getFederatedIdentity();
        assertThat(links, is(empty()));

        logoutAll();
    }

    @Test
    public void testAccountLink() throws Exception {
        RealmResource realm = adminClient.realms().realm(REALM_NAME);
        List<FederatedIdentityRepresentation> links = realm.users().get(childUserId).getFederatedIdentity();
        assertThat(links, is(empty()));

        UriBuilder linkBuilder = UriBuilder.fromUri(LINKING_URL);
        String linkUrl = linkBuilder.clone()
                .queryParam("realm", REALM_NAME)
                .queryParam("provider", PARENT_REALM).build().toString();
        log.info("linkUrl: " + linkUrl);
        navigateTo(linkUrl);
        assertCurrentUrlStartsWith(testRealmLoginPage);

        assertThat(driver.getPageSource(), containsString(PARENT_REALM));
        testRealmLoginPage.form().login(CHILD_USERNAME_1, CHILD_PASSWORD_1);

        testRealmLoginPage.setAuthRealm(PARENT_REALM);
        assertCurrentUrlStartsWith(testRealmLoginPage);
        testRealmLoginPage.form().login(PARENT_USERNAME, PARENT_PASSWORD);
        testRealmLoginPage.setAuthRealm(REALM_NAME); // clean

        log.info("After linking: " + driver.getCurrentUrl());
        log.info(driver.getPageSource());

        assertThat(driver.getCurrentUrl(), startsWith(linkBuilder.toTemplate()));
        assertThat(driver.getPageSource(), containsString("Account linked"));

        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest(
                REALM_NAME,
                CHILD_USERNAME_1,
                CHILD_PASSWORD_1,
                null,
                CLIENT_ID,
                SECRET);

        assertThat(response.getAccessToken(), is(notNullValue()));
        assertThat(response.getError(), is(nullValue()));


        Client httpClient = ClientBuilder.newClient();
        String firstToken = getToken(response, httpClient);
        assertThat(firstToken, is(notNullValue()));

        navigateTo(linkUrl);
        assertThat(driver.getPageSource(), containsString("Account linked"));

        String nextToken = getToken(response, httpClient);
        assertThat(nextToken, is(notNullValue()));
        assertThat(firstToken, is(not(equalTo(nextToken))));

        links = realm.users().get(childUserId).getFederatedIdentity();
        assertThat(links, is(not(empty())));

        realm.users().get(childUserId).removeFederatedIdentity(PARENT_REALM);
        links = realm.users().get(childUserId).getFederatedIdentity();
        assertThat(links, is(empty()));

        logoutAll();
    }

    @Test
    public void testLinkOnlyProvider() throws Exception {
        RealmResource realm = adminClient.realms().realm(REALM_NAME);
        IdentityProviderRepresentation rep = realm.identityProviders().get(PARENT_REALM).toRepresentation();
        rep.setLinkOnly(true);
        realm.identityProviders().get(PARENT_REALM).update(rep);

        try {
            List<FederatedIdentityRepresentation> links = realm.users().get(childUserId).getFederatedIdentity();
            assertThat(links, is(empty()));

            UriBuilder linkBuilder = UriBuilder.fromUri(LINKING_URL);
            String linkUrl = linkBuilder.clone()
                    .queryParam("realm", REALM_NAME)
                    .queryParam("provider", PARENT_REALM).build().toString();
            navigateTo(linkUrl);
            assertCurrentUrlStartsWith(testRealmLoginPage);

            // should not be on login page.  This is what we are testing
            assertThat(driver.getPageSource(), not(containsString(PARENT_REALM)));

            // now test that we can still link.
            testRealmLoginPage.form().login(CHILD_USERNAME_1, CHILD_PASSWORD_1);

            testRealmLoginPage.setAuthRealm(PARENT_REALM);
            assertCurrentUrlStartsWith(testRealmLoginPage);

            testRealmLoginPage.form().login(PARENT_USERNAME, PARENT_PASSWORD);
            testRealmLoginPage.setAuthRealm(REALM_NAME);

            log.info("After linking: " + driver.getCurrentUrl());
            log.info(driver.getPageSource());

            assertThat(driver.getCurrentUrl(), startsWith(linkBuilder.toTemplate()));
            assertThat(driver.getPageSource(), containsString("Account linked"));

            links = realm.users().get(childUserId).getFederatedIdentity();
            assertThat(links, is(not(empty())));

            realm.users().get(childUserId).removeFederatedIdentity(PARENT_REALM);
            links = realm.users().get(childUserId).getFederatedIdentity();
            assertThat(links, is(empty()));

            logoutAll();

            log.info("testing link-only attack");

            navigateTo(linkUrl);
            assertCurrentUrlStartsWith(testRealmLoginPage);

            log.info("login page uri is: " + driver.getCurrentUrl());

            // ok, now scrape the code from page
            String pageSource = driver.getPageSource();
            String action = ActionURIUtils.getActionURIFromPageSource(pageSource);
            System.out.println("action uri: " + action);

            Map<String, String> queryParams = ActionURIUtils.parseQueryParamsFromActionURI(action);
            System.out.println("query params: " + queryParams);

            // now try and use the code to login to remote link-only idp

            String uri = "/auth/realms/" + REALM_NAME + "/broker/" + PARENT_REALM + "/login";

            uri = UriBuilder.fromUri(getAuthServerContextRoot())
                    .path(uri)
                    .queryParam(LoginActionsService.SESSION_CODE, queryParams.get(LoginActionsService.SESSION_CODE))
                    .queryParam(Constants.CLIENT_ID, queryParams.get(Constants.CLIENT_ID))
                    .queryParam(Constants.TAB_ID, queryParams.get(Constants.TAB_ID))
                    .build().toString();

            log.info("hack uri: " + uri);

            navigateTo(uri);

            assertThat(driver.getPageSource(), containsString("Could not send authentication request to identity provider."));
        } finally {
            rep.setLinkOnly(false);
            realm.identityProviders().get(PARENT_REALM).update(rep);
        }
    }

    @Test
    public void testAccountNotLinkedAutomatically() throws Exception {
        RealmResource realm = adminClient.realms().realm(REALM_NAME);
        List<FederatedIdentityRepresentation> links = realm.users().get(childUserId).getFederatedIdentity();
        assertThat(links, is(empty()));

        // Login to account mgmt first
        profilePage.open(REALM_NAME);
        WaitUtils.waitForPageToLoad();

        assertCurrentUrlStartsWith(testRealmLoginPage);
        testRealmLoginPage.form().login(CHILD_USERNAME_1, CHILD_PASSWORD_1);
        profilePage.assertCurrent();

        // Now in another tab, open login screen with "prompt=login" . Login screen will be displayed even if I have SSO cookie
        UriBuilder linkBuilder = UriBuilder.fromUri(LINKING_URL);
        String linkUrl = linkBuilder.clone()
                .queryParam(OIDCLoginProtocol.PROMPT_PARAM, OIDCLoginProtocol.PROMPT_VALUE_LOGIN)
                .build().toString();

        navigateTo(linkUrl);
        assertCurrentUrlStartsWith(testRealmLoginPage);

        loginPage.clickSocial(PARENT_REALM);

        testRealmLoginPage.setAuthRealm(PARENT_REALM);
        assertCurrentUrlStartsWith(testRealmLoginPage);
        testRealmLoginPage.form().login(PARENT_USERNAME, PARENT_PASSWORD);
        testRealmLoginPage.setAuthRealm(REALM_NAME);

        // Test I was not automatically linked.
        links = realm.users().get(childUserId).getFederatedIdentity();
        assertThat(links, is(empty()));

        loginUpdateProfilePage.assertCurrent();
        loginUpdateProfilePage.update("Joe", "Doe", "joe@parent.com");

        errorPage.assertCurrent();

        assertThat(errorPage.getError(), is(equalTo("You are already authenticated as different user '"
                + CHILD_USERNAME_1
                + "' in this session. Please sign out first.")));

        logoutAll();

        // Remove newly created user
        String newUserId = ApiUtil.findUserByUsername(realm, PARENT_USERNAME).getId();
        getCleanup(REALM_NAME).addUserId(newUserId);
    }

    @Test
    public void testAccountLinkingExpired() throws Exception {
        RealmResource realm = adminClient.realms().realm(REALM_NAME);
        List<FederatedIdentityRepresentation> links = realm.users().get(childUserId).getFederatedIdentity();
        assertThat(links, is(empty()));

        // Login to account mgmt first
        profilePage.open(REALM_NAME);
        WaitUtils.waitForPageToLoad();

        assertCurrentUrlStartsWith(testRealmLoginPage);
        testRealmLoginPage.form().login(CHILD_USERNAME_1, CHILD_PASSWORD_1);
        profilePage.assertCurrent();

        // Now in another tab, request account linking
        UriBuilder linkBuilder = UriBuilder.fromUri(LINKING_URL);
        String linkUrl = linkBuilder.clone()
                .queryParam("realm", REALM_NAME)
                .queryParam("provider", PARENT_REALM).build().toString();
        navigateTo(linkUrl);

        testRealmLoginPage.setAuthRealm(PARENT_REALM);
        assertCurrentUrlStartsWith(testRealmLoginPage);

        setTimeOffset(1); // We need to "wait" for 1 second so that notBeforePolicy invalidates token created when logging to child realm

        // Logout "child" userSession in the meantime (for example through admin request)
        realm.logoutAll();

        // Finish login on parent.
        testRealmLoginPage.form().login(PARENT_USERNAME, PARENT_PASSWORD);


        // Test I was not automatically linked
        links = realm.users().get(childUserId).getFederatedIdentity();
        assertThat(links, is(empty()));

        errorPage.assertCurrent();
        assertThat(errorPage.getError(), is(equalTo("Requested broker account linking, but current session is no longer valid.")));

        logoutAll();

        navigateTo(linkUrl); // Check we are logged out

        testRealmLoginPage.setAuthRealm(REALM_NAME);
        assertCurrentUrlStartsWith(testRealmLoginPage);

        resetTimeOffset();
    }

    private void navigateTo(String uri) {
        driver.navigate().to(uri);
        WaitUtils.waitForPageToLoad();
    }

    public void logoutAll() {
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder()).build(REALM_NAME).toString();
        navigateTo(logoutUri);
        logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder()).build(PARENT_REALM).toString();
        navigateTo(logoutUri);
    }

    private String getToken(OAuthClient.AccessTokenResponse response, Client httpClient) throws Exception {
        log.info("target here is " + OAuthClient.AUTH_SERVER_ROOT);
        String idpToken =  httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("realms")
                .path(REALM_NAME)
                .path("broker")
                .path(PARENT_REALM)
                .path("token")
                .request()
                .header("Authorization", "Bearer " + response.getAccessToken())
                .get(String.class);
        AccessTokenResponse res = JsonSerialization.readValue(idpToken, AccessTokenResponse.class);
        return res.getToken();
    }
}
