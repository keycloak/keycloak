package org.keycloak.testsuite.events;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.events.Event;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.testsuite.rule.KeycloakRule;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EventStoreProviderTest {

    @ClassRule
    public static KeycloakRule kc = new KeycloakRule();

    private KeycloakSession session;

    private EventStoreProvider eventStore;

    @Before
    public void before() {
        session = kc.startSession();
        eventStore = session.getProvider(EventStoreProvider.class);
    }

    @After
    public void after() {
        eventStore.clear();
        kc.stopSession(session, true);
    }

    @Test
    public void save() {
        eventStore.onEvent(create(EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
    }

    @Test
    public void query() {
        long oldest = System.currentTimeMillis() - 30000;
        long newest = System.currentTimeMillis() + 30000;

        eventStore.onEvent(create(EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        eventStore.onEvent(create(newest, EventType.REGISTER, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        eventStore.onEvent(create(newest, EventType.REGISTER, "realmId", "clientId", "userId2", "127.0.0.1", "error"));
        eventStore.onEvent(create(EventType.LOGIN, "realmId2", "clientId", "userId", "127.0.0.1", "error"));
        eventStore.onEvent(create(oldest, EventType.LOGIN, "realmId", "clientId2", "userId", "127.0.0.1", "error"));
        eventStore.onEvent(create(EventType.LOGIN, "realmId", "clientId", "userId2", "127.0.0.1", "error"));

        resetSession();

        Assert.assertEquals(5, eventStore.createQuery().client("clientId").getResultList().size());
        Assert.assertEquals(5, eventStore.createQuery().realm("realmId").getResultList().size());
        Assert.assertEquals(4, eventStore.createQuery().type(EventType.LOGIN).getResultList().size());
        Assert.assertEquals(6, eventStore.createQuery().type(EventType.LOGIN, EventType.REGISTER).getResultList().size());
        Assert.assertEquals(4, eventStore.createQuery().user("userId").getResultList().size());

        Assert.assertEquals(1, eventStore.createQuery().user("userId").type(EventType.REGISTER).getResultList().size());

        Assert.assertEquals(2, eventStore.createQuery().maxResults(2).getResultList().size());
        Assert.assertEquals(1, eventStore.createQuery().firstResult(5).getResultList().size());

        Assert.assertEquals(newest, eventStore.createQuery().maxResults(1).getResultList().get(0).getTime());
        Assert.assertEquals(oldest, eventStore.createQuery().firstResult(5).maxResults(1).getResultList().get(0).getTime());
    }

    @Test
    public void clear() {
        eventStore.onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        eventStore.onEvent(create(System.currentTimeMillis() - 20000, EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        eventStore.onEvent(create(System.currentTimeMillis(), EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        eventStore.onEvent(create(System.currentTimeMillis(), EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        eventStore.onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, "realmId2", "clientId", "userId", "127.0.0.1", "error"));

        resetSession();

        eventStore.clear("realmId");

        Assert.assertEquals(1, eventStore.createQuery().getResultList().size());
    }

    @Test
    public void clearOld() {
        eventStore.onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        eventStore.onEvent(create(System.currentTimeMillis() - 20000, EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        eventStore.onEvent(create(System.currentTimeMillis(), EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        eventStore.onEvent(create(System.currentTimeMillis(), EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        eventStore.onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, "realmId2", "clientId", "userId", "127.0.0.1", "error"));

        resetSession();

        eventStore.clear("realmId", System.currentTimeMillis() - 10000);

        Assert.assertEquals(3, eventStore.createQuery().getResultList().size());
    }

    private Event create(EventType event, String realmId, String clientId, String userId, String ipAddress, String error) {
        return create(System.currentTimeMillis(), event, realmId, clientId, userId, ipAddress, error);
    }

    private Event create(long time, EventType event, String realmId, String clientId, String userId, String ipAddress, String error) {
        Event e = new Event();
        e.setTime(time);
        e.setType(event);
        e.setRealmId(realmId);
        e.setClientId(clientId);
        e.setUserId(userId);
        e.setIpAddress(ipAddress);
        e.setError(error);

        Map<String, String> details = new HashMap<String, String>();
        details.put("key1", "value1");
        details.put("key2", "value2");

        e.setDetails(details);

        return e;
    }

    private void resetSession() {
        kc.stopSession(session, true);
        session = kc.startSession();
        eventStore = session.getProvider(EventStoreProvider.class);
    }

}
