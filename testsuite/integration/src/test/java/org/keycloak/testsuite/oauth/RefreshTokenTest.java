/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.oauth;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.OAuthClient.AccessTokenResponse;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.util.Time;
import org.openqa.selenium.WebDriver;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RefreshTokenTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule();

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected LoginPage loginPage;

    @Test
    public void refreshTokenRequest() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String refreshTokenString = tokenResponse.getRefreshToken();
        RefreshToken refreshToken = oauth.verifyRefreshToken(refreshTokenString);

        Assert.assertNotNull(refreshTokenString);

        Assert.assertEquals("bearer", tokenResponse.getTokenType());

        Assert.assertThat(token.getExpiration() - Time.currentTime(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));
        Assert.assertThat(refreshToken.getExpiration() - Time.currentTime(), allOf(greaterThanOrEqualTo(35950), lessThanOrEqualTo(36000)));

        Thread.sleep(2000);

        AccessTokenResponse response = oauth.doRefreshTokenRequest(refreshTokenString, "password");
        AccessToken refreshedToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.verifyRefreshToken(response.getRefreshToken());

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertThat(response.getExpiresIn(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));
        Assert.assertThat(refreshedToken.getExpiration() - Time.currentTime(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));

        Assert.assertThat(refreshedToken.getExpiration() - token.getExpiration(), allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(3)));
        Assert.assertThat(refreshedRefreshToken.getExpiration() - refreshToken.getExpiration(), allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(3)));

        Assert.assertNotEquals(token.getId(), refreshedToken.getId());
        Assert.assertNotEquals(refreshToken.getId(), refreshedRefreshToken.getId());

        Assert.assertEquals("bearer", response.getTokenType());

        Assert.assertEquals(keycloakRule.getUser("test", "test-user@localhost").getId(), refreshedToken.getSubject());
        Assert.assertNotEquals("test-user@localhost", refreshedToken.getSubject());

        Assert.assertEquals(1, refreshedToken.getRealmAccess().getRoles().size());
        Assert.assertTrue(refreshedToken.getRealmAccess().isUserInRole("user"));

        Assert.assertEquals(1, refreshedToken.getResourceAccess(oauth.getClientId()).getRoles().size());
        Assert.assertTrue(refreshedToken.getResourceAccess(oauth.getClientId()).isUserInRole("customer-user"));
    }

}
