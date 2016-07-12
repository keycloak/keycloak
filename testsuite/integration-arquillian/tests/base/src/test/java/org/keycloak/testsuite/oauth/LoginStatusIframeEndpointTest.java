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

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Test;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginStatusIframeEndpointTest extends AbstractKeycloakTest {

    @Test
    public void checkIframeP3PHeader() throws IOException {
        CookieStore cookieStore = new BasicCookieStore();

        CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
        try {
            String redirectUri = URLEncoder.encode(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/admin/master/console", "UTF-8");

            HttpGet get = new HttpGet(
                    suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/master/protocol/openid-connect/auth?response_type=code&client_id=" + Constants.ADMIN_CONSOLE_CLIENT_ID +
                            "&redirect_uri=" + redirectUri);

            CloseableHttpResponse response = client.execute(get);
            String s = IOUtils.toString(response.getEntity().getContent());
            response.close();

            Matcher matcher = Pattern.compile("action=\"([^\"]*)\"").matcher(s);
            matcher.find();

            String action = matcher.group(1);

            HttpPost post = new HttpPost(action);

            List<NameValuePair> params = new LinkedList<>();
            params.add(new BasicNameValuePair("username", "admin"));
            params.add(new BasicNameValuePair("password", "admin"));

            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            post.setEntity(new UrlEncodedFormEntity(params));

            response = client.execute(post);

            assertEquals("CP=\"This is not a P3P policy!\"", response.getFirstHeader("P3P").getValue());

            Header setIdentityCookieHeader = null;
            Header setSessionCookieHeader = null;
            for (Header h : response.getAllHeaders()) {
                if (h.getName().equals("Set-Cookie")) {
                    if (h.getValue().contains("KEYCLOAK_SESSION")) {
                        setSessionCookieHeader = h;

                    } else if (h.getValue().contains("KEYCLOAK_IDENTITY")) {
                        setIdentityCookieHeader = h;
                    }
                }
            }
            assertNotNull(setIdentityCookieHeader);
            assertTrue(setIdentityCookieHeader.getValue().contains("HttpOnly"));

            assertNotNull(setSessionCookieHeader);
            assertFalse(setSessionCookieHeader.getValue().contains("HttpOnly"));

            response.close();

            Cookie sessionCookie = null;
            for (Cookie cookie : cookieStore.getCookies()) {
                if (cookie.getName().equals("KEYCLOAK_SESSION")) {
                    sessionCookie = cookie;
                    break;
                }
            }
            assertNotNull(sessionCookie);

            get = new HttpGet(
                    suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/master/protocol/openid-connect/login-status-iframe.html?client_id=" + Constants.ADMIN_CONSOLE_CLIENT_ID + "&origin=" + suiteContext.getAuthServerInfo().getContextRoot());
            response = client.execute(get);

            assertEquals(200, response.getStatusLine().getStatusCode());
            s = IOUtils.toString(response.getEntity().getContent());
            assertTrue(s.contains("function getCookie(cname)"));

            assertEquals("CP=\"This is not a P3P policy!\"", response.getFirstHeader("P3P").getValue());

            response.close();
        } finally {
            client.close();
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

}
