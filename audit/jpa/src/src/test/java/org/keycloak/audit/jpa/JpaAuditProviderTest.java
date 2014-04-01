package org.keycloak.audit.jpa;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.audit.AuditProvider;
import org.keycloak.audit.AuditProviderFactory;
import org.keycloak.audit.Event;
import org.keycloak.provider.ProviderFactoryLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JpaAuditProviderTest {

    private AuditProviderFactory factory;
    private AuditProvider provider;

    @Before
    public void before() {
        ProviderFactoryLoader<AuditProviderFactory> loader = ProviderFactoryLoader.load(AuditProviderFactory.class);
        factory = loader.find(JpaAuditProviderFactory.ID);
        factory.init();

        provider = factory.create();
    }

    @After
    public void after() {
        factory.close();
    }

    @Test
    public void save() {
        provider.onEvent(create("event", "realmId", "clientId", "userId", "127.0.0.1", "error"));
    }

    @Test
    public void query() {
        provider.onEvent(create("event", "realmId", "clientId", "userId", "127.0.0.1", "error"));
        provider.onEvent(create("event2", "realmId", "clientId", "userId", "127.0.0.1", "error"));
        provider.onEvent(create("event", "realmId2", "clientId", "userId", "127.0.0.1", "error"));
        provider.onEvent(create("event", "realmId", "clientId2", "userId", "127.0.0.1", "error"));
        provider.onEvent(create("event", "realmId", "clientId", "userId2", "127.0.0.1", "error"));

        Assert.assertEquals(4, provider.createQuery().client("clientId").getResultList().size());
        Assert.assertEquals(4, provider.createQuery().realm("realmId").getResultList().size());
        Assert.assertEquals(4, provider.createQuery().event("event").getResultList().size());
        Assert.assertEquals(4, provider.createQuery().user("userId").getResultList().size());

        Assert.assertEquals(1, provider.createQuery().user("userId").event("event2").getResultList().size());

        Assert.assertEquals(2, provider.createQuery().maxResults(2).getResultList().size());
        Assert.assertEquals(1, provider.createQuery().firstResult(4).getResultList().size());
    }

    private Event create(String event, String realmId, String clientId, String userId, String ipAddress, String error) {
        Event e = new Event();
        e.setTime(System.currentTimeMillis());
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
