package org.keycloak.testframework.events;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.UUID;

public class EventMatchers {

    public static Matcher<String> isUUID() {
        return new UUIDMatcher();
    }

    private EventMatchers() {
    }

    public static class UUIDMatcher extends TypeSafeMatcher<String> {

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
