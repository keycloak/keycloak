package org.keycloak.testsuite.account;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.pages.LoginPage;

public class AccountConsoleTest extends AbstractTestRealmKeycloakTest {

    @Page
    protected LoginPage loginPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    @Test
    public void redirectToLoginIfNotAuthenticated() {
        assertRedirectLocation(getAccount());
    }

    @Test
    public void testScopesPresentInAuthorizationRequest() {
        String expectedScopes = "phone address";
        String redirectLocation = URLDecoder.decode(assertRedirectLocation(getAccount(expectedScopes)));
        Assert.assertTrue(redirectLocation.contains(expectedScopes));
        expectedScopes = "phone";
        redirectLocation = URLDecoder.decode(assertRedirectLocation(getAccount(expectedScopes)));
        Assert.assertTrue(redirectLocation.contains(expectedScopes));
        Assert.assertTrue(!redirectLocation.contains("address"));

        // should render the account with the phone scope
        driver.navigate().to(redirectLocation);
        loginPage.login("test-user@localhost", "password");
        Assert.assertTrue(driver.getPageSource().contains("\"scope\": \"" + expectedScopes + "\""));

        // should render the account with the address scope only
        expectedScopes = "address";
        redirectLocation = URLDecoder.decode(assertRedirectLocation(getAccount(expectedScopes)));
        driver.navigate().to(redirectLocation);
        Assert.assertTrue(driver.getPageSource().contains("\"scope\": \"" + expectedScopes + "\""));

        // should render the account with the phone and address scopes
        expectedScopes = "phone address";
        redirectLocation = URLDecoder.decode(assertRedirectLocation(getAccount(expectedScopes)));
        driver.navigate().to(redirectLocation);
        Assert.assertTrue(driver.getPageSource().contains("\"scope\": \"" + expectedScopes + "\""));

        // should keep previously requested scopes when not setting the scope parameter
        redirectLocation = URLDecoder.decode(assertRedirectLocation(getAccount()));
        driver.navigate().to(redirectLocation);
        Assert.assertTrue(driver.getPageSource().contains("\"scope\": \"" + expectedScopes + "\""));
    }

    private CloseableHttpResponse getAccount() {
        return getAccount(null);
    }

    private CloseableHttpResponse getAccount(String scope) {
        try {
            var uriBuilder = new URIBuilder(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/test/account");

            if (scope != null) {
                uriBuilder.setParameter(OIDCLoginProtocol.SCOPE_PARAM, scope);
            }

            var request = new HttpGet(uriBuilder.build());

            try (var client = HttpClientBuilder.create().disableRedirectHandling().build()) {
                return client.execute(request);
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String assertRedirectLocation(CloseableHttpResponse Account) {
        try (var response = Account) {
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertEquals(302, statusCode);
            String expectedLoginUrlPart = "/realms/" + oauth.getRealm() + "/protocol/openid-connect/auth?client_id=" + Constants.ACCOUNT_CONSOLE_CLIENT_ID;
            String redirectLocation = response.getFirstHeader("Location").getValue();
            Assert.assertTrue(redirectLocation.contains(expectedLoginUrlPart));
            return redirectLocation;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
