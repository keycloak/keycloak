package org.keycloak.testframework.tests;

import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.services.scheduled.ClearExpiredAdminEvents;
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

import org.junit.jupiter.api.Test;

import static org.keycloak.models.jpa.entities.RealmAttributes.ADMIN_EVENTS_EXPIRATION;

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
    public void testExpireEvents() {
        this.realm.updateWithCleanup(r -> r
                .eventsEnabled(true)
                .adminEventsEnabled(true)
                .eventsExpiration(1)
                // TODO: Create a new method in the builder
                .update(r1 -> r1.getAttributes().put(ADMIN_EVENTS_EXPIRATION, "1")));
        // TODO create a user and an admin event. Ensure that they are in the store
        timeOffSet.set(10);
        runOnServer.run(session -> {
            new ClearExpiredAdminEvents().run(session);
            new ClearExpiredEvents().run(session);
        });
        // TODO: Verify that those events have been removed from the database
    }

}
