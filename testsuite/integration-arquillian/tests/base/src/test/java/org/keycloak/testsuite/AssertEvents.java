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

package org.keycloak.testsuite;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.common.util.PemUtils;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import java.io.IOException;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AssertEvents {

    public static final String DEFAULT_CLIENT_ID = "test-app";
    public static final String DEFAULT_IP_ADDRESS = "127.0.0.1";
    public static final String DEFAULT_REALM = "test";
    public static final String DEFAULT_USERNAME = "test-user@localhost";

    String defaultRedirectUri = "http://localhost:8180/auth/realms/master/app/auth";
    String defaultEventsQueueUri = "http://localhost:8092";

    private RealmResource realmResource;
    private RealmRepresentation realmRep;
    private AbstractKeycloakTest context;
    private PublicKey realmPublicKey;
    private UserRepresentation defaultUser;

    public AssertEvents(AbstractKeycloakTest ctx) throws Exception {
        context = ctx;

        realmResource = context.adminClient.realms().realm(DEFAULT_REALM);
        realmRep = realmResource.toRepresentation();
        String pubKeyString = realmRep.getPublicKey();
        realmPublicKey = PemUtils.decodePublicKey(pubKeyString);

        defaultUser = getUser(DEFAULT_USERNAME);
        if (defaultUser == null) {
            throw new RuntimeException("Default user does not exist: " + DEFAULT_USERNAME + ". Make sure to add it to your test realm.");
        }

        defaultEventsQueueUri = getAuthServerEventsQueueUri();
    }

    String getAuthServerEventsQueueUri() {
        int httpPort = Integer.parseInt(System.getProperty("auth.server.event.http.port", "8089"));
        int portOffset = Integer.parseInt(System.getProperty("auth.server.port.offset", "0"));
        return "http://localhost:" + (httpPort + portOffset);
    }

    public EventRepresentation poll() {
        EventRepresentation event = fetchNextEvent();
        Assert.assertNotNull("Event expected", event);

        return event;
    }

    public void clear() {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpPost post = new HttpPost(defaultEventsQueueUri + "/clear-event-queue");
            CloseableHttpResponse response = httpclient.execute(post);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed to clear events from " + post.getURI() + ": " + response.getStatusLine().toString());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ExpectedEvent expectRequiredAction(EventType event) {
        return expectLogin().event(event).removeDetail(Details.CONSENT).session(isUUID());
    }

    public ExpectedEvent expectLogin() {
        return expect(EventType.LOGIN)
                .detail(Details.CODE_ID, isCodeId())
                //.detail(Details.USERNAME, DEFAULT_USERNAME)
                //.detail(Details.AUTH_METHOD, OIDCLoginProtocol.LOGIN_PROTOCOL)
                //.detail(Details.AUTH_TYPE, AuthorizationEndpoint.CODE_AUTH_TYPE)
                .detail(Details.REDIRECT_URI, defaultRedirectUri)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_NO_CONSENT_REQUIRED)
                .session(isUUID());
    }

    public ExpectedEvent expectClientLogin() {
        return expect(EventType.CLIENT_LOGIN)
                .detail(Details.CODE_ID, isCodeId())
                .detail(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID)
                .detail(Details.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS)
                .removeDetail(Details.CODE_ID)
                .session(isUUID());
    }

    public ExpectedEvent expectSocialLogin() {
        return expect(EventType.LOGIN)
                .detail(Details.CODE_ID, isCodeId())
                .detail(Details.USERNAME, DEFAULT_USERNAME)
                .detail(Details.AUTH_METHOD, "form")
                .detail(Details.REDIRECT_URI, defaultRedirectUri)
                .session(isUUID());
    }

    public ExpectedEvent expectCodeToToken(String codeId, String sessionId) {
        return expect(EventType.CODE_TO_TOKEN)
                .detail(Details.CODE_ID, codeId)
                .detail(Details.TOKEN_ID, isUUID())
                .detail(Details.REFRESH_TOKEN_ID, isUUID())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .detail(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID)
                .session(sessionId);
    }

    public ExpectedEvent expectRefresh(String refreshTokenId, String sessionId) {
        return expect(EventType.REFRESH_TOKEN)
                .detail(Details.TOKEN_ID, isUUID())
                .detail(Details.REFRESH_TOKEN_ID, refreshTokenId)
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .detail(Details.UPDATED_REFRESH_TOKEN_ID, isUUID())
                .detail(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID)
                .session(sessionId);
    }

    public ExpectedEvent expectLogout(String sessionId) {
        return expect(EventType.LOGOUT).client((String) null)
                .detail(Details.REDIRECT_URI, defaultRedirectUri)
                .session(sessionId);
    }

    public ExpectedEvent expectRegister(String username, String email) {
        UserRepresentation user = username != null ? getUser(username) : null;
        return expect(EventType.REGISTER)
                .user(user != null ? user.getId() : null)
                .detail(Details.USERNAME, username)
                .detail(Details.EMAIL, email)
                .detail(Details.REGISTER_METHOD, "form")
                .detail(Details.REDIRECT_URI, defaultRedirectUri);
    }

    public ExpectedEvent expectAccount(EventType event) {
        return expect(event).client("account");
    }

    public ExpectedEvent expect(EventType event) {
        return new ExpectedEvent()
                .realm(realmRep.getId())
                .client(DEFAULT_CLIENT_ID)
                .user(defaultUser.getId())
                .ipAddress(DEFAULT_IP_ADDRESS)
                .session((String) null)
                .event(event);
    }

    UserRepresentation getUser(String username) {
        List<UserRepresentation> result = realmResource.users().search(username, null, null, null, 0, 1);
        return result.size() > 0 ? result.get(0) : null;
    }

    public PublicKey getRealmPublicKey() {
        return realmPublicKey;
    }

    public class ExpectedEvent {
        private EventRepresentation expected = new EventRepresentation();
        private Matcher<String> userId;
        private Matcher<String> sessionId;
        private HashMap<String, Matcher<String>> details;

        public ExpectedEvent realm(RealmRepresentation realm) {
            expected.setRealmId(realm.getId());
            return this;
        }

        public ExpectedEvent realm(String realmId) {
            expected.setRealmId(realmId);
            return this;
        }

        public ExpectedEvent client(ClientRepresentation client) {
            expected.setClientId(client.getClientId());
            return this;
        }

        public ExpectedEvent client(String clientId) {
            expected.setClientId(clientId);
            return this;
        }

        public ExpectedEvent user(UserRepresentation user) {
            return user(user.getId());
        }

        public ExpectedEvent user(String userId) {
            return user(CoreMatchers.equalTo(userId));
        }

        public ExpectedEvent user(Matcher<String> userId) {
            this.userId = userId;
            return this;
        }

        public ExpectedEvent session(UserSessionRepresentation session) {
            return session(session.getId());
        }

        public ExpectedEvent session(String sessionId) {
            return session(CoreMatchers.equalTo(sessionId));
        }

        public ExpectedEvent session(Matcher<String> sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public ExpectedEvent ipAddress(String ipAddress) {
            expected.setIpAddress(ipAddress);
            return this;
        }

        public ExpectedEvent event(EventType e) {
            expected.setType(e.name());
            return this;
        }

        public ExpectedEvent detail(String key, String value) {
            return detail(key, CoreMatchers.equalTo(value));
        }

        public ExpectedEvent detail(String key, Matcher<String> matcher) {
            if (details == null) {
                details = new HashMap<String, Matcher<String>>();
            }
            details.put(key, matcher);
            return this;
        }

        public ExpectedEvent removeDetail(String key) {
            if (details != null) {
                details.remove(key);
            }
            return this;
        }

        public ExpectedEvent clearDetails() {
            if (details != null) details.clear();
            return this;
        }

        public ExpectedEvent error(String error) {
            expected.setError(error);
            return this;
        }

        public EventRepresentation assertEvent() {
            return assertEvent(poll());
        }

        public EventRepresentation assertEvent(EventRepresentation actual) {
            if (expected.getError() != null && !expected.getType().toString().endsWith("_ERROR")) {
                expected.setType(expected.getType() + "_ERROR");
            }
            Assert.assertEquals(expected.getType(), actual.getType());
            Assert.assertEquals(expected.getRealmId(), actual.getRealmId());
            Assert.assertEquals(expected.getClientId(), actual.getClientId());
            Assert.assertEquals(expected.getError(), actual.getError());
            Assert.assertEquals(expected.getIpAddress(), actual.getIpAddress());
            Assert.assertThat(actual.getUserId(), userId);
            Assert.assertThat(actual.getSessionId(), sessionId);

            if (details == null || details.isEmpty()) {
//                Assert.assertNull(actual.getDetails());
            } else {
                Assert.assertNotNull(actual.getDetails());
                for (Map.Entry<String, Matcher<String>> d : details.entrySet()) {
                    String actualValue = actual.getDetails().get(d.getKey());
                    if (!actual.getDetails().containsKey(d.getKey())) {
                        Assert.fail(d.getKey() + " missing");
                    }

                    Assert.assertThat("Unexpected value for " + d.getKey(), actualValue, d.getValue());
                }
                /*
                for (String k : actual.getDetails().keySet()) {
                    if (!details.containsKey(k)) {
                        Assert.fail(k + " was not expected");
                    }
                }
                */
            }

            return actual;
        }
    }

    public static Matcher<String> isCodeId() {
        return isUUID();
    }

    public static Matcher<String> isUUID() {
        return new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String item) {
                return 36 == item.length() && item.charAt(8) == '-' && item.charAt(13) == '-' && item.charAt(18) == '-' && item.charAt(23) == '-';
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Not an UUID");
            }
        };
    }

    private EventRepresentation fetchNextEvent() {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpPost post = new HttpPost(defaultEventsQueueUri + "/event-queue");
            CloseableHttpResponse response = httpclient.execute(post);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed to retrieve event from " + post.getURI() + ": " + response.getStatusLine().toString() + " / " + IOUtils.toString(response.getEntity().getContent()));
            }

            return JsonSerialization.readValue(response.getEntity().getContent(), EventRepresentation.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
