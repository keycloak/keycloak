package org.keycloak.testsuite.events;

import org.apache.commons.lang3.StringUtils;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
        
        eventStore.clear("realmId");
        eventStore.clear("realmId2");
        
        Assert.assertEquals(0, eventStore.createQuery().getResultList().size());
        
        String d1 = new String("2015-03-04");
        String d2 = new String("2015-03-05");
        String d3 = new String("2015-03-06");
        String d4 = new String("2015-03-07");
        
        String d5 = new String("2015-03-01");
        String d6 = new String("2015-03-03");
        String d7 = new String("2015-03-08");
        String d8 = new String("2015-03-10");
        
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date date1 = null, date2 = null, date3 = null, date4 = null;
        Date date5 = null, date6 = null, date7 = null, date8 = null;
        
        try {
            date1 = formatter.parse(d1);
            date2 = formatter.parse(d2);
            date3 = formatter.parse(d3);
            date4 = formatter.parse(d4);
            
            date5 = formatter.parse(d5);
            date6 = formatter.parse(d6);
            date7 = formatter.parse(d7);
            date8 = formatter.parse(d8);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        eventStore.onEvent(create(date1, EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        eventStore.onEvent(create(date1, EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        eventStore.onEvent(create(date2, EventType.REGISTER, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        eventStore.onEvent(create(date2, EventType.REGISTER, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        eventStore.onEvent(create(date3, EventType.CODE_TO_TOKEN, "realmId", "clientId", "userId2", "127.0.0.1", "error"));
        eventStore.onEvent(create(date3, EventType.LOGOUT, "realmId", "clientId", "userId2", "127.0.0.1", "error"));
        eventStore.onEvent(create(date4, EventType.UPDATE_PROFILE, "realmId2", "clientId2", "userId2", "127.0.0.1", "error"));
        eventStore.onEvent(create(date4, EventType.UPDATE_EMAIL, "realmId2", "clientId2", "userId2", "127.0.0.1", "error"));
        
        resetSession();
        
        Assert.assertEquals(6, eventStore.createQuery().client("clientId").getResultList().size());
        Assert.assertEquals(2, eventStore.createQuery().client("clientId2").getResultList().size());
        
        Assert.assertEquals(6, eventStore.createQuery().realm("realmId").getResultList().size());
        Assert.assertEquals(2, eventStore.createQuery().realm("realmId2").getResultList().size());
        
        Assert.assertEquals(4, eventStore.createQuery().user("userId").getResultList().size());
        Assert.assertEquals(4, eventStore.createQuery().user("userId2").getResultList().size());
        
        Assert.assertEquals(2, eventStore.createQuery().type(EventType.LOGIN).getResultList().size());
        Assert.assertEquals(2, eventStore.createQuery().type(EventType.REGISTER).getResultList().size());
        Assert.assertEquals(4, eventStore.createQuery().type(EventType.LOGIN, EventType.REGISTER).getResultList().size());
        Assert.assertEquals(1, eventStore.createQuery().type(EventType.CODE_TO_TOKEN).getResultList().size());
        Assert.assertEquals(1, eventStore.createQuery().type(EventType.LOGOUT).getResultList().size());
        Assert.assertEquals(1, eventStore.createQuery().type(EventType.UPDATE_PROFILE).getResultList().size());
        Assert.assertEquals(1, eventStore.createQuery().type(EventType.UPDATE_EMAIL).getResultList().size());
        
        Assert.assertEquals(8, eventStore.createQuery().fromDate(date1).getResultList().size());
        Assert.assertEquals(8, eventStore.createQuery().toDate(date4).getResultList().size());
        
        Assert.assertEquals(4, eventStore.createQuery().fromDate(date3).getResultList().size());
        Assert.assertEquals(4, eventStore.createQuery().toDate(date2).getResultList().size());
        
        Assert.assertEquals(0, eventStore.createQuery().fromDate(date7).getResultList().size());
        Assert.assertEquals(0, eventStore.createQuery().toDate(date6).getResultList().size());
        
        Assert.assertEquals(8, eventStore.createQuery().fromDate(date1).toDate(date4).getResultList().size());
        Assert.assertEquals(6, eventStore.createQuery().fromDate(date2).toDate(date4).getResultList().size());
        Assert.assertEquals(4, eventStore.createQuery().fromDate(date1).toDate(date2).getResultList().size());
        Assert.assertEquals(4, eventStore.createQuery().fromDate(date3).toDate(date4).getResultList().size());
        
        Assert.assertEquals(0, eventStore.createQuery().fromDate(date5).toDate(date6).getResultList().size());
        Assert.assertEquals(0, eventStore.createQuery().fromDate(date7).toDate(date8).getResultList().size());
        
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
    public void lengthExceedLimit(){
        eventStore.onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, "realmId", StringUtils.repeat("clientId", 100), "userId", "127.0.0.1", "error"));
        eventStore.onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, StringUtils.repeat("realmId", 100), "clientId", "userId", "127.0.0.1", "error"));
        eventStore.onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, "realmId", "clientId", StringUtils.repeat("userId", 100), "127.0.0.1", "error"));

    }

    @Test
    public void maxLengthWithNull(){
        eventStore.onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, null, null, null, "127.0.0.1", "error"));
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
    
    private Event create(Date date, EventType event, String realmId, String clientId, String userId, String ipAddress, String error) {
        return create(date.getTime(), event, realmId, clientId, userId, ipAddress, error);
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
