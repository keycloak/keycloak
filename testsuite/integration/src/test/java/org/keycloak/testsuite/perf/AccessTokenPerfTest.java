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
package org.keycloak.testsuite.perf;

import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.testsuite.Constants;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.OAuthClient.AccessTokenResponse;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.util.BasicAuthHelper;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccessTokenPerfTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule();

    public static class BrowserLogin implements Runnable
    {

        private WebDriver driver;

        public BrowserLogin() {
            driver = WebRule.createWebDriver();
        }

        @Override
        public void run() {
            driver.manage().deleteAllCookies();
            OAuthClient oauth = new OAuthClient(driver);
            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
            Assert.assertEquals(200, response.getStatusCode());
            count.incrementAndGet();

        }
    }

    public static AtomicLong count = new AtomicLong(0);

    public static class JaxrsClientLogin implements Runnable
    {
        ResteasyClient client;

        private String baseUrl = Constants.AUTH_SERVER_ROOT;

        private String realm = "test";

        private String responseType = OAuth2Constants.CODE;

        private String grantType = "authorization_code";

        private String clientId = "test-app";

        private String redirectUri = "http://localhost:8081/app/auth";


        public JaxrsClientLogin() {
            DefaultHttpClient httpClient = (DefaultHttpClient) new HttpClientBuilder().build();
            httpClient.setCookieStore(new CookieStore() {
                @Override
                public void addCookie(Cookie cookie) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }

                @Override
                public List<Cookie> getCookies() {
                    return Collections.emptyList();
                }

                @Override
                public boolean clearExpired(Date date) {
                    return false;  //To change body of implemented methods use File | Settings | File Templates.
                }

                @Override
                public void clear() {
                    //To change body of implemented methods use File | Settings | File Templates.
                }
            });
            ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);
            this.client = new ResteasyClientBuilder().httpEngine(engine).build();
        }

        public String getLoginFormUrl(String state) {
            UriBuilder b = OIDCLoginProtocolService.authUrl(UriBuilder.fromUri(baseUrl));
            if (responseType != null) {
                b.queryParam(OAuth2Constants.RESPONSE_TYPE, responseType);
            }
            if (clientId != null) {
                b.queryParam(OAuth2Constants.CLIENT_ID, clientId);
            }
            if (redirectUri != null) {
                b.queryParam(OAuth2Constants.REDIRECT_URI, redirectUri);
            }
            if (state != null) {
                b.queryParam(OAuth2Constants.STATE, state);
            }
            return b.build(realm).toString();
        }

        public String getProcessLoginUrl(String state) {
            UriBuilder b = LoginActionsService.processLoginUrl(UriBuilder.fromUri(baseUrl));
            if (clientId != null) {
                b.queryParam(OAuth2Constants.CLIENT_ID, clientId);
            }
            if (redirectUri != null) {
                b.queryParam(OAuth2Constants.REDIRECT_URI, redirectUri);
            }
            if (state != null) {
                b.queryParam(OAuth2Constants.STATE, state);
            }
            return b.build(realm).toString();
        }

        static Pattern actionParser = Pattern.compile("action=\"([^\"]+)\"");

        public void run() {
            //this.client = new ResteasyClientBuilder().build();
            String state = "42";
            String loginFormUrl = getLoginFormUrl(state);
            String html = client.target(loginFormUrl).request().get(String.class);
            Matcher matcher = actionParser.matcher(html);
            matcher.find();
            String actionUrl = matcher.group(1);
            if (!actionUrl.startsWith("http")) {
                actionUrl = UriBuilder.fromUri(actionUrl).scheme("http").host("localhost").port(8081).build().toString();
            }
            Form form = new Form();
            form.param("username", "test-user@localhost");
            form.param("password", "password");
            Response response = client.target(actionUrl).request().post(Entity.form(form));
            URI uri = null;
            Assert.assertEquals(302, response.getStatus());
            uri = response.getLocation();
            for (String header : response.getHeaders().keySet()) {
                for (Object value : response.getHeaders().get(header)) {
                    System.out.println(header + ": " + value);
                }
            }
            response.close();

            Assert.assertNotNull(uri);
            String code = getCode(uri);
            Assert.assertNotNull(code);

            form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, grantType)
                    .param(OAuth2Constants.CODE, code)
                    .param(OAuth2Constants.REDIRECT_URI, redirectUri);

            String authorization = BasicAuthHelper.createHeader(clientId, "password");

            String res = client.target(OIDCLoginProtocolService.tokenUrl(UriBuilder.fromUri(baseUrl)).build(realm)).request()
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .post(Entity.form(form), String.class);
            count.incrementAndGet();
            //client.close();
        }

        public String getCode(URI uri) {
            Map<String, String> m = new HashMap<String, String>();
            List<NameValuePair> pairs = URLEncodedUtils.parse(uri, "UTF-8");
            for (NameValuePair p : pairs) {
                if (p.getName().equals("code")) return p.getValue();
                m.put(p.getName(), p.getValue());
            }
            return null;
        }


        public void close()
        {
            client.close();
        }
    }

    @Test
    public void perfJaxrsClientLogin()
    {
        long ITERATIONS = 3;
        JaxrsClientLogin login = new JaxrsClientLogin();
        long start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            //System.out.println("*************************");
            login.run();
        }
        long end = System.currentTimeMillis() - start;
        System.out.println("took: " + end);
    }

    @Test
    public void perfBrowserLogin()
    {
        long ITERATIONS = 3;
        long start = System.currentTimeMillis();
        BrowserLogin login = new BrowserLogin();
        for (int i = 0; i < ITERATIONS; i++) {
            //System.out.println("----------------------------------");
            login.run();
        }
        long end = System.currentTimeMillis() - start;
        System.out.println("took: " + end);
    }

    @Test
    public void multiThread() throws Exception {
        int num_threads = 20;
        Thread[] threads = new Thread[num_threads];
        for (int i = 0; i < num_threads; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    perfJaxrsClientLogin();
                }
            });
        }
        long start = System.currentTimeMillis();
        for (int i = 0; i < num_threads; i++) {
            threads[i].start();
        }
        for (int i = 0; i < num_threads; i++) {
            threads[i].join();
        }
        long end = System.currentTimeMillis() - start;
        System.out.println(count.toString() + " took: " + end);
        System.out.println(count.floatValue() / ((float)end) * 1000+ " logins/s");
    }

}
