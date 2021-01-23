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
package org.keycloak.testsuite.account;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.common.enums.AccountRestApiVersion;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.TokenUtil;
import org.keycloak.testsuite.util.WebDriverLogDumper;
import org.openqa.selenium.JavascriptExecutor;

import java.io.IOException;

import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class AccountRestServiceCorsTest extends AbstractTestRealmKeycloakTest {

    private static final String VALID_CORS_URL = "http://localtest.me:8180/auth";
    private static final String INVALID_CORS_URL = "http://invalid.localtest.me:8180/auth";

    @Rule
    public TokenUtil tokenUtil = new TokenUtil();

    private CloseableHttpClient client;
    private JavascriptExecutor executor;

    @Before
    public void before() {
        client = HttpClientBuilder.create().build();
        oauth.clientId("direct-grant");
        executor = (JavascriptExecutor) driver;
    }

    @After
    public void after() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.setEditUsernameAllowed(false);
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Test
    public void testGetProfile() throws IOException, InterruptedException {
        driver.navigate().to(VALID_CORS_URL);

        doXhr(executor, getAccountUrl(), tokenUtil.getToken(), null, true);
    }

    @Test
    public void testGetProfileInvalidOrigin() throws IOException, InterruptedException {
        driver.navigate().to(INVALID_CORS_URL);

        doXhr(executor, getAccountUrl(), tokenUtil.getToken(), null, false);
    }

    @Test
    public void testUpdateProfile() throws IOException {
        driver.navigate().to(VALID_CORS_URL);

        doXhr(executor, getAccountUrl(), tokenUtil.getToken(), "{ \"firstName\" : \"Bob\" }", true);
    }

    @Test
    public void testUpdateProfileInvalidOrigin() throws IOException {
        driver.navigate().to(INVALID_CORS_URL);

        doXhr(executor, getAccountUrl(), tokenUtil.getToken(), "{ \"firstName\" : \"Bob\" }", false);
    }

    @Test
    public void testErrorResponse() {
        driver.navigate().to(VALID_CORS_URL);

        Result result = doXhr(executor, getAccountUrl(), tokenUtil.getToken(), "{ \"username\" : \"vmuzikar\" }", true);
        assertEquals(400, result.getStatus());
        assertThat(result.getResult(), containsString("readOnlyUsernameMessage"));
    }

    @Test
    public void testErrorResponseInvalidOrigin() {
        driver.navigate().to(INVALID_CORS_URL);

        doXhr(executor, getAccountUrl(), tokenUtil.getToken(), "{ \"username\" : \"vmuzikar\" }", false);
    }

    @Test
    public void testGetVersionedApi() {
        driver.navigate().to(VALID_CORS_URL);

        doXhr(executor, getAccountUrl() + "/" + AccountRestApiVersion.DEFAULT.getStrVersion(), tokenUtil.getToken(), null, true);
    }

    @Test
    public void testGetVersionedApiInvalidOrigin() {
        driver.navigate().to(INVALID_CORS_URL);

        doXhr(executor, getAccountUrl() + "/" + AccountRestApiVersion.DEFAULT.getStrVersion(), tokenUtil.getToken(), null, false);
    }

    private String getAccountUrl() {
        return suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/test/account";
    }

    private Result doXhr(JavascriptExecutor executor, String url, String token, String postData, boolean expectAllowed) {
        String js = "var r = new XMLHttpRequest();" +
                "r.open('" + (postData == null ? "GET" : "POST") + "', '" + url + "', false);" +
                "r.setRequestHeader('Accept','application/json');" +
                "r.setRequestHeader('Content-Type','application/json');" +
                "r.setRequestHeader('Authorization','bearer " + token + "');" +
                "r.send(" + (postData == null ? "" : "'" + postData + "'") + ");" +
                "return r.status + ':::' + r.responseText";

        Result result = null;
        Throwable error = null;
        try {
            String response = (String) executor.executeScript(js);
            String[] r = response.split(":::");
            result = new Result(Integer.parseInt(r[0]), r.length == 2 ? r[1] : null);
        } catch (Throwable t ) {
            error = t;
        }

        if (error != null) {
            if (expectAllowed) {
                throw new AssertionError("Cors request failed: " + WebDriverLogDumper.dumpBrowserLogs(driver));
            } else {
                return result;
            }
        } else {
            if (!expectAllowed) {
                throw new AssertionError("Expected CORS request to be rejected, but was successful");
            } else {
                return result;
            }
        }
    }

    private static class Result {
        int status;

        String result;

        public Result(int status, String result) {
            this.status = status;
            this.result = result;
        }

        public int getStatus() {
            return status;
        }

        public String getResult() {
            return result;
        }
    }

}
