/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.oauth;

import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
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
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ServiceAccountTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {
        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            ClientModel app = KeycloakModelUtils.createClient(appRealm, "service-account-cl");
            app.setSecret("secret1");
            new ClientManager(manager).enableServiceAccount(app);

            ClientModel disabledApp = KeycloakModelUtils.createClient(appRealm, "service-account-disabled");
            disabledApp.setSecret("secret1");

            UserModel serviceAccountUser = session.users().getUserByUsername(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "service-account-cl", appRealm);
            userId = serviceAccountUser.getId();
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
    public void clientCredentialsAuthSuccess() throws Exception {
        oauth.clientId("service-account-cl");

        OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("secret1");

        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.verifyRefreshToken(response.getRefreshToken());

        events.expectClientLogin()
                .client("service-account-cl")
                .user(userId)
                .session(accessToken.getSessionState())
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "service-account-cl")
                .assertEvent();

        assertEquals(accessToken.getSessionState(), refreshToken.getSessionState());
        System.out.println("Access token other claims: " + accessToken.getOtherClaims());
        Assert.assertEquals("service-account-cl", accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ID));
        Assert.assertTrue(accessToken.getOtherClaims().containsKey(ServiceAccountConstants.CLIENT_ADDRESS));
        Assert.assertTrue(accessToken.getOtherClaims().containsKey(ServiceAccountConstants.CLIENT_HOST));

        OAuthClient.AccessTokenResponse refreshedResponse = oauth.doRefreshTokenRequest(response.getRefreshToken(), "secret1");

        AccessToken refreshedAccessToken = oauth.verifyToken(refreshedResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.verifyRefreshToken(refreshedResponse.getRefreshToken());

        assertEquals(accessToken.getSessionState(), refreshedAccessToken.getSessionState());
        assertEquals(accessToken.getSessionState(), refreshedRefreshToken.getSessionState());

        events.expectRefresh(refreshToken.getId(), refreshToken.getSessionState()).user(userId).client("service-account-cl").assertEvent();
    }

    @Test
    public void clientCredentialsLogout() throws Exception {
        oauth.clientId("service-account-cl");

        OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("secret1");

        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.verifyRefreshToken(response.getRefreshToken());

        events.expectClientLogin()
                .client("service-account-cl")
                .user(userId)
                .session(accessToken.getSessionState())
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "service-account-cl")
                .detail(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID)
                .assertEvent();

        HttpResponse logoutResponse = oauth.doLogout(response.getRefreshToken(), "secret1");
        assertEquals(204, logoutResponse.getStatusLine().getStatusCode());
        events.expectLogout(accessToken.getSessionState())
                .client("service-account-cl")
                .user(userId)
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();

        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), "secret1");
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());

        events.expectRefresh(refreshToken.getId(), refreshToken.getSessionState())
                .client("service-account-cl")
                .user(userId)
                .removeDetail(Details.TOKEN_ID)
                .removeDetail(Details.UPDATED_REFRESH_TOKEN_ID)
                .error(Errors.INVALID_TOKEN).assertEvent();
    }

    @Test
    public void clientCredentialsInvalidClientCredentials() throws Exception {
        oauth.clientId("service-account-cl");

        OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("secret2");

        assertEquals(400, response.getStatusCode());

        assertEquals("unauthorized_client", response.getError());

        events.expectClientLogin()
                .client("service-account-cl")
                .session((String) null)
                .clearDetails()
                .error(Errors.INVALID_CLIENT_CREDENTIALS)
                .user((String) null)
                .assertEvent();
    }

    @Test
    public void clientCredentialsDisabledServiceAccount() throws Exception {
        oauth.clientId("service-account-disabled");

        OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("secret1");

        assertEquals(401, response.getStatusCode());

        assertEquals("unauthorized_client", response.getError());

        events.expectClientLogin()
                .client("service-account-disabled")
                .user((String) null)
                .session((String) null)
                .removeDetail(Details.USERNAME)
                .removeDetail(Details.RESPONSE_TYPE)
                .error(Errors.INVALID_CLIENT)
                .assertEvent();
    }

    @Test
    public void changeClientIdTest() throws Exception {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                ClientModel app = appRealm.getClientByClientId("service-account-cl");
                app.setClientId("updated-client");
            }

        });

        oauth.clientId("updated-client");

        OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("secret1");

        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.verifyRefreshToken(response.getRefreshToken());
        Assert.assertEquals("updated-client", accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ID));

        // Username still same. Client ID changed
        events.expectClientLogin()
                .client("updated-client")
                .user(userId)
                .session(accessToken.getSessionState())
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "service-account-cl")
                .assertEvent();

        // Revert change
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                ClientModel app = appRealm.getClientByClientId("updated-client");
                app.setClientId("service-account-cl");
            }

        });
    }

}
