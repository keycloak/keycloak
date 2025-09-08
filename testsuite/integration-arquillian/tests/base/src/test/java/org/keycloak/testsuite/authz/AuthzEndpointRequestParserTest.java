package org.keycloak.testsuite.authz;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang.RandomStringUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.OAuthClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class AuthzEndpointRequestParserTest extends AbstractTestRealmKeycloakTest {

    @Page
    protected LoginPage loginPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void test_authentication_backwards_compatible() {

        try (Client client = AdminClientUtil.createResteasyClient()) {

            oauth.addCustomParameter("paramkey1_too_long", RandomStringUtils.random(2000 + 1));
            oauth.addCustomParameter("paramkey2", "paramvalue2");
            oauth.addCustomParameter("paramkey3", "paramvalue3");
            oauth.addCustomParameter("paramkey4", "paramvalue4");
            oauth.addCustomParameter("paramkey5", "paramvalue5");
            oauth.addCustomParameter("paramkey6_too_many", "paramvalue6");

            try (Response response = client.target(oauth.getLoginFormUrl()).request().get()) {

                assertThat(response.getStatus(), is(equalTo(200)));
                assertThat(response, Matchers.body(containsString("Sign in")));

            }

        }

    }

    @Test
    public void testParamsLength() {
        // Login hint with length 200 allowed, state with length 200 allowed
        String loginHint200 = SecretGenerator.getInstance().randomString(200);
        String state200 = SecretGenerator.getInstance().randomString(200);
        oauth
                .loginHint(loginHint200)
                .stateParamHardcoded(state200)
                .openLoginForm();
        assertLogin(loginHint200, state200);

        // Login hint with length 500 not allowed, state with length 500 allowed
        String loginHint500 = SecretGenerator.getInstance().randomString(500);
        String state500 = SecretGenerator.getInstance().randomString(500);
        oauth
                .loginHint(loginHint500)
                .stateParamHardcoded(state500)
                .openLoginForm();
        assertLogin("", state500);

        // state with length 4100 not allowed
        String state4100 = SecretGenerator.getInstance().randomString(4100);
        oauth
                .stateParamHardcoded(state4100)
                .openLoginForm();
        assertLogin("", null);
    }

    protected void assertLogin(String loginHintExpected, String stateExpected) {
        loginPage.assertCurrent();
        Assert.assertEquals(loginHintExpected, loginPage.getUsername());
        loginPage.login("test-user@localhost", "password");

        // String currentUrl = driver.getCurrentUrl();
        OAuthClient.AuthorizationEndpointResponse response = new OAuthClient.AuthorizationEndpointResponse(oauth);
        String state = response.getState();
        Assert.assertEquals(stateExpected, state);

        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
        user.logout();
    }

}
