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

import java.io.IOException;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.util.OAuthClient;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;

/**
 * Test for "client_secret_post" client authentication (clientID + clientSecret sent in the POST body instead of in "Authorization: Basic" header)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientAuthPostMethodTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);


    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }


    @Test
    public void testPostAuthentication() {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = doAccessTokenRequestPostAuth(code, "password");

        assertEquals(200, response.getStatusCode());

        Assert.assertThat(response.getExpiresIn(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));
        Assert.assertThat(response.getRefreshExpiresIn(), allOf(greaterThanOrEqualTo(1750), lessThanOrEqualTo(1800)));

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        EventRepresentation event = events.expectCodeToToken(codeId, sessionId).assertEvent();
        assertEquals(token.getId(), event.getDetails().get(Details.TOKEN_ID));
        assertEquals(oauth.parseRefreshToken(response.getRefreshToken()).getId(), event.getDetails().get(Details.REFRESH_TOKEN_ID));
        assertEquals(sessionId, token.getSessionState());
    }


    private OAuthClient.AccessTokenResponse doAccessTokenRequestPostAuth(String code, String clientSecret) {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(oauth.getAccessTokenUrl());

            List<NameValuePair> parameters = new LinkedList<>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
            parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));

            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, oauth.getClientId()));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_SECRET, clientSecret));


            UrlEncodedFormEntity formEntity;
            try {
                formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            post.setEntity(formEntity);

            try {
                return new OAuthClient.AccessTokenResponse(client.execute(post));
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve access token", e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



}
