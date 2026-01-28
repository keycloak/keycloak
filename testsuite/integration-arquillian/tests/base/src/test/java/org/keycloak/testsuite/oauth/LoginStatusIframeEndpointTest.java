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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.ActionURIUtils;
import org.keycloak.testsuite.runonserver.ServerVersion;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.oauth.PkceGenerator;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginStatusIframeEndpointTest extends AbstractKeycloakTest {

    @Test
    public void checkIframe() throws IOException {
        CookieStore cookieStore = new BasicCookieStore();

        try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(cookieStore).build()) {
            String redirectUri = URLEncoder.encode(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/admin/master/console", StandardCharsets.UTF_8);

            PkceGenerator pkce = PkceGenerator.s256();

            HttpGet get = new HttpGet(
                    suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/master/protocol/openid-connect/auth?response_type=code&client_id=" + Constants.ADMIN_CONSOLE_CLIENT_ID +
                            "&redirect_uri=" + redirectUri + "&scope=openid&code_challenge_method=S256&code_challenge=" + pkce.getCodeChallenge());

            CloseableHttpResponse response = client.execute(get);
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            response.close();

            String action = ActionURIUtils.getActionURIFromPageSource(s);

            HttpPost post = new HttpPost(action);

            List<NameValuePair> params = new LinkedList<>();
            params.add(new BasicNameValuePair("username", "admin"));
            params.add(new BasicNameValuePair("password", "admin"));

            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            post.setEntity(new UrlEncodedFormEntity(params));

            response = client.execute(post);

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

            get = new HttpGet(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/master/protocol/openid-connect/login-status-iframe.html");
            response = client.execute(get);

            assertEquals(200, response.getStatusLine().getStatusCode());
            assertNull(response.getFirstHeader(BrowserSecurityHeaders.X_FRAME_OPTIONS.getHeaderName()));
            assertEquals("frame-src 'self'; object-src 'none';", response.getFirstHeader(BrowserSecurityHeaders.CONTENT_SECURITY_POLICY.getHeaderName()).getValue());
            assertEquals("none", response.getFirstHeader(BrowserSecurityHeaders.X_ROBOTS_TAG.getHeaderName()).getValue());

            response.close();

            get = new HttpGet(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/master/protocol/openid-connect/login-status-iframe.html/init");
            response = client.execute(get);
            assertEquals(403, response.getStatusLine().getStatusCode());
            response.close();

            get = new HttpGet(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/master/protocol/openid-connect/login-status-iframe.html/init?"
                + "client_id=invalid"
                + "&origin=" + suiteContext.getAuthServerInfo().getContextRoot()
            );
            response = client.execute(get);
            assertEquals(403, response.getStatusLine().getStatusCode());
            response.close();

            get = new HttpGet(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/master/protocol/openid-connect/login-status-iframe.html/init?"
                + "client_id=" + Constants.ADMIN_CONSOLE_CLIENT_ID
                + "&origin=http://invalid"
            );
            response = client.execute(get);
            assertEquals(403, response.getStatusLine().getStatusCode());
            response.close();

            get = new HttpGet(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/master/protocol/openid-connect/login-status-iframe.html/init?"
                + "client_id=" + Constants.ADMIN_CONSOLE_CLIENT_ID
                + "&origin=" + suiteContext.getAuthServerInfo().getContextRoot()
            );
            response = client.execute(get);
            assertEquals(204, response.getStatusLine().getStatusCode());
            response.close();
        }
    }

    @Test
    public void checkIframeWildcardOrigin() throws IOException {
        String id = adminClient.realm("master").clients().findByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID).get(0).getId();
        ClientResource master = adminClient.realm("master").clients().get(id);
        ClientRepresentation rep = master.toRepresentation();
        List<String> org = rep.getWebOrigins();
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            rep.setWebOrigins(Collections.singletonList("*"));
            master.update(rep);

            HttpGet get = new HttpGet(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/master/protocol/openid-connect/login-status-iframe.html/init?"
                    + "client_id=" + Constants.ADMIN_CONSOLE_CLIENT_ID
                    + "&origin=" + "http://anything"
            );
            try (CloseableHttpResponse response = client.execute(get)) {
                assertEquals(204, response.getStatusLine().getStatusCode());
            }
        } finally {
            rep.setWebOrigins(org);
            master.update(rep);
        }
    }

    @Test
    public void checkIframeCache() throws IOException {
        String version = testingClient.server().fetch(new ServerVersion());

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/master/protocol/openid-connect/login-status-iframe.html");
            CloseableHttpResponse response = client.execute(get);

            assertEquals(200, response.getStatusLine().getStatusCode());
            assertEquals("no-cache, must-revalidate, no-transform, no-store", response.getHeaders("Cache-Control")[0].getValue());

            get = new HttpGet(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/master/protocol/openid-connect/login-status-iframe.html?version=" + version);
            response = client.execute(get);

            assertEquals(200, response.getStatusLine().getStatusCode());
            assertTrue(response.getHeaders("Cache-Control")[0].getValue().contains("max-age"));
        }
    }

    @Test
    public void checkEmptyCsp() throws Exception {
        try (RealmAttributeUpdater realmUpdater = new RealmAttributeUpdater(adminClient.realm("test"))
                .setBrowserSecurityHeader(BrowserSecurityHeaders.CONTENT_SECURITY_POLICY.getKey(), "")
                .update();
                Client client = AdminClientUtil.createResteasyClient();
                Response response = client.target(suiteContext.getAuthServerInfo().getContextRoot()
                        + "/auth/realms/test/protocol/openid-connect/login-status-iframe.html").request().get()) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertNull(response.getHeaderString(BrowserSecurityHeaders.CONTENT_SECURITY_POLICY.getKey()));
            assertNull(response.getHeaderString(BrowserSecurityHeaders.X_FRAME_OPTIONS.getHeaderName()));
        }
    }

    @Test
    public void checkCspWithNewline() throws Exception {
        try {
            new RealmAttributeUpdater(adminClient.realm("test"))
                    .setBrowserSecurityHeader(BrowserSecurityHeaders.CONTENT_SECURITY_POLICY.getKey(), "test\ntest")
                    .update();
            fail("Validation should fail due to newline");
        }
        catch (BadRequestException ex) {
            ErrorRepresentation errorRep = ex.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Newline not allowed.", errorRep.getErrorMessage());
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create().name("test").build());
    }

}
