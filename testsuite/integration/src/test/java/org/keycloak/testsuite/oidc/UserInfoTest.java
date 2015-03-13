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
package org.keycloak.testsuite.oidc;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.UserInfo;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.util.BasicAuthHelper;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author pedroigor
 */
public class UserInfoTest {

    private static RealmModel realm;

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule();

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @Test
    public void testSuccessfulUserInfoRequest() throws Exception {
        Client client = ClientBuilder.newClient();
        UriBuilder builder = UriBuilder.fromUri(org.keycloak.testsuite.Constants.AUTH_SERVER_ROOT);
        URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
        WebTarget grantTarget = client.target(grantUri);
        AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(grantTarget);
        Response response = executeUserInfoRequest(accessTokenResponse.getToken());

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        UserInfo userInfo = response.readEntity(UserInfo.class);

        response.close();

        assertNotNull(userInfo);
        assertNotNull(userInfo.getSubject());
        assertEquals("test-user@localhost", userInfo.getEmail());
        assertEquals("test-user@localhost", userInfo.getPreferredUsername());

        client.close();
    }

    @Test
    public void testUnsuccessfulUserInfoRequest() throws Exception {
        Response response = executeUserInfoRequest("bad");

        response.close();

        assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    private AccessTokenResponse executeGrantAccessTokenRequest(WebTarget grantTarget) {
        String header = BasicAuthHelper.createHeader("test-app", "password");
        Form form = new Form();
        form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .param("username", "test-user@localhost")
                .param("password", "password");

        Response response = grantTarget.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));

        assertEquals(200, response.getStatus());

        AccessTokenResponse accessTokenResponse = response.readEntity(AccessTokenResponse.class);

        response.close();

        return accessTokenResponse;
    }

    private Response executeUserInfoRequest(String accessToken) {
        UriBuilder builder = UriBuilder.fromUri(org.keycloak.testsuite.Constants.AUTH_SERVER_ROOT);
        UriBuilder uriBuilder = OIDCLoginProtocolService.tokenServiceBaseUrl(builder);
        URI userInfoUri = uriBuilder.path(OIDCLoginProtocolService.class, "issueUserInfo").build("test");
        Client client = ClientBuilder.newClient();
        WebTarget userInfoTarget = client.target(userInfoUri);

        return userInfoTarget.request()
                .header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken)
                .get();
    }
}
