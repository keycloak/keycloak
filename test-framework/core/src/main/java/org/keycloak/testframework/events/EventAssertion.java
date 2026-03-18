package org.keycloak.testframework.events;

import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;

import static org.hamcrest.Matchers.is;

/**
 * Helper to assert login events
 */
public class EventAssertion {

    private final EventRepresentation event;

    private static final String DEFAULT_IP_ADDRESS = "127.0.0.1";
    private static final String DEFAULT_IP_ADDRESS_V6 = "0:0:0:0:0:0:0:1";
    private static final String DEFAULT_IP_ADDRESS_V6_SHORT = "::1";

    protected EventAssertion(EventRepresentation event) {
        Assertions.assertNotNull(event, "Event was null");
        Assertions.assertNotNull(event.getId(), "Event id was null");
        this.event = event;
    }

    /**
     * Assert an expected successfull event
     *
     * @param event the event to assert
     * @return
     */
    public static EventAssertion assertSuccess(EventRepresentation event) {
        Assertions.assertFalse(event.getType().endsWith("_ERROR"), "Expected successful event");
        return new EventAssertion(event);
    }

    /**
     * Assert an expected error event
     *
     * @param event the event to assert
     * @return
     */
    public static EventAssertion assertError(EventRepresentation event) {
        Assertions.assertTrue(event.getType().endsWith("_ERROR"), "Expected error event");
        return new EventAssertion(event);
    }

    /**
     * Assert the error message
     *
     * @param error the expected error message
     * @return
     */
    public EventAssertion error(String error) {
        Assertions.assertEquals(error, event.getError());
        return this;
    }

    /**
     * Assert the type of the event
     *
     * @param type the expected type of the event
     * @return
     */
    public EventAssertion type(EventType type) {
        Assertions.assertEquals(type, EventType.valueOf(event.getType()));
        return this;
    }

    /**
     * Assert the event has a sessionId set
     *
     * @return
     */
    public EventAssertion hasSessionId() {
        MatcherAssert.assertThat(event.getSessionId(), EventMatchers.isSessionId());
        return this;
    }

    /**
     * Assert the event has the <code>code_id</code> details set
     * @return
     */
    public EventAssertion isCodeId() {
        MatcherAssert.assertThat(event.getDetails().get(Details.CODE_ID), EventMatchers.isCodeId());
        return this;
    }

    /**
     * Assert the event has an ipAddress set
     * @return
     */
    public EventAssertion hasIpAddress() {
        Assertions.assertNotNull(event.getIpAddress());
        Assertions.assertFalse(event.getIpAddress().isEmpty());
        MatcherAssert.assertThat(event.getIpAddress(), Matchers.anyOf(is(DEFAULT_IP_ADDRESS), is(DEFAULT_IP_ADDRESS_V6), is(DEFAULT_IP_ADDRESS_V6_SHORT)));
        return this;
    }

    /**
     * Assert the clientId for the event
     *
     * @param clientId the expected clientId
     * @return
     */
    public EventAssertion clientId(String clientId) {
        Assertions.assertEquals(clientId, event.getClientId());
        return this;
    }

    /**
     * Assert the sessionId for the event
     *
     * @param sessionId the expected sessionId
     * @return
     */
    public EventAssertion sessionId(String sessionId) {
        Assertions.assertEquals(sessionId, event.getSessionId());
        return this;
    }

    /**
     * Assert the userId (sub) of the event
     *
     * @param userId the expected userId
     * @return
     */
    public EventAssertion userId(String userId) {
        Assertions.assertEquals(userId, event.getUserId());
        return this;
    }

    /**
     * Assert the event has an entry in the details map with the specified key and value
     *
     * @param key the expected details key
     * @param value the expected details value
     * @return
     */
    public EventAssertion details(String key, String value) {
        if (value != null) {
            MatcherAssert.assertThat(event.getDetails(), Matchers.hasEntry(key, value));
        } else {
            withoutDetails(key);
        }
        return this;
    }

    /**
     * Assert the event details map does not contain the specified keys
     *
     * @param keys the list of keys that are not expected in the details map
     * @return
     */
    public EventAssertion withoutDetails(String... keys) {
        for (String key : keys) {
            MatcherAssert.assertThat(event.getDetails(), Matchers.not(Matchers.hasKey(key)));
        }
        return this;
    }

}
