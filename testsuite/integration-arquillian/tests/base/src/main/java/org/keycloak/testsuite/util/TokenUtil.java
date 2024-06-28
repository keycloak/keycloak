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
package org.keycloak.testsuite.util;

import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;
import org.keycloak.common.util.Time;

import static org.junit.Assert.fail;

/**
 * Created by st on 22/03/17.
 */
public class TokenUtil implements TestRule {

    private final String username;
    private final String password;
    private OAuthClient oauth;

    private String refreshToken;
    private String token;
    private int expires;

    public TokenUtil() {
        this("test-user@localhost", "password");
    }

    public TokenUtil(String username, String password) {
        this.username = username;
        this.password = password;
        this.oauth = new OAuthClient();
        this.oauth.init(null);
        this.oauth.clientId("direct-grant");
    }

    @Override
    public Statement apply(final Statement base, org.junit.runner.Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                base.evaluate();
            }
        };
    }

    public String getToken() {
        if (refreshToken == null) {
            load();
        } else if (expires < Time.currentTime()) {
            refresh();
        }
        return token;
    }

    private void load() {
        try {
            OAuthClient.AccessTokenResponse accessTokenResponse = oauth.doGrantAccessTokenRequest("password", username, password);
            if (accessTokenResponse.getStatusCode() != 200) {
                fail("Failed to get token: " + accessTokenResponse.getErrorDescription());
            }

            this.refreshToken = accessTokenResponse.getRefreshToken();
            this.token = accessTokenResponse.getAccessToken();

            expires = Time.currentTime() + accessTokenResponse.getExpiresIn() - 20;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void refresh() {
        try {
            OAuthClient.AccessTokenResponse accessTokenResponse = oauth.doRefreshTokenRequest(refreshToken, "password");
            if (accessTokenResponse.getStatusCode() != 200) {
                fail("Failed to get token: " + accessTokenResponse.getErrorDescription());
            }

            this.refreshToken = accessTokenResponse.getRefreshToken();
            this.token = accessTokenResponse.getAccessToken();

            expires = Time.currentTime() + accessTokenResponse.getExpiresIn() - 20;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
