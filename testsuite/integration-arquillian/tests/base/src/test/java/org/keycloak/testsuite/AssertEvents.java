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

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.util.TokenUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.hamcrest.Matchers.is;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AssertEvents implements TestRule {

    public static final String DEFAULT_CLIENT_ID = "test-app";
    public static final String DEFAULT_IP_ADDRESS = "127.0.0.1";
    public static final String DEFAULT_IP_ADDRESS_V6 = "0:0:0:0:0:0:0:1";
    public static final String DEFAULT_IP_ADDRESS_V6_SHORT = "::1";
    public static final String DEFAULT_REALM = "test";
    public static final String DEFAULT_USERNAME = "test-user@localhost";

    public static final String DEFAULT_REDIRECT_URI = getAuthServerContextRoot() + "/auth/realms/master/app/auth";

    private AbstractKeycloakTest context;

    public AssertEvents(AbstractKeycloakTest ctx) {
        context = ctx;
    }

    @Override
    public Statement apply(final Statement base, org.junit.runner.Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                // TODO: Ideally clear the queue just before testClass rather then before each method
                clear();
                base.evaluate();
                // TODO Test should fail if there are leftover events
            }
        };
    }

    public EventRepresentation poll() {
        EventRepresentation event = fetchNextEvent();
        Assert.assertNotNull("Event expected", event);

        return event;
    }

    public void assertEmpty() {
        EventRepresentation event = fetchNextEvent();
        Assert.assertNull("Empty event queue expected, but there is " + event, event);
    }

    public void clear() {
        context.getTestingClient().testing().clearEventQueue();
    }

    public ExpectedEvent expectRequiredAction(EventType event) {
        return expectLogin().event(event).removeDetail(Details.CONSENT).session(Matchers.isEmptyOrNullString());
    }

    public ExpectedEvent expectLogin() {
        return expect(EventType.LOGIN)
                .detail(Details.CODE_ID, isCodeId())
                //.detail(Details.USERNAME, DEFAULT_USERNAME)
                //.detail(Details.AUTH_METHOD, OIDCLoginProtocol.LOGIN_PROTOCOL)
                //.detail(Details.AUTH_TYPE, AuthorizationEndpoint.CODE_AUTH_TYPE)
                .detail(Details.REDIRECT_URI, Matchers.equalTo(DEFAULT_REDIRECT_URI))
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
                .detail(Details.REDIRECT_URI, Matchers.equalTo(DEFAULT_REDIRECT_URI))
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

    public ExpectedEvent expectDeviceVerifyUserCode(String clientId) {
        return expect(EventType.OAUTH2_DEVICE_VERIFY_USER_CODE)
                .user((String) null)
                .client(clientId)
                .detail(Details.CODE_ID, isUUID());
    }

    public ExpectedEvent expectDeviceLogin(String clientId, String codeId, String userId) {
        return expect(EventType.LOGIN)
                .user(userId)
                .client(clientId)
                .detail(Details.CODE_ID, codeId)
                .session(codeId);
//                .session((String) null);
    }

    public ExpectedEvent expectDeviceCodeToToken(String clientId, String codeId, String userId) {
        return expect(EventType.OAUTH2_DEVICE_CODE_TO_TOKEN)
                .client(clientId)
                .user(userId)
                .detail(Details.CODE_ID, codeId)
                .detail(Details.TOKEN_ID, isUUID())
                .detail(Details.REFRESH_TOKEN_ID, isUUID())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .detail(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID)
                .session(codeId);
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
                .detail(Details.REDIRECT_URI, Matchers.equalTo(DEFAULT_REDIRECT_URI))
                .session(sessionId);
    }

    public ExpectedEvent expectLogoutError(String error) {
        return expect(EventType.LOGOUT_ERROR)
                .error(error)
                .client((String) null)
                .user((String) null);
    }

    public ExpectedEvent expectRegister(String username, String email) {
        return expectRegister(username, email, DEFAULT_CLIENT_ID);
    }
    
    public ExpectedEvent expectRegister(String username, String email, String clientId) {
        UserRepresentation user = username != null ? getUser(username) : null;
        return expect(EventType.REGISTER)
                .user(user != null ? user.getId() : null)
                .client(clientId)
                .detail(Details.USERNAME, username)
                .detail(Details.EMAIL, email)
                .detail(Details.REGISTER_METHOD, "form")
                .detail(Details.REDIRECT_URI, Matchers.equalTo(DEFAULT_REDIRECT_URI));
    }

    public ExpectedEvent expectRegisterError(String username, String email) {
        UserRepresentation user = username != null ? getUser(username) : null;
        return expect(EventType.REGISTER_ERROR)
                .user(user != null ? user.getId() : null)
                .detail(Details.USERNAME, username)
                .detail(Details.EMAIL, email)
                .detail(Details.REGISTER_METHOD, "form")
                .detail(Details.REDIRECT_URI, Matchers.equalTo(DEFAULT_REDIRECT_URI));
    }

    public ExpectedEvent expectAccount(EventType event) {
        return expect(event).client("account");
    }

    public ExpectedEvent expectAuthReqIdToToken(String codeId, String sessionId) {
        return expect(EventType.AUTHREQID_TO_TOKEN)
                .detail(Details.CODE_ID, codeId)
                .detail(Details.TOKEN_ID, isUUID())
                .detail(Details.REFRESH_TOKEN_ID, isUUID())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .detail(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID)
                .session(isUUID());
    }

    public ExpectedEvent expect(EventType event) {
        return new ExpectedEvent()
                .realm(defaultRealmId())
                .client(DEFAULT_CLIENT_ID)
                .user(defaultUserId())
                .ipAddress(
                        System.getProperty("auth.server.host", "localhost").contains("localhost")
                        ? Matchers.anyOf(is(DEFAULT_IP_ADDRESS), is(DEFAULT_IP_ADDRESS_V6), is(DEFAULT_IP_ADDRESS_V6_SHORT))
                        : Matchers.any(String.class))
                .session((String) null)
                .event(event);
    }

    public class ExpectedEvent {
        private EventRepresentation expected = new EventRepresentation();
        private Matcher<String> realmId;
        private Matcher<String> userId;
        private Matcher<String> sessionId;
        private Matcher<String> ipAddress;
        private HashMap<String, Matcher<? super String>> details;

        public ExpectedEvent realm(Matcher<String> realmId) {
            this.realmId = realmId;
            return this;
        }

        public ExpectedEvent realm(RealmRepresentation realm) {
            return realm(CoreMatchers.equalTo(realm.getId()));
        }

        public ExpectedEvent realm(String realmId) {
            return realm(CoreMatchers.equalTo(realmId));
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
            this.ipAddress = CoreMatchers.equalTo(ipAddress);
            return this;
        }

        public ExpectedEvent ipAddress(Matcher<String> ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public ExpectedEvent event(EventType e) {
            expected.setType(e.name());
            return this;
        }

        public ExpectedEvent detail(String key, String value) {
            if (key.equals(Details.SCOPE)) {
                // the scopes can be given in any order,
                // therefore, use a matcher that takes a string and ignores the order of the scopes
                return detail(key, new TypeSafeMatcher<String>() {
                    @Override
                    protected boolean matchesSafely(String actualValue) {
                        return Matchers.containsInAnyOrder(value.split(" ")).matches(Arrays.asList(actualValue.split(" ")));
                    }

                    @Override
                    public void describeTo(Description description) {
                        description.appendText("contains scope in any order");
                    }
                });
            } else {
                return detail(key, CoreMatchers.equalTo(value));
            }
        }

        public ExpectedEvent detail(String key, Matcher<? super String> matcher) {
            if (details == null) {
                details = new HashMap<String, Matcher<? super String>>();
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
            if (expected.getError() != null && ! expected.getType().toString().endsWith("_ERROR")) {
                expected.setType(expected.getType() + "_ERROR");
            }
            Assert.assertThat("type", actual.getType(), is(expected.getType()));
            Assert.assertThat("realm ID", actual.getRealmId(), is(realmId));
            Assert.assertThat("client ID", actual.getClientId(), is(expected.getClientId()));
            Assert.assertThat("error", actual.getError(), is(expected.getError()));
            Assert.assertThat("ip address", actual.getIpAddress(), ipAddress);
            Assert.assertThat("user ID", actual.getUserId(), is(userId));
            Assert.assertThat("session ID", actual.getSessionId(), is(sessionId));

            if (details == null || details.isEmpty()) {
//                Assert.assertNull(actual.getDetails());
            } else {
                Assert.assertNotNull(actual.getDetails());
                for (Map.Entry<String, Matcher<? super String>> d : details.entrySet()) {
                    String actualValue = actual.getDetails().get(d.getKey());
                    if (!actual.getDetails().containsKey(d.getKey())) {
                        Assert.fail(d.getKey() + " missing");
                    }

                    Assert.assertThat("Unexpected value for " + d.getKey(), actualValue, is(d.getValue()));
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

    public Matcher<String> defaultRealmId() {
        return new TypeSafeMatcher<String>() {
            private String realmId;

            @Override
            protected boolean matchesSafely(String item) {
                return item.equals(getRealmId());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(getRealmId());
            }

            private String getRealmId() {
                if (realmId == null) {
                    RealmRepresentation realm = context.adminClient.realm(DEFAULT_REALM).toRepresentation();
                    if (realm == null) {
                        throw new RuntimeException("Default user does not exist: " + DEFAULT_USERNAME + ". Make sure to add it to your test realm.");
                    }
                    realmId = realm.getId();
                }
                return realmId;
            }

        };
    }

    public Matcher<String> defaultUserId() {
        return new TypeSafeMatcher<String>() {
            private String userId;

            @Override
            protected boolean matchesSafely(String item) {
                return item.equals(getUserId());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(getUserId());
            }

            private String getUserId() {
                if (userId == null) {
                    UserRepresentation user = getUser(DEFAULT_USERNAME);
                    if (user == null) {
                        throw new RuntimeException("Default user does not exist: " + DEFAULT_USERNAME + ". Make sure to add it to your test realm.");
                    }
                    userId = user.getId();
                }
                return userId;
            }

        };
    }

    private UserRepresentation getUser(String username) {
        List<UserRepresentation> users = context.adminClient.realm(DEFAULT_REALM).users().search(username, null, null, null, 0, 1);
        return users.isEmpty() ? null : users.get(0);
    }

    private EventRepresentation fetchNextEvent() {
        return context.testingClient.testing().pollEvent();
    }
}
