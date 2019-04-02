package org.keycloak.testsuite.adapter.servlet;

import javax.ws.rs.core.UriBuilder;
import java.util.List;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.filter.AdapterActionsFilter;
import org.keycloak.testsuite.adapter.page.OfflineToken;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import org.keycloak.testsuite.pages.AccountApplicationsPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.keycloak.util.TokenUtil;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;

import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlDoesntStartWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.pause;
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
    public void testServlet() throws Exception {
        String servletUri = UriBuilder.fromUri(offlineTokenPage.toString())
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.OFFLINE_ACCESS)
                .build().toString();

        driver.navigate().to(servletUri);
        waitUntilElement(By.tagName("body")).is().visible();

        loginPage.login("test-user@localhost", "password");

        assertCurrentUrlStartsWith(offlineTokenPage);

        Assert.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineTokenPage.getRefreshToken().getType());
        Assert.assertEquals(0, offlineTokenPage.getRefreshToken().getExpiration());

        String accessTokenId = offlineTokenPage.getAccessToken().getId();
        String refreshTokenId = offlineTokenPage.getRefreshToken().getId();

        setAdapterAndServerTimeOffset(9999);
        offlineTokenPage.navigateTo();
        assertCurrentUrlStartsWith(offlineTokenPage);
        Assert.assertNotEquals(offlineTokenPage.getRefreshToken().getId(), refreshTokenId);
        Assert.assertNotEquals(offlineTokenPage.getAccessToken().getId(), accessTokenId);

        // Ensure that logout works for webapp (even if offline token will be still valid in Keycloak DB)
        offlineTokenPage.logout();
        assertCurrentUrlDoesntStartWith(offlineTokenPage);
        loginPage.assertCurrent();
        offlineTokenPage.navigateTo();
        assertCurrentUrlDoesntStartWith(offlineTokenPage);
        loginPage.assertCurrent();

        setAdapterAndServerTimeOffset(0);
        events.clear();
    }

    @Test
    public void testServletWithRevoke() {
        // Login to servlet first with offline token
        String servletUri = UriBuilder.fromUri(offlineTokenPage.toString())
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.OFFLINE_ACCESS)
                .build().toString();
        driver.navigate().to(servletUri);
        waitUntilElement(By.tagName("body")).is().visible();

        loginPage.login("test-user@localhost", "password");
        assertCurrentUrlStartsWith(offlineTokenPage);

        Assert.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineTokenPage.getRefreshToken().getType());

        // Assert refresh works with increased time
        setAdapterAndServerTimeOffset(9999);
        offlineTokenPage.navigateTo();
        assertCurrentUrlStartsWith(offlineTokenPage);
        setAdapterAndServerTimeOffset(0);

        events.clear();

        // Go to account service and revoke grant
        accountAppPage.open();

        List<String> additionalGrants = accountAppPage.getApplications().get("offline-client").getAdditionalGrants();
        Assert.assertEquals(1, additionalGrants.size());
        Assert.assertEquals("Offline Token", additionalGrants.get(0));
        accountAppPage.revokeGrant("offline-client");
        pause(500);
        Assert.assertEquals(0, accountAppPage.getApplications().get("offline-client").getAdditionalGrants().size());

        events.expect(EventType.REVOKE_GRANT)
                .client("account").detail(Details.REVOKED_CLIENT, "offline-client").assertEvent();

        // Assert refresh doesn't work now (increase time one more time)
        setAdapterAndServerTimeOffset(9999);
        offlineTokenPage.navigateTo();
        assertCurrentUrlDoesntStartWith(offlineTokenPage);
        loginPage.assertCurrent();
        setAdapterAndServerTimeOffset(0);
    }

    @Test
    public void testServletWithConsent() {
        ClientManager.realm(adminClient.realm("test")).clientId("offline-client").consentRequired(true);

        // Assert grant page doesn't have 'Offline Access' role when offline token is not requested
        offlineTokenPage.navigateTo();
        loginPage.login("test-user@localhost", "password");
        oauthGrantPage.assertCurrent();
        waitUntilElement(By.xpath("//body")).text().not().contains("Offline access");
        oauthGrantPage.cancel();

        // Assert grant page has 'Offline Access' role now
        String servletUri = UriBuilder.fromUri(offlineTokenPage.toString())
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.OFFLINE_ACCESS)
                .build().toString();
        driver.navigate().to(servletUri);
        waitUntilElement(By.tagName("body")).is().visible();

        loginPage.login("test-user@localhost", "password");
        oauthGrantPage.assertCurrent();
        waitUntilElement(By.xpath("//body")).text().contains(OAuthGrantPage.OFFLINE_ACCESS_CONSENT_TEXT);

        oauthGrantPage.accept();

        assertCurrentUrlStartsWith(offlineTokenPage);
        Assert.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineTokenPage.getRefreshToken().getType());

        accountAppPage.open();
        AccountApplicationsPage.AppEntry offlineClient = accountAppPage.getApplications().get("offline-client");
        Assert.assertThat(offlineClient.getClientScopesGranted(), Matchers.hasItem(OAuthGrantPage.OFFLINE_ACCESS_CONSENT_TEXT));
        Assert.assertThat(offlineClient.getAdditionalGrants(), Matchers.hasItem("Offline Token"));

        //This was necessary to be introduced, otherwise other testcases will fail
        offlineTokenPage.logout();
        assertCurrentUrlDoesntStartWith(offlineTokenPage);
        loginPage.assertCurrent();

        events.clear();

        // Revert change
        ClientManager.realm(adminClient.realm("test")).clientId("offline-client").consentRequired(false);

    }

    private void setAdapterAndServerTimeOffset(int timeOffset) {
        super.setAdapterAndServerTimeOffset(timeOffset, offlineTokenPage.toString());
    }

}
