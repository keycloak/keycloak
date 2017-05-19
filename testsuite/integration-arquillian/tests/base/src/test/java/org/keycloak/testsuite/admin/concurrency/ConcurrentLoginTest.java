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

package org.keycloak.testsuite.admin.concurrency;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.util.OAuthClient;



/**
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class ConcurrentLoginTest extends AbstractConcurrencyTest {
    
    private static final int DEFAULT_THREADS = 10;
    private static final int DEFAULT_ITERATIONS = 20;
    private static final int CLIENTS_PER_THREAD = 10;
    private static final int DEFAULT_CLIENTS_COUNT = CLIENTS_PER_THREAD * DEFAULT_THREADS;
    
    @Before
    public void beforeTest() {
        for (int i = 0; i < DEFAULT_CLIENTS_COUNT; i++) {
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId("client" + i);
            client.setDirectAccessGrantsEnabled(true);
            client.setRedirectUris(Arrays.asList("http://localhost:8180/auth/realms/master/app/*"));
            client.setWebOrigins(Arrays.asList("http://localhost:8180"));
            client.setSecret("password");
            
            log.debug("creating " + client.getClientId());
            Response create = adminClient.realm("test").clients().create(client);
            Assert.assertEquals(Response.Status.CREATED, create.getStatusInfo());
            create.close();
        }
        log.debug("clients created");
    }
    
    @Override
    protected void run(final KeycloakRunnable runnable) throws Throwable {
        run(runnable, DEFAULT_THREADS, DEFAULT_ITERATIONS);
    }
    
    @Test
    public void concurrentLogin() throws Throwable {
        System.out.println("*********************************************");
        long start = System.currentTimeMillis();

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build()) {
            
            HttpUriRequest request = handleLogin(getPageContent(oauth.getLoginFormUrl(), httpClient, null), "test-user@localhost", "password");
            
            log.debug("Executing login request");
            
            Assert.assertTrue(parseAndCloseResponse(httpClient.execute(request)).contains("<title>AUTH_RESPONSE</title>"));

            run(new KeycloakRunnable() {
                @Override
                public void run(Keycloak keycloak, RealmResource realm, int threadNum, int iterationNum) {
                    OAuthClient oauth = new OAuthClient();
                    oauth.init(adminClient, driver);
                    
                    int startIndex = CLIENTS_PER_THREAD * threadNum;
                    for (int i = startIndex; i < startIndex + CLIENTS_PER_THREAD; i++) {
                        oauth.clientId("client" + i);
                        log.trace("Accessing login page for " + oauth.getClientId() + " threat " + threadNum + " iteration " + iterationNum);
                        try {
                            final HttpClientContext context = HttpClientContext.create();
                            
                            String pageContent = getPageContent(oauth.getLoginFormUrl(), httpClient, context);
                            String currentUrl = context.getRedirectLocations().get(0).toString();
                            
                            Assert.assertTrue(pageContent.contains("<title>AUTH_RESPONSE</title>"));
                            
                            String code = getQueryFromUrl(currentUrl).get(OAuth2Constants.CODE);
                            OAuthClient.AccessTokenResponse accessRes = oauth.doAccessTokenRequest(code, "password");
                            Assert.assertEquals("AccessTokenResponse: error: '" + accessRes.getError() + "' desc: '" + accessRes.getErrorDescription() + "'",
                                    200, accessRes.getStatusCode());
                            
                            OAuthClient.AccessTokenResponse refreshRes = oauth.doRefreshTokenRequest(accessRes.getRefreshToken(), "password");
                            Assert.assertEquals("AccessTokenResponse: error: '" + refreshRes.getError() + "' desc: '" + refreshRes.getErrorDescription() + "'",
                                    200, refreshRes.getStatusCode());
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            });
        } 

        long end = System.currentTimeMillis() - start;
        System.out.println("concurrentLogin took " + (end/1000) + "s");
        System.out.println("*********************************************");
    }
    
    private String getPageContent(String url, CloseableHttpClient httpClient, HttpClientContext context) throws Exception {

        HttpGet request = new HttpGet(url);

        request.setHeader("User-Agent", "Mozilla/5.0");
        request.setHeader("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setHeader("Accept-Language", "en-US,en;q=0.5");

        if (context != null) {
            return parseAndCloseResponse(httpClient.execute(request, context));
        } else {
            return parseAndCloseResponse(httpClient.execute(request));
        }

    }

    private String parseAndCloseResponse(CloseableHttpResponse response) throws UnsupportedOperationException, IOException {
        try {
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode != 200) {
                log.debug("Response Code : " + responseCode);
            }
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            if (responseCode != 200) {
                log.debug(result.toString());
            }
            return result.toString();
        } catch (IOException | UnsupportedOperationException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
                try {
                    response.close();
                } catch (IOException ex) { }
            }
        }
    }
    
    private HttpUriRequest handleLogin(String html, String username, String password) throws UnsupportedEncodingException {

        System.out.println("Extracting form's data...");

        // Keycloak form id
        Element loginform = Jsoup.parse(html).getElementById("kc-form-login");
        String method = loginform.attr("method");
        String action = loginform.attr("action");
        
        List<NameValuePair> paramList = new ArrayList<>();

        for (Element inputElement : loginform.getElementsByTag("input")) {
            String key = inputElement.attr("name");

            if (key.equals("username")) {
                paramList.add(new BasicNameValuePair(key, username));
            } else if (key.equals("password")) {
                paramList.add(new BasicNameValuePair(key, password));
            }
        }
        
        boolean isPost = method != null && "post".equalsIgnoreCase(method);
        
        if (isPost) {
            HttpPost req = new HttpPost(action);

            UrlEncodedFormEntity formEntity;
            try {
                formEntity = new UrlEncodedFormEntity(paramList, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            req.setEntity(formEntity);

            return req;
        } else {
            throw new UnsupportedOperationException("not supported yet!");
        }
    }
    
    private Map<String, String> getQueryFromUrl(String url) throws URISyntaxException {
        Map<String, String> m = new HashMap<>();
        List<NameValuePair> pairs = URLEncodedUtils.parse(new URI(url), "UTF-8");
        for (NameValuePair p : pairs) {
            m.put(p.getName(), p.getValue());
        }
        return m;
    }

    
}