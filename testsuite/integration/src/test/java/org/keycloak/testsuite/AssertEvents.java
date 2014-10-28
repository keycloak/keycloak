package org.keycloak.testsuite;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;
import org.keycloak.Config;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.rule.KeycloakRule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AssertEvents implements TestRule, EventListenerProviderFactory {

    public static String DEFAULT_CLIENT_ID = "test-app";
    public static String DEFAULT_REDIRECT_URI = "http://localhost:8081/app/auth";
    public static String DEFAULT_IP_ADDRESS = "127.0.0.1";
    public static String DEFAULT_REALM = "test";
    public static String DEFAULT_USERNAME = "test-user@localhost";

    private KeycloakRule keycloak;

    private static BlockingQueue<Event> events = new LinkedBlockingQueue<Event>();

    public AssertEvents() {
    }

    public AssertEvents(KeycloakRule keycloak) {
        this.keycloak = keycloak;
    }

    @Override
    public String getId() {
        return "assert-events";
    }

    @Override
    public Statement apply(final Statement base, org.junit.runner.Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                events.clear();

                keycloak.configure(new KeycloakRule.KeycloakSetup() {
                    @Override
                    public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                        Set<String> listeners = new HashSet<String>();
                        listeners.add("jboss-logging");
                        listeners.add("assert-events");
                        appRealm.setEventsListeners(listeners);
                    }
                });

                try {
                    base.evaluate();

                    Event event = events.peek();
                    if (event != null) {
                        Assert.fail("Unexpected type after test: " + event.getType());
                    }
                } finally {
                    keycloak.configure(new KeycloakRule.KeycloakSetup() {
                        @Override
                        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                            appRealm.setEventsListeners(null);
                        }
                    });
                }
            }
        };
    }

    public void assertEmpty() {
         Assert.assertTrue(events.isEmpty());
    }

    public Event poll() {
        try {
            return events.poll(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }

    public void clear() {
        events.clear();
    }

    public ExpectedEvent expectRequiredAction(EventType event) {
        return expectLogin().event(event).session(isUUID());
    }

    public ExpectedEvent expectLogin() {
        return expect(EventType.LOGIN)
                .detail(Details.CODE_ID, isCodeId())
                .detail(Details.USERNAME, DEFAULT_USERNAME)
                .detail(Details.RESPONSE_TYPE, "code")
                .detail(Details.AUTH_METHOD, "form")
                .detail(Details.REDIRECT_URI, DEFAULT_REDIRECT_URI)
                .session(isUUID());
    }

    public ExpectedEvent expectSocialLogin() {
        return expect(EventType.LOGIN)
                .detail(Details.CODE_ID, isCodeId())
                .detail(Details.USERNAME, DEFAULT_USERNAME)
                .detail(Details.AUTH_METHOD, "form")
                .detail(Details.REDIRECT_URI, DEFAULT_REDIRECT_URI)
                .session(isUUID());
    }

    public ExpectedEvent expectCodeToToken(String codeId, String sessionId) {
        return expect(EventType.CODE_TO_TOKEN)
                .detail(Details.CODE_ID, codeId)
                .detail(Details.TOKEN_ID, isUUID())
                .detail(Details.REFRESH_TOKEN_ID, isUUID())
                .session(sessionId);
    }

    public ExpectedEvent expectRefresh(String refreshTokenId, String sessionId) {
        return expect(EventType.REFRESH_TOKEN)
                .detail(Details.TOKEN_ID, isUUID())
                .detail(Details.REFRESH_TOKEN_ID, refreshTokenId)
                .detail(Details.UPDATED_REFRESH_TOKEN_ID, isUUID())
                .session(sessionId);
    }

    public ExpectedEvent expectLogout(String sessionId) {
        return expect(EventType.LOGOUT).client((String) null)
                .detail(Details.REDIRECT_URI, DEFAULT_REDIRECT_URI)
                .session(sessionId);
    }

    public ExpectedEvent expectRegister(String username, String email) {
        UserRepresentation user = username != null ? keycloak.getUser("test", username) : null;
        return expect(EventType.REGISTER)
                .user(user != null ? user.getId() : null)
                .detail(Details.USERNAME, username)
                .detail(Details.EMAIL, email)
                .detail(Details.RESPONSE_TYPE, "code")
                .detail(Details.REGISTER_METHOD, "form")
                .detail(Details.REDIRECT_URI, DEFAULT_REDIRECT_URI);
    }

    public ExpectedEvent expectAccount(EventType event) {
        return expect(event).client("account");
    }

    public ExpectedEvent expect(EventType event) {
        return new ExpectedEvent()
                .realm(DEFAULT_REALM)
                .client(DEFAULT_CLIENT_ID)
                .user(keycloak.getUser(DEFAULT_REALM, DEFAULT_USERNAME).getId())
                .ipAddress(DEFAULT_IP_ADDRESS)
                .session((String) null)
                .event(event);
    }

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new EventListenerProvider() {
            @Override
            public void onEvent(Event event) {
                if (event == null) {
                    throw new RuntimeException("Added null type");
                }
                events.add(event);
            }

            @Override
            public void close() {
            }
        };
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void close() {
    }

    public static class ExpectedEvent {
        private Event expected = new Event();
        private Matcher<String> userId;
        private Matcher<String> sessionId;
        private HashMap<String, Matcher<String>> details;

        public ExpectedEvent realm(RealmModel realm) {
            expected.setRealmId(realm.getId());
            return this;
        }

        public ExpectedEvent realm(String realmId) {
            expected.setRealmId(realmId);
            return this;
        }

        public ExpectedEvent client(ClientModel client) {
            expected.setClientId(client.getClientId());
            return this;
        }

        public ExpectedEvent client(String clientId) {
            expected.setClientId(clientId);
            return this;
        }

        public ExpectedEvent user(UserModel user) {
            return user(user.getId());
        }

        public ExpectedEvent user(String userId) {
            return user(CoreMatchers.equalTo(userId));
        }

        public ExpectedEvent user(Matcher<String> userId) {
            this.userId = userId;
            return this;
        }

        public ExpectedEvent session(UserSessionModel session) {
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
            expected.setType(e);
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

        public Event assertEvent() {
            try {
                return assertEvent(events.poll(10, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new AssertionError("No type received within timeout");
            }
        }

        public Event assertEvent(Event actual) {
            if (expected.getError() != null && !expected.getType().toString().endsWith("_ERROR")) {
                expected.setType(EventType.valueOf(expected.getType().toString() + "_ERROR"));
            }
            Assert.assertEquals(expected.getType(), actual.getType());
            Assert.assertEquals(expected.getRealmId(), actual.getRealmId());
            Assert.assertEquals(expected.getClientId(), actual.getClientId());
            Assert.assertEquals(expected.getError(), actual.getError());
            Assert.assertEquals(expected.getIpAddress(), actual.getIpAddress());
            Assert.assertThat(actual.getUserId(), userId);
            Assert.assertThat(actual.getSessionId(), sessionId);

            if (details == null || details.isEmpty()) {
                Assert.assertNull(actual.getDetails());
            } else {
                Assert.assertNotNull(actual.getDetails());
                for (Map.Entry<String, Matcher<String>> d : details.entrySet()) {
                    String actualValue = actual.getDetails().get(d.getKey());
                    if (!actual.getDetails().containsKey(d.getKey())) {
                        Assert.fail(d.getKey() + " missing");
                    }

                    Assert.assertThat("Unexpected value for " + d.getKey(), actualValue, d.getValue());
                }

                for (String k : actual.getDetails().keySet()) {
                    if (!details.containsKey(k)) {
                        Assert.fail(k + " was not expected");
                    }
                }
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
                return KeycloakModelUtils.generateId().length() == item.length();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Not an UUID");
            }
        };
    }

}
