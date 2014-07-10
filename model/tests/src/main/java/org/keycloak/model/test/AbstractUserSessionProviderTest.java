package org.keycloak.model.test;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelProvider;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractUserSessionProviderTest {

    private KeycloakSession session;
    private UserSessionProvider provider;
    private RealmModel realm;
    private UserModel user;
    private ApplicationModel app1;
    private ApplicationModel app2;
    private OAuthClientModel client1;
    private ModelProvider model;

    @Before
    public void before() {
        session = EasyMock.createMock(KeycloakSession.class);

        model = EasyMock.createMock(ModelProvider.class);
        EasyMock.expect(session.model()).andReturn(model).anyTimes();

        realm = EasyMock.createMock(RealmModel.class);
        EasyMock.expect(realm.getId()).andReturn("realm-id").anyTimes();
        EasyMock.expect(realm.getSsoSessionIdleTimeout()).andReturn(1).anyTimes();
        EasyMock.expect(model.getRealm("realm-id")).andReturn(realm).anyTimes();

        user = EasyMock.createMock(UserModel.class);
        EasyMock.expect(user.getId()).andReturn("user-id").anyTimes();
        EasyMock.expect(model.getUserById("user-id", realm)).andReturn(user).anyTimes();

        app1 = EasyMock.createMock(ApplicationModel.class);
        EasyMock.expect(app1.getClientId()).andReturn("app1").anyTimes();
        EasyMock.expect(realm.findClient("app1")).andReturn(app1).anyTimes();

        app2 = EasyMock.createMock(ApplicationModel.class);
        EasyMock.expect(app2.getClientId()).andReturn("app2").anyTimes();
        EasyMock.expect(realm.findClient("app2")).andReturn(app2).anyTimes();

        client1 = EasyMock.createMock(OAuthClientModel.class);
        EasyMock.expect(client1.getClientId()).andReturn("client1").anyTimes();
        EasyMock.expect(realm.findClient("client1")).andReturn(client1).anyTimes();

        EasyMock.replay(session, model, realm, user, app1, app2, client1);

        provider = createProvider(session);
        provider.getTransaction().begin();
    }

    @After
    public void after() {
        provider.getTransaction().commit();

        provider.getTransaction().begin();
        provider.onRealmRemoved(realm);
        provider.getTransaction().commit();

        provider.close();
    }

    public abstract UserSessionProvider createProvider(KeycloakSession session);

    @Test
    public void userSessions() throws InterruptedException {
        UserSessionModel userSession = provider.createUserSession(realm, user, "127.0.0.1");
        commit();

        assertNotNull(provider.getUserSession(realm, userSession.getId()));
        commit();

        provider.removeUserSession(realm, provider.getUserSession(realm, userSession.getId()));
        commit();

        assertNull(provider.getUserSession(realm, userSession.getId()));

        userSession = provider.createUserSession(realm, user, "127.0.0.1");
        commit();

        provider.removeUserSessions(realm, user);
        commit();

        assertNull(provider.getUserSession(realm, userSession.getId()));

        userSession = provider.createUserSession(realm, user, "127.0.0.1");
        commit();

        Thread.sleep(2000);

        provider.removeExpiredUserSessions(realm);
        commit();

        assertNull(provider.getUserSession(realm, userSession.getId()));
    }

    @Test
    public void userSessionAssociations() {
        UserSessionModel userSession = provider.createUserSession(realm, user, "127.0.0.1");

        assertEquals(0, userSession.getClientAssociations().size());

        userSession.associateClient(app1);
        userSession.associateClient(client1);

        assertEquals(2, userSession.getClientAssociations().size());
        assertTrue(provider.getUserSessions(realm, app1).contains(userSession));
        assertFalse(provider.getUserSessions(realm, app2).contains(userSession));
        assertTrue(provider.getUserSessions(realm, client1).contains(userSession));

        commit();

        userSession = provider.getUserSession(realm, userSession.getId());

        userSession.removeAssociatedClient(app1);
        assertEquals(1, userSession.getClientAssociations().size());
        assertEquals(client1, userSession.getClientAssociations().get(0));
        assertFalse(provider.getUserSessions(realm, app1).contains(userSession));

        commit();

        userSession = provider.getUserSession(realm, userSession.getId());

        userSession.removeAssociatedClient(client1);
        assertEquals(0, userSession.getClientAssociations().size());
        assertFalse(provider.getUserSessions(realm, client1).contains(userSession));
    }

    public void commit() {
        provider.getTransaction().commit();
        provider.getTransaction().begin();
    }

}
