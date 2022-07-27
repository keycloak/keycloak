package org.keycloak.testsuite.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.*;
import org.keycloak.models.UserManager;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.util.TokenClientAttributeHelper;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

@AuthServerContainerExclude(AuthServerContainerExclude.AuthServer.REMOTE)
public class TokenClientAttributeHelperTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }


    @Before
    public  void before() {
        testingClient.server().run( session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            session.users().addUser(realm, "user1");
        });
    }


    @After
    public void after() {
        testingClient.server().run( session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            session.sessions().removeUserSessions(realm);
            UserModel user1 = session.users().getUserByUsername(realm, "user1");

            org.keycloak.models.UserManager um = new UserManager(session);
            if (user1 != null) {
                um.removeUser(realm, user1);
            }
        });
    }

    @Test
    @ModelTest
    public void testGetClientSessionIdleTimeoutValue(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");

        ClientModel clientOverrides = realm.addClient("client-session-idle");
        clientOverrides.setAttribute("client.session.idle.timeout", "3");

        Integer result = TokenClientAttributeHelper.getClientSessionIdleTimeout(realm, clientOverrides.getClientId());
        assertThat(result, is(notNullValue()));
    }

    @Test
    @ModelTest
    public void testGetClientSessionIdleTimeoutNull(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");
        ClientModel clientOverrides = realm.addClient("client-session-idle-null");

        Integer result = TokenClientAttributeHelper.getClientSessionIdleTimeout(realm, clientOverrides.getClientId());
        assertThat(result, is(nullValue()));
    }

    @Test
    @ModelTest
    public void testGetClientSessionMaxLifespanValue(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");

        ClientModel clientOverrides = realm.addClient("client-max-lifespan");
        clientOverrides.setAttribute("client.session.max.lifespan", "5");

        Integer result = TokenClientAttributeHelper.getClientSessionMaxLifespan(realm, clientOverrides.getClientId());
        assertThat(result, is(notNullValue()));
    }

    @Test
    @ModelTest
    public void testGetClientSessionMaxLifespanNull(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");
        ClientModel clientOverrides = realm.addClient("client-max-lifespan-null");

        Integer result = TokenClientAttributeHelper.getClientSessionMaxLifespan(realm, clientOverrides.getClientId());
        assertThat(result, is(nullValue()));
    }
}
