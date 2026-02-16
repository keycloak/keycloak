package org.keycloak.testframework.events;

import java.util.UUID;
import java.util.regex.Pattern;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matchers to assert event properties
 */
public class EventMatchers {

    /**
     * Check if value is a UUID
     * @return
     */
    public static Matcher<String> isUUID() {
        return new UUIDMatcher();
    }

    /**
     * Check if value is a code_id
     *
     * @return
     */
    public static Matcher<String> isCodeId() {
        // Make the tests pass with the old and the new encoding of code IDs
        return Matchers.anyOf(isBase64WithAtLeast128Bits(), isUUID());
    }

    /**
     * Check if value is a session_id
     *
     * @return
     */
    public static Matcher<String> isSessionId() {
        // Make the tests pass with the old and the new encoding of sessions
        return Matchers.anyOf(isBase64WithAtLeast128Bits(), isUUID());
    }

    private static Matcher<String> isBase64WithAtLeast128Bits() {
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

    private EventMatchers() {
    }

    private static class UUIDMatcher extends TypeSafeMatcher<String> {

        @Override
        protected boolean matchesSafely(String item) {
            try {
                UUID.fromString(item);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("not a UUID");
        }
    }

}
