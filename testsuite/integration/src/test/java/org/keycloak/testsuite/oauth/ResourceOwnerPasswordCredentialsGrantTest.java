package org.keycloak.testsuite.oauth;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ResourceOwnerPasswordCredentialsGrantTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {
        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            ClientModel app = new ClientManager(manager).createClient(appRealm, "resource-owner");
            app.setSecret("secret");

            UserModel user = session.users().addUser(appRealm, "direct-login");
            user.setEmail("direct-login@localhost");
            user.setEnabled(true);

            userId = user.getId();

            session.users().updateCredential(appRealm, user, UserCredentialModel.password("password"));
        }
    });

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    private static String userId;

    @Test
    public void grantAccessTokenUsername() throws Exception {
        grantAccessToken("direct-login");
    }

    @Test
    public void grantAccessTokenEmail() throws Exception {
        grantAccessToken("direct-login@localhost");
    }

    private void grantAccessToken(String login) throws Exception {
        oauth.clientId("resource-owner");

        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", login, "password");

        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.verifyRefreshToken(response.getRefreshToken());

        events.expectLogin()
                .client("resource-owner")
                .user(userId)
                .session(accessToken.getSessionState())
                .detail(Details.RESPONSE_TYPE, "token")
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.USERNAME, login)
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .assertEvent();

        assertEquals(accessToken.getSessionState(), refreshToken.getSessionState());

        OAuthClient.AccessTokenResponse refreshedResponse = oauth.doRefreshTokenRequest(response.getRefreshToken(), "secret");

        AccessToken refreshedAccessToken = oauth.verifyToken(refreshedResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.verifyRefreshToken(refreshedResponse.getRefreshToken());

        assertEquals(accessToken.getSessionState(), refreshedAccessToken.getSessionState());
        assertEquals(accessToken.getSessionState(), refreshedRefreshToken.getSessionState());

        events.expectRefresh(refreshToken.getId(), refreshToken.getSessionState()).user(userId).client("resource-owner").assertEvent();
    }

    @Test
    public void grantAccessTokenLogout() throws Exception {
        oauth.clientId("resource-owner");

        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "test-user@localhost", "password");

        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.verifyRefreshToken(response.getRefreshToken());

        events.expectLogin()
                .client("resource-owner")
                .session(accessToken.getSessionState())
                .detail(Details.RESPONSE_TYPE, "token")
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .detail(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID)
                .assertEvent();

        HttpResponse logoutResponse = oauth.doLogout(response.getRefreshToken(), "secret");
        assertEquals(204, logoutResponse.getStatusLine().getStatusCode());
        events.expectLogout(accessToken.getSessionState()).client("resource-owner").removeDetail(Details.REDIRECT_URI).assertEvent();

        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), "secret");
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());

        events.expectRefresh(refreshToken.getId(), refreshToken.getSessionState()).client("resource-owner")
                .removeDetail(Details.TOKEN_ID)
                .removeDetail(Details.UPDATED_REFRESH_TOKEN_ID)
                .error(Errors.INVALID_TOKEN).assertEvent();
    }



    @Test
    public void grantAccessTokenInvalidClientCredentials() throws Exception {
        oauth.clientId("resource-owner");

        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("invalid", "test-user@localhost", "password");

        assertEquals(400, response.getStatusCode());

        assertEquals("unauthorized_client", response.getError());

        events.expectLogin()
                .client("resource-owner")
                .session((String) null)
                .clearDetails()
                .error(Errors.INVALID_CLIENT_CREDENTIALS)
                .user((String) null)
                .assertEvent();
    }

    @Test
    public void grantAccessTokenVerifyEmail() throws Exception {

        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.setVerifyEmail(true);
            }
        });


        oauth.clientId("resource-owner");

        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "test-user@localhost", "password");

        assertEquals(400, response.getStatusCode());

        assertEquals("invalid_grant", response.getError());
        assertEquals("Account is not fully set up", response.getErrorDescription());

        events.expectLogin()
                .client("resource-owner")
                .session((String) null)
                .clearDetails()
                .error(Errors.RESOLVE_REQUIRED_ACTIONS)
                .user((String) null)
                .assertEvent();

        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.setVerifyEmail(false);
                UserModel user = manager.getSession().users().getUserByEmail("test-user@localhost", appRealm);
                user.removeRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL);
            }
        });

    }




    @Test
    public void grantAccessTokenInvalidUserCredentials() throws Exception {
        oauth.clientId("resource-owner");

        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "test-user@localhost", "invalid");

        assertEquals(401, response.getStatusCode());

        assertEquals("invalid_grant", response.getError());

        events.expectLogin()
                .client("resource-owner")
                .session((String) null)
                .detail(Details.RESPONSE_TYPE, "token")
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .error(Errors.INVALID_USER_CREDENTIALS)
                .assertEvent();
    }

    @Test
    public void grantAccessTokenUserNotFound() throws Exception {
        oauth.clientId("resource-owner");

        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "invalid", "invalid");

        assertEquals(401, response.getStatusCode());

        assertEquals("invalid_grant", response.getError());

        events.expectLogin()
                .client("resource-owner")
                .user((String) null)
                .session((String) null)
                .detail(Details.RESPONSE_TYPE, "token")
                .detail(Details.USERNAME, "invalid")
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .error(Errors.INVALID_USER_CREDENTIALS)
                .assertEvent();
    }

    @Test
    public void grantAccessTokenMissingGrantType() throws Exception {
        oauth.clientId("resource-owner");

        DefaultHttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost(oauth.getResourceOwnerPasswordCredentialGrantUrl());
            OAuthClient.AccessTokenResponse response = new OAuthClient.AccessTokenResponse(client.execute(post));

            assertEquals(400, response.getStatusCode());

            assertEquals("invalid_request", response.getError());
            assertEquals("Missing form parameter: grant_type", response.getErrorDescription());
        } finally {
            client.close();
        }
    }

}
