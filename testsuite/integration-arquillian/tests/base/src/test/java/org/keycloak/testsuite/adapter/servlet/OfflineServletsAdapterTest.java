package org.keycloak.testsuite.adapter.servlet;

import javax.ws.rs.core.UriBuilder;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.filter.AdapterActionsFilter;
import org.keycloak.testsuite.adapter.page.OfflineToken;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.InfinispanTestTimeServiceRule;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import org.keycloak.testsuite.pages.AccountApplicationsPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.keycloak.util.TokenUtil;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;

import java.io.Closeable;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlDoesntStartWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY_DEPRECATED)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
@DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
public class OfflineServletsAdapterTest extends AbstractServletsAdapterTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);
    @Page
    protected OfflineToken offlineTokenPage;
    @Page
    protected LoginPage loginPage;
    @Page
    protected AccountApplicationsPage accountAppPage;
    @Page
    protected OAuthGrantPage oauthGrantPage;

    @Rule
    public InfinispanTestTimeServiceRule ispnTestTimeService = new InfinispanTestTimeServiceRule(this);

    private final String DEFAULT_USERNAME = "test-user@localhost";
    private final String DEFAULT_PASSWORD = "password";
    private final String OFFLINE_CLIENT_ID = "offline-client";

    @Deployment(name = OfflineToken.DEPLOYMENT_NAME)
    protected static WebArchive offlineClient() {
        return servletDeployment(OfflineToken.DEPLOYMENT_NAME, AdapterActionsFilter.class, AbstractShowTokensServlet.class, OfflineTokenServlet.class, ErrorServlet.class, ServletTestUtils.class);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(TEST);
        testRealmLoginPage.setAuthRealm(TEST);
    }


    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(IOUtil.loadRealm("/adapter-test/offline-client/offlinerealm.json"));
    }

    @Test
    public void testServlet() {
        try {
            String servletUri = UriBuilder.fromUri(offlineTokenPage.toString())
                    .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.OFFLINE_ACCESS)
                    .build().toString();
            oauth.redirectUri(offlineTokenPage.toString());
            oauth.clientId("offline-client");

            driver.navigate().to(servletUri);
            waitUntilElement(By.tagName("body")).is().visible();

            loginPage.assertCurrent();
            loginPage.login(DEFAULT_USERNAME, DEFAULT_PASSWORD);

            assertCurrentUrlStartsWith(offlineTokenPage);

            assertThat(offlineTokenPage.getRefreshToken(), notNullValue());
            assertThat(TokenUtil.TOKEN_TYPE_OFFLINE, is(offlineTokenPage.getRefreshToken().getType()));
            assertThat(offlineTokenPage.getRefreshToken().getExp(), nullValue());

            String accessTokenId = offlineTokenPage.getAccessToken().getId();
            String refreshTokenId = offlineTokenPage.getRefreshToken().getId();

            // online user session will be expired and removed
            setAdapterAndServerTimeOffset(9999);

            // still able to access the page using the offline token
            offlineTokenPage.navigateTo();
            assertCurrentUrlStartsWith(offlineTokenPage);

            // assert successful refresh
            assertThat(offlineTokenPage.getRefreshToken().getId(), not(refreshTokenId));
            assertThat(offlineTokenPage.getAccessToken().getId(), not(accessTokenId));

            // logout doesn't make sense because online user session is gone and there is no KEYCLOAK_IDENTITY / KEYCLOAK_SESSION cookie in the browser
            // navigate to login page which won't be possible if there's valid online session
            driver.navigate().to(oauth.getLoginFormUrl());
            WaitUtils.waitForPageToLoad();
            loginPage.assertCurrent();

            // navigate back to offlineTokenPage to verify the offline session is still valid
            offlineTokenPage.navigateTo();
            assertCurrentUrlStartsWith(offlineTokenPage);

            // logout the offline user session using the offline refresh token
            oauth.doLogout(offlineTokenPage.getRefreshTokenString(), "secret1");

            // can't access the offlineTokenPage anymore
            offlineTokenPage.navigateTo();
            assertCurrentUrlDoesntStartWith(offlineTokenPage);
            loginPage.assertCurrent();
        } finally {
            events.clear();
            resetTimeOffsetAuthenticated();
        }
    }

    @Test
    public void testServletWithRevoke() {
        try { // Login to servlet first with offline token
            String servletUri = UriBuilder.fromUri(offlineTokenPage.toString())
                    .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.OFFLINE_ACCESS)
                    .build().toString();

            driver.navigate().to(servletUri);
            waitUntilElement(By.tagName("body")).is().visible();

            loginPage.assertCurrent();
            loginPage.login(DEFAULT_USERNAME, DEFAULT_PASSWORD);
            assertCurrentUrlStartsWith(offlineTokenPage);

            assertThat(offlineTokenPage.getRefreshToken(), notNullValue());
            assertThat(offlineTokenPage.getRefreshToken().getType(), is(TokenUtil.TOKEN_TYPE_OFFLINE));

            // Assert refresh works with increased time
            setAdapterAndServerTimeOffset(9999);
            offlineTokenPage.navigateTo();
            assertCurrentUrlStartsWith(offlineTokenPage);
            setAdapterAndServerTimeOffset(0);

            events.clear();

            // Go to account service and revoke grant
            accountAppPage.open();

            List<String> additionalGrants = accountAppPage.getApplications().get(OFFLINE_CLIENT_ID).getAdditionalGrants();
            assertThat(additionalGrants.size(), is(1));
            assertThat(additionalGrants.get(0), is("Offline Token"));

            accountAppPage.revokeGrant(OFFLINE_CLIENT_ID);
            pause(500);
            assertThat(accountAppPage.getApplications().get(OFFLINE_CLIENT_ID).getAdditionalGrants().size(), is(0));

            events.expect(EventType.REVOKE_GRANT)
                    .client("account").detail(Details.REVOKED_CLIENT, OFFLINE_CLIENT_ID).assertEvent();

            // Assert refresh doesn't work now (increase time one more time)
            setAdapterAndServerTimeOffset(19999);
            offlineTokenPage.navigateTo();
            assertCurrentUrlDoesntStartWith(offlineTokenPage);
            loginPage.assertCurrent();
        } finally {
            events.clear();
            resetTimeOffsetAuthenticated();
        }
    }

    @Test
    public void testServletWithConsent() throws IOException {
        try (Closeable cau = ClientAttributeUpdater.forClient(adminClient, TEST, OFFLINE_CLIENT_ID)
                .setConsentRequired(true).update()) {

            // Assert grant page doesn't have 'Offline Access' role when offline token is not requested
            offlineTokenPage.navigateTo();

            loginPage.assertCurrent();
            loginPage.login(DEFAULT_USERNAME, DEFAULT_PASSWORD);

            oauthGrantPage.assertCurrent();
            waitUntilElement(By.xpath("//body")).text().not().contains("Offline access");
            oauthGrantPage.cancel();

            // Assert grant page has 'Offline Access' role now
            String servletUri = UriBuilder.fromUri(offlineTokenPage.toString())
                    .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.OFFLINE_ACCESS)
                    .build().toString();
            driver.navigate().to(servletUri);
            waitUntilElement(By.tagName("body")).is().visible();

            loginPage.login(DEFAULT_USERNAME, DEFAULT_PASSWORD);
            oauthGrantPage.assertCurrent();
            waitUntilElement(By.xpath("//body")).text().contains(OAuthGrantPage.OFFLINE_ACCESS_CONSENT_TEXT);

            oauthGrantPage.accept();

            assertCurrentUrlStartsWith(offlineTokenPage);
            assertThat(offlineTokenPage.getRefreshToken(), notNullValue());
            assertThat(offlineTokenPage.getRefreshToken().getType(), is(TokenUtil.TOKEN_TYPE_OFFLINE));

            accountAppPage.open();
            AccountApplicationsPage.AppEntry offlineClient = accountAppPage.getApplications().get(OFFLINE_CLIENT_ID);
            assertThat(offlineClient.getClientScopesGranted(), Matchers.hasItem(OAuthGrantPage.OFFLINE_ACCESS_CONSENT_TEXT));
            assertThat(offlineClient.getAdditionalGrants(), Matchers.hasItem("Offline Token"));

            //This was necessary to be introduced, otherwise other testcases will fail
            offlineTokenPage.logout();
            assertCurrentUrlDoesntStartWith(offlineTokenPage);
            loginPage.assertCurrent();
        } finally {
            events.clear();
            resetTimeOffsetAuthenticated();
        }
    }

    private void setAdapterAndServerTimeOffset(int timeOffset) {
        super.setAdapterAndServerTimeOffset(timeOffset, offlineTokenPage.toString());
    }

    private void resetTimeOffsetAuthenticated() {
        resetTimeOffsetAuthenticated(DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }

    /**
     * Reset time offset for remote environment.
     * After the token expiration, process of re-authentication is necessary.
     *
     * @param username
     * @param password
     */
    private void resetTimeOffsetAuthenticated(String username, String password) {
        if (testContext.getAppServerInfo().isUndertow()) {
            setAdapterAndServerTimeOffset(0);
            return;
        }
        super.setAdapterServletTimeOffset(0, offlineTokenPage.toString());

        if (loginPage.isCurrent()) {
            loginPage.login(username, password);
            waitForPageToLoad();
            offlineTokenPage.logout();
        }
        setTimeOffset(0);
    }
}
