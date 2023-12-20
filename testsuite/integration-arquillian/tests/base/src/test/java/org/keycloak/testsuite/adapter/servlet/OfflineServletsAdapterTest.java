package org.keycloak.testsuite.adapter.servlet;

import jakarta.ws.rs.core.UriBuilder;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.filter.AdapterActionsFilter;
import org.keycloak.testsuite.adapter.page.OfflineToken;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.InfinispanTestTimeServiceRule;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.keycloak.util.TokenUtil;
import org.openqa.selenium.By;

import java.io.Closeable;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
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
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
public class OfflineServletsAdapterTest extends AbstractServletsAdapterTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);
    @Page
    protected OfflineToken offlineTokenPage;
    @Page
    protected LoginPage loginPage;
    @Page
    protected OAuthGrantPage oauthGrantPage;

    @Rule
    public InfinispanTestTimeServiceRule ispnTestTimeService = new InfinispanTestTimeServiceRule(this);

    private final String DEFAULT_USERNAME = "test-user@localhost";
    private final String DEFAULT_PASSWORD = "password";
    private final String OFFLINE_CLIENT_ID = "offline-client";

    /**
     * URL in the deployment that doesn't require authentication and which therefore can be used to trigger activities in the actionfilter.
     */
    private static final String UNSECURED_URL = "unsecured/foo";

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

    @Override
    protected boolean isImportAfterEachMethod() {
        // For proper cleanup of test methods
        return true;
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

            RefreshToken refreshToken = offlineTokenPage.getRefreshToken();
            assertThat(TokenUtil.TOKEN_TYPE_OFFLINE, is(refreshToken.getType()));
            assertThat(refreshToken.getExp(), nullValue());

            String accessTokenId = offlineTokenPage.getAccessToken().getId();
            String refreshTokenId = refreshToken.getId();

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
            resetTimeOffset();
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

            final RefreshToken refreshToken = offlineTokenPage.getRefreshToken();
            assertThat(refreshToken.getType(), is(TokenUtil.TOKEN_TYPE_OFFLINE));

            // Assert refresh works with increased time
            setAdapterAndServerTimeOffset(9999);
            offlineTokenPage.navigateTo();
            assertCurrentUrlStartsWith(offlineTokenPage);
            setAdapterAndServerTimeOffset(0);

            events.clear();

            // Check that Offline Token is granted
            List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(adminClient.realm(TEST), DEFAULT_USERNAME);
            String actualValue = String.valueOf(((LinkedHashMap) ((ArrayList) userConsents.get(0).get("additionalGrants")).get(0)).get("key"));
            Assert.assertEquals("Offline Token", actualValue);

            // Revoke consents
            AccountHelper.revokeConsents(adminClient.realm(TEST), DEFAULT_USERNAME, OFFLINE_CLIENT_ID);
            pause(500);

            userConsents = AccountHelper.getUserConsents(adminClient.realm(TEST), DEFAULT_USERNAME);
            assertThat(userConsents.size(), is(0));

            // Assert refresh doesn't work now (increase time one more time)
            setAdapterAndServerTimeOffset(19999);
            offlineTokenPage.navigateTo();
            assertCurrentUrlDoesntStartWith(offlineTokenPage);
            loginPage.assertCurrent();
        } finally {
            events.clear();
            resetTimeOffset();
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

            RefreshToken refreshToken = offlineTokenPage.getRefreshToken();
            assertThat(refreshToken.getType(), is(TokenUtil.TOKEN_TYPE_OFFLINE));

            // Check that the client scopes have been granted by the user
            List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(adminClient.realm(TEST), DEFAULT_USERNAME);
            Assert.assertTrue(((List) userConsents.get(0).get("grantedClientScopes")).stream().anyMatch(p -> p.equals("offline_access")));
            String actualValue = String.valueOf(((LinkedHashMap) ((ArrayList) userConsents.get(0).get("additionalGrants")).get(0)).get("key"));
            Assert.assertEquals("Offline Token", actualValue);

            AccountHelper.logout(adminClient.realm(TEST), DEFAULT_USERNAME);
        } finally {
            events.clear();
            resetTimeOffset();
        }
    }

    private void setAdapterAndServerTimeOffset(int timeOffset) {
        super.setAdapterAndServerTimeOffset(timeOffset, offlineTokenPage.toString() + UNSECURED_URL);
    }

    @Override
    public void resetTimeOffset() {
        setAdapterServletTimeOffset(0, offlineTokenPage.toString() + UNSECURED_URL);
        super.resetTimeOffset();
    }

    @Override
    protected void afterAbstractKeycloakTestRealmImport() {
        // after each re-import, ensure that the information stored in JWKPublicKeyLocator is reset
        String resetDeploymentUri = UriBuilder.fromUri(offlineTokenPage.toString() + UNSECURED_URL)
                .queryParam(AdapterActionsFilter.RESET_DEPLOYMENT_PARAM, "true")
                .build().toString();
        driver.navigate().to(resetDeploymentUri);
        waitForPageToLoad();

        assertThat(driver.getPageSource(), containsString("Restarted PublicKeyLocator"));
        super.afterAbstractKeycloakTestRealmImport();
    }

}
