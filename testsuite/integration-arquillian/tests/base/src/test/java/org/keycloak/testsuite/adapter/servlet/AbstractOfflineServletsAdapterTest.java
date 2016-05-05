package org.keycloak.testsuite.adapter.servlet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.page.OfflineToken;
import org.keycloak.testsuite.pages.AccountApplicationsPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.util.TokenUtil;

import javax.ws.rs.core.UriBuilder;
import java.util.List;

import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
public abstract class AbstractOfflineServletsAdapterTest extends AbstractServletsAdapterTest {

    private static final String OFFLINE_CLIENT_APP_URI = "http://localhost:8280/offline-client";

    @Rule
    public AssertEvents events = new AssertEvents(this);
    @Page
    protected OfflineToken offlineToken;
    @Page
    protected LoginPage loginPage;
    @Page
    protected AccountApplicationsPage accountAppPage;
    @Page
    protected OAuthGrantPage oauthGrantPage;

    @Deployment(name = OfflineToken.DEPLOYMENT_NAME)
    protected static WebArchive offlineClient() {
        return servletDeployment(OfflineToken.DEPLOYMENT_NAME, OfflineTokenServlet.class, ErrorServlet.class);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(TEST);
        testRealmLoginPage.setAuthRealm(TEST);
    }


    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/adapter-test/offline-client/offlinerealm.json"));
    }

    @Test
    public void testServlet() throws Exception {
        String servletUri = UriBuilder.fromUri(OFFLINE_CLIENT_APP_URI)
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.OFFLINE_ACCESS)
                .build().toString();
        oauth.doLogin("test-user@localhost", "password");

        driver.navigate().to(servletUri);

        Assert.assertTrue(driver.getCurrentUrl().startsWith(OFFLINE_CLIENT_APP_URI));

        Assert.assertEquals(offlineToken.getRefreshToken().getType(), TokenUtil.TOKEN_TYPE_OFFLINE);
        Assert.assertEquals(offlineToken.getRefreshToken().getExpiration(), 0);

        String accessTokenId = offlineToken.getAccessToken().getId();
        String refreshTokenId = offlineToken.getRefreshToken().getId();

        setAdapterTimeOffset(9999);

        Assert.assertTrue(driver.getCurrentUrl().startsWith(OFFLINE_CLIENT_APP_URI));
        Assert.assertNotEquals(offlineToken.getRefreshToken().getId(), refreshTokenId);
        Assert.assertNotEquals(offlineToken.getAccessToken().getId(), accessTokenId);

        // Ensure that logout works for webapp (even if offline token will be still valid in Keycloak DB)
        driver.navigate().to(OFFLINE_CLIENT_APP_URI + "/logout");
        loginPage.assertCurrent();
        driver.navigate().to(OFFLINE_CLIENT_APP_URI);
        loginPage.assertCurrent();

        setAdapterTimeOffset(0);
        events.clear();
    }

    @Test
    public void testServletWithRevoke() {
        // Login to servlet first with offline token
        String servletUri = UriBuilder.fromUri(OFFLINE_CLIENT_APP_URI)
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.OFFLINE_ACCESS)
                .build().toString();
        driver.navigate().to(servletUri);
        loginPage.login("test-user@localhost", "password");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(OFFLINE_CLIENT_APP_URI));

        Assert.assertEquals(offlineToken.getRefreshToken().getType(), TokenUtil.TOKEN_TYPE_OFFLINE);

        // Assert refresh works with increased time
        setAdapterTimeOffset(9999);
        driver.navigate().to(OFFLINE_CLIENT_APP_URI);
        Assert.assertTrue(driver.getCurrentUrl().startsWith(OFFLINE_CLIENT_APP_URI));
        setAdapterTimeOffset(0);

        events.clear();

        // Go to account service and revoke grant
        accountAppPage.open();
        List<String> additionalGrants = accountAppPage.getApplications().get("offline-client").getAdditionalGrants();
        Assert.assertEquals(additionalGrants.size(), 1);
        Assert.assertEquals(additionalGrants.get(0), "Offline Token");
        accountAppPage.revokeGrant("offline-client");
        Assert.assertEquals(accountAppPage.getApplications().get("offline-client").getAdditionalGrants().size(), 0);

        events.expect(EventType.REVOKE_GRANT)
                .client("account").detail(Details.REVOKED_CLIENT, "offline-client").assertEvent();

        // Assert refresh doesn't work now (increase time one more time)
        setAdapterTimeOffset(9999);
        driver.navigate().to(OFFLINE_CLIENT_APP_URI);
        Assert.assertFalse(driver.getCurrentUrl().startsWith(OFFLINE_CLIENT_APP_URI));
        loginPage.assertCurrent();
        setAdapterTimeOffset(0);
    }

    @Test
    public void testServletWithConsent() {
        ClientManager.realm(adminClient.realm("test")).clientId("offline-client").consentRequired(true);

        // Assert grant page doesn't have 'Offline Access' role when offline token is not requested
        driver.navigate().to(OFFLINE_CLIENT_APP_URI);
        loginPage.login("test-user@localhost", "password");
        oauthGrantPage.assertCurrent();
        Assert.assertFalse(driver.getPageSource().contains("Offline access"));
        oauthGrantPage.cancel();

        // Assert grant page has 'Offline Access' role now
        String servletUri = UriBuilder.fromUri(OFFLINE_CLIENT_APP_URI)
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.OFFLINE_ACCESS)
                .build().toString();
        driver.navigate().to(servletUri);
        loginPage.login("test-user@localhost", "password");
        oauthGrantPage.assertCurrent();
        Assert.assertTrue(driver.getPageSource().contains("Offline access"));
        oauthGrantPage.accept();

        Assert.assertTrue(driver.getCurrentUrl().startsWith(OFFLINE_CLIENT_APP_URI));
        Assert.assertEquals(offlineToken.getRefreshToken().getType(), TokenUtil.TOKEN_TYPE_OFFLINE);

        accountAppPage.open();
        AccountApplicationsPage.AppEntry offlineClient = accountAppPage.getApplications().get("offline-client");
        Assert.assertTrue(offlineClient.getRolesGranted().contains("Offline access"));
        Assert.assertTrue(offlineClient.getAdditionalGrants().contains("Offline Token"));

        //This was necessary to be introduced, otherwise other testcases will fail
        driver.navigate().to(OFFLINE_CLIENT_APP_URI + "/logout");
        loginPage.assertCurrent();

        events.clear();

        // Revert change
        ClientManager.realm(adminClient.realm("test")).clientId("offline-client").consentRequired(false);

    }

    private void setAdapterTimeOffset(int timeOffset) {
        Time.setOffset(timeOffset);
        String timeOffsetUri = UriBuilder.fromUri(OFFLINE_CLIENT_APP_URI)
                .queryParam("timeOffset", timeOffset)
                .build().toString();

        driver.navigate().to(timeOffsetUri);
    }

}
