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
import org.keycloak.audit.Details;
import org.keycloak.audit.Event;
import org.keycloak.representations.AccessToken;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.OAuthClient.AccessTokenResponse;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccessTokenTest {

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

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

    @Test
    public void accessTokenRequest() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertThat(response.getExpiresIn(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));

        Assert.assertEquals("bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        Assert.assertEquals(keycloakRule.getUser("test", "test-user@localhost").getId(), token.getSubject());
        Assert.assertNotEquals("test-user@localhost", token.getSubject());

        Assert.assertEquals(1, token.getRealmAccess().getRoles().size());
        Assert.assertTrue(token.getRealmAccess().isUserInRole("user"));

        Assert.assertEquals(1, token.getResourceAccess(oauth.getClientId()).getRoles().size());
        Assert.assertTrue(token.getResourceAccess(oauth.getClientId()).isUserInRole("customer-user"));

        Event event = events.expectCodeToToken(codeId).assertEvent();
        Assert.assertEquals(token.getId(), event.getDetails().get(Details.TOKEN_ID));
        Assert.assertEquals(oauth.verifyRefreshToken(response.getRefreshToken()).getId(), event.getDetails().get(Details.REFRESH_TOKEN_ID));

        response = oauth.doAccessTokenRequest(code, "password");
        Assert.assertEquals(400, response.getStatusCode());

        events.expectCodeToToken(codeId).error("invalid_code").removeDetail(Details.TOKEN_ID).removeDetail(Details.REFRESH_TOKEN_ID).client((String) null).user((String) null).assertEvent();
    }

    @Test
    public void accessTokenInvalidClientCredentials() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "invalid");
        Assert.assertEquals(400, response.getStatusCode());

        events.expectCodeToToken(codeId).error("invalid_client_credentials").removeDetail(Details.TOKEN_ID).removeDetail(Details.REFRESH_TOKEN_ID).assertEvent();
    }

}
