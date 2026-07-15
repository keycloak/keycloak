package org.keycloak.testframework.tests;

import java.util.List;

import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.services.scheduled.ClearExpiredEvents;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class EventsTest {

    @InjectRealm
    private ManagedRealm realm;

    @InjectEvents
    private Events events;

    @InjectOAuthClient
    private OAuthClient oAuthClient;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void testFailedLogin() {
        oAuthClient.doPasswordGrantRequest("invalid", "invalid");

        EventRepresentation event = events.poll();
        EventAssertion.assertError(event).type(EventType.LOGIN_ERROR).details("username", "invalid");

        oAuthClient.doPasswordGrantRequest("invalid2", "invalid");

        event = events.poll();
        EventAssertion.assertError(event).type(EventType.LOGIN_ERROR).details("username", "invalid2");
    }

    @Test
    public void testTimeOffset() {
        timeOffSet.set(60);

        oAuthClient.doClientCredentialsGrantAccessTokenRequest();

        EventAssertion.assertSuccess(events.poll()).type(EventType.CLIENT_LOGIN);
    }

    @Test
    public void testClientLogin() {
        oAuthClient.doClientCredentialsGrantAccessTokenRequest();

        EventAssertion.assertSuccess(events.poll()).type(EventType.CLIENT_LOGIN);
    }

    @Test
    public void testExpireLoginEvents() {
        this.realm.updateWithCleanup(r -> r
                .eventsEnabled(true)
                .eventsExpiration(1));

        oAuthClient.doPasswordGrantRequest("invalid", "invalid");
        events.poll();

        List<EventRepresentation> storedEvents = realm.admin().getEvents();
        Assertions.assertFalse(storedEvents.isEmpty(), "Expected at least one login event in the store");

        timeOffSet.set(10);

        runOnServer.run(session -> new ClearExpiredEvents().run(session));

        List<EventRepresentation> remainingEvents = realm.admin().getEvents();
        Assertions.assertTrue(remainingEvents.isEmpty(), "Expected all login events to be expired and removed");
    }

}
