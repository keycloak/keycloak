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
package org.keycloak.tests.account;

import org.keycloak.common.enums.AccountRestApiVersion;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.utils.LegacyRealmConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class AccountRestServiceCorsTest {

    @InjectRealm(config = AccountRestServiceCorsRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    private static final String VALID_CORS_URL = "http://localtest.me:8180/auth";
    private static final String INVALID_CORS_URL = "http://invalid.localtest.me:8180/auth";

    private JavascriptExecutor executor;

    @BeforeEach
    public void before() {
        oauth.client("direct-grant", "password");
        executor = (JavascriptExecutor) driver.driver();
    }

    @Test
    public void testGetProfile() {
        driver.driver().navigate().to(VALID_CORS_URL);

        doXhr(executor, getAccountUrl(), getToken(), null, true);
    }

    @Test
    public void testGetProfileInvalidOrigin() {
        driver.driver().navigate().to(INVALID_CORS_URL);

        doXhr(executor, getAccountUrl(), getToken(), null, false);
    }

    @Test
    public void testUpdateProfile() {
        driver.driver().navigate().to(VALID_CORS_URL);

        doXhr(executor, getAccountUrl(), getToken(), "{ \"firstName\" : \"Bob\" }", true);
    }

    @Test
    public void testUpdateProfileInvalidOrigin() {
        driver.driver().navigate().to(INVALID_CORS_URL);

        doXhr(executor, getAccountUrl(), getToken(), "{ \"firstName\" : \"Bob\" }", false);
    }

    @Test
    public void testErrorResponse() {
        driver.driver().navigate().to(VALID_CORS_URL);

        Result result = doXhr(executor, getAccountUrl(), getToken(), "{ \"username\" : \"vmuzikar\" }", true);
        assertEquals(400, result.getStatus());
        assertThat(result.getResult(), containsString("readOnlyUsernameMessage"));
    }

    @Test
    public void testErrorResponseInvalidOrigin() {
        driver.driver().navigate().to(INVALID_CORS_URL);

        doXhr(executor, getAccountUrl(), getToken(), "{ \"username\" : \"vmuzikar\" }", false);
    }

    @Test
    public void testGetVersionedApi() {
        driver.driver().navigate().to(VALID_CORS_URL);

        doXhr(executor, getAccountUrl() + "/" + AccountRestApiVersion.DEFAULT.getStrVersion(), getToken(), null, true);
    }

    @Test
    public void testGetVersionedApiInvalidOrigin() {
        driver.driver().navigate().to(INVALID_CORS_URL);

        doXhr(executor, getAccountUrl() + "/" + AccountRestApiVersion.DEFAULT.getStrVersion(), getToken(), null, false);
    }

    private String getAccountUrl() {
        return managedRealm.getBaseUrl() + "/account";
    }

    private String getToken() {
        return oauth.doPasswordGrantRequest("test-user@localhost", "password").getAccessToken();
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
                throw new AssertionError("Cors request failed", error);
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


    private static class AccountRestServiceCorsRealmConfig extends LegacyRealmConfig {

        @Override
        public void configureTestRealm(RealmRepresentation testRealm) {
            testRealm.setEditUsernameAllowed(false);
        }
    }
}
