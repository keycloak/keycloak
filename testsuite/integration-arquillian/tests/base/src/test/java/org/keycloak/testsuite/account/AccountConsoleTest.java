package org.keycloak.testsuite.account;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.OAuthClient;

public class AccountConsoleTest extends AbstractTestRealmKeycloakTest {

    @Page
    protected LoginPage loginPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    @Test
    public void redirectToLoginIfNotAuthenticated() throws Exception {

        String accountUrl = oauth.getCurrentUri().toString().replace("/admin/master/console", "/realms/" + oauth.getRealm() + "/account");

        HttpGet getAccount = new HttpGet(accountUrl);

        int statusCode;
        String redirectLocation;
        try (var client = HttpClientBuilder.create().disableRedirectHandling().build()) {
            try (var response = client.execute(getAccount)) {
                statusCode  = response.getStatusLine().getStatusCode();
                redirectLocation = response.getFirstHeader("Location").getValue();
            }
        }

        Assert.assertEquals(302, statusCode);
        String expectedLoginUrlPart = "/realms/" + oauth.getRealm() + "/protocol/openid-connect/auth?client_id=account";
        Assert.assertTrue(redirectLocation.contains(expectedLoginUrlPart));
    }
}
