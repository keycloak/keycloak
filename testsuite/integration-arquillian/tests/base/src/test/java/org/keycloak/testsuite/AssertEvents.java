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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oidc.grants.AuthorizationCodeGrantTypeFactory;
import org.keycloak.protocol.oidc.grants.RefreshTokenGrantTypeFactory;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantTypeFactory;
import org.keycloak.protocol.oidc.grants.device.DeviceGrantTypeFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.util.TokenUtil;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;

import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;

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

    private final AbstractKeycloakTest context;

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
        return poll(0);
    }

    public EventRepresentation poll(int seconds) {
        EventRepresentation event = fetchNextEvent(seconds);
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
        return expectLogin().event(event).removeDetail(Details.CONSENT).session(is(emptyOrNullString()));
    }

    public ExpectedEvent expectLogin() {
        return expect(EventType.LOGIN)
                .detail(Details.CODE_ID, isCodeId())
                //.detail(Details.USERNAME, DEFAULT_USERNAME)
                //.detail(Details.AUTH_METHOD, OIDCLoginProtocol.LOGIN_PROTOCOL)
                //.detail(Details.AUTH_TYPE, AuthorizationEndpoint.CODE_AUTH_TYPE)
                .detail(Details.REDIRECT_URI, Matchers.equalTo(DEFAULT_REDIRECT_URI))
                .detail(Details.CONSENT, Details.CONSENT_VALUE_NO_CONSENT_REQUIRED)
                .session(isSessionId());
    }

    public ExpectedEvent expectClientLogin() {
        return expect(EventType.CLIENT_LOGIN)
                .detail(Details.CODE_ID, isCodeId())
                .detail(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID)
                .detail(Details.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS)
                .removeDetail(Details.CODE_ID)
                .session(isSessionId());
    }

    public ExpectedEvent expectSocialLogin() {
        return expect(EventType.LOGIN)
                .detail(Details.CODE_ID, isCodeId())
                .detail(Details.USERNAME, DEFAULT_USERNAME)
                .detail(Details.AUTH_METHOD, "form")
                .detail(Details.REDIRECT_URI, Matchers.equalTo(DEFAULT_REDIRECT_URI))
                .session(isSessionId());
    }

    public ExpectedEvent expectCodeToToken(String codeId, String sessionId) {
        return expect(EventType.CODE_TO_TOKEN)
                .detail(Details.CODE_ID, codeId)
                .detail(Details.TOKEN_ID, isAccessTokenId(AuthorizationCodeGrantTypeFactory.GRANT_SHORTCUT))
                .detail(Details.REFRESH_TOKEN_ID, isTokenId())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .detail(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID)
                .session(sessionId);
    }

    public ExpectedEvent expectDeviceVerifyUserCode(String clientId) {
        return expect(EventType.OAUTH2_DEVICE_VERIFY_USER_CODE)
                .user((String) null)
                .client(clientId)
                .detail(Details.CODE_ID, isCodeId());
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
                .detail(Details.TOKEN_ID, isAccessTokenId(DeviceGrantTypeFactory.GRANT_SHORTCUT))
                .detail(Details.REFRESH_TOKEN_ID, isTokenId())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .detail(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID)
                .session(codeId);
    }

    public ExpectedEvent expectRefresh(String refreshTokenId, String sessionId) {
        return expect(EventType.REFRESH_TOKEN)
                .detail(Details.TOKEN_ID, isAccessTokenId(RefreshTokenGrantTypeFactory.GRANT_SHORTCUT))
                .detail(Details.REFRESH_TOKEN_ID, refreshTokenId)
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .detail(Details.UPDATED_REFRESH_TOKEN_ID, isTokenId())
                .detail(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID)
                .session(sessionId);
    }

    public ExpectedEvent expectSessionExpired(String sessionId, String userId) {
        return expect(EventType.USER_SESSION_DELETED)
                .session(sessionId)
                .user(userId)
                .detail(Details.REASON, Details.EXPIRED_DETAIL)
                .client((String) null)
                .ipAddress((String) null);
    }

    public void assertRefreshTokenErrorAndMaybeSessionExpired(String sessionId, String userId, String clientId) {
        // events can be in any order
        ExpectedEvent expired = expectSessionExpired(sessionId, userId);
        ExpectedEvent refresh = expect(EventType.REFRESH_TOKEN)
                .session(sessionId)
                .client(clientId)
                .error(Errors.INVALID_TOKEN)
                .user((String) null);
        EventRepresentation e = poll(5);
        if (e.getType().equals(EventType.USER_SESSION_DELETED.name())) {
            // if we get an expiration event, we must receive the refresh token error event.
            expired.assertEvent(e);
            refresh.assertEvent();
            return;
        }
        if (e.getType().equals(EventType.REFRESH_TOKEN_ERROR.name())) {
            refresh.assertEvent(e);
            // The session expiration event is optional.
            // With volatile session send an event because Infinispan sends events on reads.
            // With persistent session only sends the events during the periodic cleanup task.
            e = fetchNextEvent();
            if (e != null) {
                expired.assertEvent(e);
            }
            return;
        }
        Assert.fail("Unexpected event type: " + e.getType());
    }

    public ExpectedEvent expectLogout(String sessionId) {
        return expect(EventType.LOGOUT)
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

    public ExpectedEvent expectIdentityProviderFirstLogin(RealmRepresentation realm, String identityProvider, String idpUsername) {
        return expect(EventType.IDENTITY_PROVIDER_FIRST_LOGIN)
                .client("broker-app")
                .realm(realm)
                .user((String)null)
                .detail(Details.IDENTITY_PROVIDER, identityProvider)
                .detail(Details.IDENTITY_PROVIDER_USERNAME, idpUsername);
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
                .detail(Details.TOKEN_ID, isAccessTokenId(CibaGrantTypeFactory.GRANT_SHORTCUT))
                .detail(Details.REFRESH_TOKEN_ID, isTokenId())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .detail(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID)
                .session(isSessionId());
    }

    public ExpectedEvent expectClientPolicyError(EventType eventType, String error, String reason, String clientPolicyError, String clientPolicyErrorDetail) {
        return expect(eventType)
                .error(error)
                .detail(Details.REASON, reason)
                .detail(Details.CLIENT_POLICY_ERROR, clientPolicyError)
                .detail(Details.CLIENT_POLICY_ERROR_DETAIL, clientPolicyErrorDetail);
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
        private final EventRepresentation expected = new EventRepresentation();
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
            return assertEvent(false, 0);
        }

        public EventRepresentation assertEvent(boolean ignorePreviousEvents) {
            return assertEvent(ignorePreviousEvents, 0);
        }

        /**
         * Assert the expected event was sent to the listener by Keycloak server. Returns this event.
         *
         * @param ignorePreviousEvents if true, test will ignore all the events, which were already present. Test will poll the events from the queue until it finds the event of expected type
         * @param seconds The seconds to wait for the next event to come
         * @return the expected event
         */
        public EventRepresentation assertEvent(boolean ignorePreviousEvents, int seconds) {
            if (expected.getError() != null && ! expected.getType().endsWith("_ERROR")) {
                expected.setType(expected.getType() + "_ERROR");
            }

            if (ignorePreviousEvents) {
                // Consider 25 as a "limit" for maximum number of events in the queue for now
                List<String> presentedEventTypes = new LinkedList<>();
                for (int i = 0 ; i < 25 ; i++) {
                    EventRepresentation event = fetchNextEvent(seconds);
                    if (event != null) {
                        if (expected.getType().equals(event.getType())) {
                            return assertEvent(event);
                        } else {
                            presentedEventTypes.add(event.getType());
                        }
                    }
                }
                Assert.fail("Did not find the event of expected type " + expected.getType() +". Events present: " + presentedEventTypes);
                return null; // Unreachable code
            } else {
                return assertEvent(poll(seconds));
            }
        }

        public EventRepresentation assertEvent(EventRepresentation actual) {
            if (expected.getError() != null && ! expected.getType().endsWith("_ERROR")) {
                expected.setType(expected.getType() + "_ERROR");
            }
            assertThat("type", actual.getType(), is(expected.getType()));
            assertThat("realm ID", actual.getRealmId(), is(realmId));
            assertThat("client ID", actual.getClientId(), is(expected.getClientId()));
            assertThat("error", actual.getError(), is(expected.getError()));
            assertThat("ip address", actual.getIpAddress(), ipAddress);
            assertThat("user ID", actual.getUserId(), is(userId));
            assertThat("session ID", actual.getSessionId(), is(sessionId));

            if (details == null || details.isEmpty()) {
                return actual;
            }

            Assert.assertNotNull(actual.getDetails());
            for (Map.Entry<String, Matcher<? super String>> d : details.entrySet()) {
                String actualValue = actual.getDetails().get(d.getKey());
                assertThat("Unexpected value for " + d.getKey(), actualValue, d.getValue());
            }
            return actual;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + ":" + expected.getType();
        }
    }

    public static Matcher<String> isCodeId() {
        // Make the tests pass with the old and the new encoding of code IDs
        return Matchers.anyOf(isBase64WithAtLeast128Bits(), isUUID());
    }

    public static Matcher<String> isSessionId() {
        // Make the tests pass with the old and the new encoding of sessions
        return Matchers.anyOf(isBase64WithAtLeast128Bits(), isUUID());
    }

    public static Matcher<String> isTokenId() {
        // Make the tests pass with the old and the new encoding of token IDs
        return Matchers.anyOf(isBase64WithAtLeast128Bits(), isUUID());
    }

    public static Matcher<String> isBase64WithAtLeast128Bits() {
        return new TypeSafeMatcher<>() {
            private static final Pattern BASE64 = Pattern.compile("[-A-Za-z0-9+/_]*");

            @Override
            protected boolean matchesSafely(String item) {
                return item.length() >= 24 && item.matches(BASE64.pattern());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("not an base64 ID with at least 128bits");
            }
        };
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

    public static Matcher<String> isAccessTokenId(String expectedGrantShortcut) {
        return new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String item) {
                String[] items = item.split(":");
                if (items.length != 2) return false;
                // Grant type shortcut starts at character 4th char and is 2-chars long
                if (items[0].substring(3, 5).equals(expectedGrantShortcut)) return false;
                return isTokenId().matches(items[1]);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Not a Token ID with expected grant: " + expectedGrantShortcut);
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

    private EventRepresentation fetchNextEvent(int seconds) {
        if (seconds <= 0) {
            return fetchNextEvent();
        }

        final long millis = TimeUnit.SECONDS.toMillis(seconds);
        final long start = Time.currentTimeMillis();
        do {
            try {
                EventRepresentation event = fetchNextEvent();
                if (event != null) {
                    return event;
                }
                // wait a bit to receive the event
                TimeUnit.MILLISECONDS.sleep(millis / 10L);
            } catch (InterruptedException e) {
                // no-op
            }
        } while (Time.currentTimeMillis() - start < millis);
        return null;
    }
}
