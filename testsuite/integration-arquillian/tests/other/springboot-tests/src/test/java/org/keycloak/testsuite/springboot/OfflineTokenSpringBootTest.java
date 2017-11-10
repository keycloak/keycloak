package org.keycloak.testsuite.springboot;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AccountApplicationsPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.util.TokenUtil;
import org.openqa.selenium.By;

import javax.ws.rs.core.UriBuilder;
import java.util.List;

import static org.keycloak.testsuite.util.WaitUtils.pause;

public class OfflineTokenSpringBootTest extends AbstractSpringBootTest {
    private static final String SERVLET_URL = BASE_URL + "/TokenServlet";

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    private AccountApplicationsPage accountAppPage;

    @Page
    private OAuthGrantPage oauthGrantPage;

    @Test
    public void testTokens() {
        String servletUri = UriBuilder.fromUri(SERVLET_URL)
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.OFFLINE_ACCESS)
                .build().toString();
        driver.navigate().to(servletUri);

        Assert.assertTrue("Must be on login page", loginPage.isCurrent());
        loginPage.login(USER_LOGIN, USER_PASSWORD);

        WaitUtils.waitUntilElement(By.tagName("body")).is().visible();

        Assert.assertTrue("Must be on tokens page", tokenPage.isCurrent());

        Assert.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, tokenPage.getRefreshToken().getType());
        Assert.assertEquals(0, tokenPage.getRefreshToken().getExpiration());

        String accessTokenId = tokenPage.getAccessToken().getId();
        String refreshTokenId = tokenPage.getRefreshToken().getId();

        setAdapterAndServerTimeOffset(9999, SERVLET_URL);

        driver.navigate().to(SERVLET_URL);
        Assert.assertTrue("Must be on tokens page", tokenPage.isCurrent());
        Assert.assertNotEquals(refreshTokenId, tokenPage.getRefreshToken().getId());
        Assert.assertNotEquals(accessTokenId, tokenPage.getAccessToken().getId());

        setAdapterAndServerTimeOffset(0, SERVLET_URL);

        driver.navigate().to(logoutPage(SERVLET_URL));
        Assert.assertTrue("Must be on login page", loginPage.isCurrent());
    }

    @Test
    public void testRevoke() {
        // Login to servlet first with offline token
        String servletUri = UriBuilder.fromUri(SERVLET_URL)
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.OFFLINE_ACCESS)
                .build().toString();
        driver.navigate().to(servletUri);
        WaitUtils.waitUntilElement(By.tagName("body")).is().visible();

        loginPage.login(USER_LOGIN, USER_PASSWORD);
        Assert.assertTrue("Must be on token page", tokenPage.isCurrent());

        Assert.assertEquals(tokenPage.getRefreshToken().getType(), TokenUtil.TOKEN_TYPE_OFFLINE);

        // Assert refresh works with increased time
        setAdapterAndServerTimeOffset(9999, SERVLET_URL);
        driver.navigate().to(SERVLET_URL);
        Assert.assertTrue("Must be on token page", tokenPage.isCurrent());
        setAdapterAndServerTimeOffset(0, SERVLET_URL);

        events.clear();

        // Go to account service and revoke grant
        accountAppPage.open();

        List<String> additionalGrants = accountAppPage.getApplications().get(CLIENT_ID).getAdditionalGrants();
        Assert.assertEquals(additionalGrants.size(), 1);
        Assert.assertEquals(additionalGrants.get(0), "Offline Token");
        accountAppPage.revokeGrant(CLIENT_ID);
        pause(500);
        Assert.assertEquals(accountAppPage.getApplications().get(CLIENT_ID).getAdditionalGrants().size(), 0);

        UserRepresentation userRepresentation =
                ApiUtil.findUserByUsername(realmsResouce().realm(REALM_NAME), USER_LOGIN);
        Assert.assertNotNull("User should exist", userRepresentation);

        events.expect(EventType.REVOKE_GRANT).realm(REALM_ID).user(userRepresentation.getId())
                .client("account").detail(Details.REVOKED_CLIENT, CLIENT_ID).assertEvent();

        // Assert refresh doesn't work now (increase time one more time)
        setAdapterAndServerTimeOffset(9999, SERVLET_URL);
        driver.navigate().to(SERVLET_URL);
        loginPage.assertCurrent();
        setAdapterAndServerTimeOffset(0, SERVLET_URL);
    }

    @Test
    public void testConsent() {
        ClientManager.realm(adminClient.realm(REALM_NAME)).clientId(CLIENT_ID).consentRequired(true);

        // Assert grant page doesn't have 'Offline Access' role when offline token is not requested
        driver.navigate().to(SERVLET_URL);
        loginPage.login(USER_LOGIN, USER_PASSWORD);
        oauthGrantPage.assertCurrent();
        WaitUtils.waitUntilElement(By.xpath("//body")).text().not().contains("Offline access");
        oauthGrantPage.cancel();

        driver.navigate().to(UriBuilder.fromUri(SERVLET_URL)
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.OFFLINE_ACCESS)
                .build().toString());
        WaitUtils.waitUntilElement(By.tagName("body")).is().visible();

        loginPage.login(USER_LOGIN, USER_PASSWORD);
        oauthGrantPage.assertCurrent();
        WaitUtils.waitUntilElement(By.xpath("//body")).text().contains("Offline access");

        oauthGrantPage.accept();

        Assert.assertTrue("Must be on token page", tokenPage.isCurrent());
        Assert.assertEquals(tokenPage.getRefreshToken().getType(), TokenUtil.TOKEN_TYPE_OFFLINE);

        String accountAppPageUrl =
            Urls.accountApplicationsPage(getAuthServerRoot(), REALM_NAME).toString();
        driver.navigate().to(accountAppPageUrl);
        AccountApplicationsPage.AppEntry offlineClient = accountAppPage.getApplications().get(CLIENT_ID);
        Assert.assertTrue(offlineClient.getRolesGranted().contains("Offline access"));
        Assert.assertTrue(offlineClient.getAdditionalGrants().contains("Offline Token"));

        //This was necessary to be introduced, otherwise other testcases will fail
        driver.navigate().to(logoutPage(SERVLET_URL));
        loginPage.assertCurrent();

        events.clear();

        // Revert change
        ClientManager.realm(adminClient.realm(REALM_NAME)).clientId(CLIENT_ID).consentRequired(false);
    }
}
