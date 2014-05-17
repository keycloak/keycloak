package org.keycloak.audit.tests;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.Config;
import org.keycloak.audit.AuditProvider;
import org.keycloak.audit.AuditProviderFactory;
import org.keycloak.audit.Event;
import org.keycloak.provider.ProviderFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractAuditProviderTest {

    private ProviderFactory<AuditProvider> factory;
    private AuditProvider provider;

    @Before
    public void before() {
        String providerId = getProviderId();
        ServiceLoader<AuditProviderFactory> factories = ServiceLoader.load(AuditProviderFactory.class);
        for (AuditProviderFactory f : factories) {
            if (f.getId().equals(providerId)) {
                factory = f;
                factory.init(Config.scope("audit", providerId));
            }
        }

        provider = factory.create(null);
    }

    protected abstract String getProviderId();

    @After
    public void after() {
        provider.clear();
        provider.close();
        factory.close();
    }

    @Test
    public void save() {
        provider.onEvent(create("event", "realmId", "clientId", "userId", "127.0.0.1", "error"));
    }

    @Test
    public void query() {
        long oldest = System.currentTimeMillis() - 30000;
        long newest = System.currentTimeMillis() + 30000;

        provider.onEvent(create("event", "realmId", "clientId", "userId", "127.0.0.1", "error"));
        provider.onEvent(create(newest, "event2", "realmId", "clientId", "userId", "127.0.0.1", "error"));
        provider.onEvent(create(newest, "event2", "realmId", "clientId", "userId2", "127.0.0.1", "error"));
        provider.onEvent(create("event", "realmId2", "clientId", "userId", "127.0.0.1", "error"));
        provider.onEvent(create(oldest, "event", "realmId", "clientId2", "userId", "127.0.0.1", "error"));
        provider.onEvent(create("event", "realmId", "clientId", "userId2", "127.0.0.1", "error"));

        provider.close();
        provider = factory.create(null);

        Assert.assertEquals(5, provider.createQuery().client("clientId").getResultList().size());
        Assert.assertEquals(5, provider.createQuery().realm("realmId").getResultList().size());
        Assert.assertEquals(4, provider.createQuery().event("event").getResultList().size());
        Assert.assertEquals(6, provider.createQuery().event("event", "event2").getResultList().size());
        Assert.assertEquals(4, provider.createQuery().user("userId").getResultList().size());

        Assert.assertEquals(1, provider.createQuery().user("userId").event("event2").getResultList().size());

        Assert.assertEquals(2, provider.createQuery().maxResults(2).getResultList().size());
        Assert.assertEquals(1, provider.createQuery().firstResult(5).getResultList().size());

        Assert.assertEquals(newest, provider.createQuery().maxResults(1).getResultList().get(0).getTime());
        Assert.assertEquals(oldest, provider.createQuery().firstResult(5).maxResults(1).getResultList().get(0).getTime());
    }

    @Test
    public void clear() {
        provider.onEvent(create(System.currentTimeMillis() - 30000, "event", "realmId", "clientId", "userId", "127.0.0.1", "error"));
        provider.onEvent(create(System.currentTimeMillis() - 20000, "event", "realmId", "clientId", "userId", "127.0.0.1", "error"));
        provider.onEvent(create(System.currentTimeMillis(), "event", "realmId", "clientId", "userId", "127.0.0.1", "error"));
        provider.onEvent(create(System.currentTimeMillis(), "event", "realmId", "clientId", "userId", "127.0.0.1", "error"));
        provider.onEvent(create(System.currentTimeMillis() - 30000, "event", "realmId2", "clientId", "userId", "127.0.0.1", "error"));

        provider.close();
        provider = factory.create(null);

        provider.clear("realmId");

        Assert.assertEquals(1, provider.createQuery().getResultList().size());
    }

    @Test
    public void clearOld() {
        provider.onEvent(create(System.currentTimeMillis() - 30000, "event", "realmId", "clientId", "userId", "127.0.0.1", "error"));
        provider.onEvent(create(System.currentTimeMillis() - 20000, "event", "realmId", "clientId", "userId", "127.0.0.1", "error"));
        provider.onEvent(create(System.currentTimeMillis(), "event", "realmId", "clientId", "userId", "127.0.0.1", "error"));
        provider.onEvent(create(System.currentTimeMillis(), "event", "realmId", "clientId", "userId", "127.0.0.1", "error"));
        provider.onEvent(create(System.currentTimeMillis() - 30000, "event", "realmId2", "clientId", "userId", "127.0.0.1", "error"));

        provider.close();
        provider = factory.create(null);

        provider.clear("realmId", System.currentTimeMillis() - 10000);

        Assert.assertEquals(3, provider.createQuery().getResultList().size());
    }

    private Event create(String event, String realmId, String clientId, String userId, String ipAddress, String error) {
        return create(System.currentTimeMillis(), event, realmId, clientId, userId, ipAddress, error);
    }

    private Event create(long time, String event, String realmId, String clientId, String userId, String ipAddress, String error) {
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

}
