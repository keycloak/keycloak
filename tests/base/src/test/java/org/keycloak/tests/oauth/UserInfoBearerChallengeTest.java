/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.oauth;

import java.io.IOException;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;

import org.junit.jupiter.api.Test;

import static jakarta.ws.rs.core.HttpHeaders.WWW_AUTHENTICATE;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest
public class UserInfoBearerChallengeTest {

    private static final String USERNAME = "test-user";
    private static final String PASSWORD = "password";

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @InjectRealm(config = UserInfoRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @Test
    public void noToken() throws IOException {
        try (SimpleHttpResponse response = userInfoGet(null)) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE), is("Bearer realm=\"" + realm.getName() + "\""));
            assertThat(response.asString(), is(""));
        }
    }

    @Test
    public void expiredToken() throws IOException {
        String token = oauth.doPasswordGrantRequest(USERNAME, PASSWORD).getAccessToken();
        timeOffSet.set(600);

        try (SimpleHttpResponse response = userInfoGet(token)) {
            assertThat(response.getStatus(), is(401));
            // UserInfo uses a generic description for all token failures.
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE),
                    is("Bearer realm=\"" + realm.getName() + "\", error=\"invalid_token\""
                            + ", error_description=\"Token verification failed\""));
            assertThat(response.asString(), is(""));
        }
    }

    private SimpleHttpResponse userInfoGet(String token) throws IOException {
        String url = realm.getBaseUrl() + "/protocol/openid-connect/userinfo";
        return token != null
                ? simpleHttp.doGet(url).auth(token).asResponse()
                : simpleHttp.doGet(url).asResponse();
    }

    public static class UserInfoRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.users(UserBuilder.create()
                    .username(USERNAME)
                    .password(PASSWORD)
                    .name("Test", "User")
                    .email("test-user@localhost")
                    .emailVerified(true));
        }
    }
}
