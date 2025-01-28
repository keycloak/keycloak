package org.keycloak.test.examples;

import com.nimbusds.oauth2.sdk.GeneralException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.nimbus.OAuthClient;
import org.keycloak.testframework.oauth.nimbus.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;

import java.io.IOException;

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

    @Test
    public void testFailedLogin() {
        oAuthClient.resourceOwnerCredentialGrant("invalid", "invalid");

        EventRepresentation event = events.poll();
        Assertions.assertEquals(EventType.LOGIN_ERROR.name(), event.getType());
        Assertions.assertEquals("invalid", event.getDetails().get("username"));

        oAuthClient.resourceOwnerCredentialGrant("invalid2", "invalid");

        event = events.poll();
        Assertions.assertEquals(EventType.LOGIN_ERROR.name(), event.getType());
        Assertions.assertEquals("invalid2", event.getDetails().get("username"));
    }

    @Test
    public void testTimeOffset() throws GeneralException, IOException {
        timeOffSet.set(60);

        oAuthClient.clientCredentialGrant();

        Assertions.assertEquals(EventType.CLIENT_LOGIN.name(), events.poll().getType());
    }

    @Test
    public void testClientLogin() throws GeneralException, IOException {
        oAuthClient.clientCredentialGrant();

        Assertions.assertEquals(EventType.CLIENT_LOGIN.name(), events.poll().getType());
    }

}
