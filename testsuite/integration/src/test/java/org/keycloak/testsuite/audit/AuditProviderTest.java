package org.keycloak.testsuite.audit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.audit.AuditProvider;
import org.keycloak.audit.Event;
import org.keycloak.audit.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.testsuite.rule.KeycloakRule;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuditProviderTest {

    @ClassRule
    public static KeycloakRule kc = new KeycloakRule();

    private KeycloakSession session;

    private AuditProvider audit;

    @Before
    public void before() {
        session = kc.startSession();
        audit = session.getProvider(AuditProvider.class);
    }

    @After
    public void after() {
        audit.clear();
        kc.stopSession(session, true);
    }

    @Test
    public void save() {
        audit.onEvent(create(EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
    }

    @Test
    public void query() {
        long oldest = System.currentTimeMillis() - 30000;
        long newest = System.currentTimeMillis() + 30000;

        audit.onEvent(create(EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        audit.onEvent(create(newest, EventType.REGISTER, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        audit.onEvent(create(newest, EventType.REGISTER, "realmId", "clientId", "userId2", "127.0.0.1", "error"));
        audit.onEvent(create(EventType.LOGIN, "realmId2", "clientId", "userId", "127.0.0.1", "error"));
        audit.onEvent(create(oldest, EventType.LOGIN, "realmId", "clientId2", "userId", "127.0.0.1", "error"));
        audit.onEvent(create(EventType.LOGIN, "realmId", "clientId", "userId2", "127.0.0.1", "error"));

        resetSession();

        Assert.assertEquals(5, audit.createQuery().client("clientId").getResultList().size());
        Assert.assertEquals(5, audit.createQuery().realm("realmId").getResultList().size());
        Assert.assertEquals(4, audit.createQuery().event(EventType.LOGIN).getResultList().size());
        Assert.assertEquals(6, audit.createQuery().event(EventType.LOGIN, EventType.REGISTER).getResultList().size());
        Assert.assertEquals(4, audit.createQuery().user("userId").getResultList().size());

        Assert.assertEquals(1, audit.createQuery().user("userId").event(EventType.REGISTER).getResultList().size());

        Assert.assertEquals(2, audit.createQuery().maxResults(2).getResultList().size());
        Assert.assertEquals(1, audit.createQuery().firstResult(5).getResultList().size());

        Assert.assertEquals(newest, audit.createQuery().maxResults(1).getResultList().get(0).getTime());
        Assert.assertEquals(oldest, audit.createQuery().firstResult(5).maxResults(1).getResultList().get(0).getTime());
    }

    @Test
    public void clear() {
        audit.onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        audit.onEvent(create(System.currentTimeMillis() - 20000, EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        audit.onEvent(create(System.currentTimeMillis(), EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        audit.onEvent(create(System.currentTimeMillis(), EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        audit.onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, "realmId2", "clientId", "userId", "127.0.0.1", "error"));

        resetSession();

        audit.clear("realmId");

        Assert.assertEquals(1, audit.createQuery().getResultList().size());
    }

    @Test
    public void clearOld() {
        audit.onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        audit.onEvent(create(System.currentTimeMillis() - 20000, EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        audit.onEvent(create(System.currentTimeMillis(), EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        audit.onEvent(create(System.currentTimeMillis(), EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        audit.onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, "realmId2", "clientId", "userId", "127.0.0.1", "error"));

        resetSession();

        audit.clear("realmId", System.currentTimeMillis() - 10000);

        Assert.assertEquals(3, audit.createQuery().getResultList().size());
    }

    private Event create(EventType event, String realmId, String clientId, String userId, String ipAddress, String error) {
        return create(System.currentTimeMillis(), event, realmId, clientId, userId, ipAddress, error);
    }

    private Event create(long time, EventType event, String realmId, String clientId, String userId, String ipAddress, String error) {
        Event e = new Event();
        e.setTime(time);
        e.setEvent(event);
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
        audit = session.getProvider(AuditProvider.class);
    }

}
