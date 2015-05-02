package org.keycloak.testsuite.events;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.testsuite.rule.KeycloakRule;

/**
 * @author <a href="mailto:giriraj.sharma27@gmail.com">Giriraj Sharma</a>
 */
public class AdminEventStoreProviderTest {

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
        eventStore.clearAdmin();
        kc.stopSession(session, true);
    }

    @Test
    public void save() {
        eventStore.onEvent(create(OperationType.VIEW, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
    }

    @Test
    public void query() {
        long oldest = System.currentTimeMillis() - 30000;
        long newest = System.currentTimeMillis() + 30000;

        eventStore.onEvent(create(OperationType.VIEW, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(newest, OperationType.ACTION, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(newest, OperationType.ACTION, "realmId", "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(OperationType.VIEW, "realmId2", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(oldest, OperationType.VIEW, "realmId", "clientId2", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(OperationType.VIEW, "realmId", "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);

        resetSession();

        Assert.assertEquals(5, eventStore.createAdminQuery().authClient("clientId").getResultList().size());
        Assert.assertEquals(5, eventStore.createAdminQuery().authRealm("realmId").getResultList().size());
        Assert.assertEquals(4, eventStore.createAdminQuery().operation(OperationType.VIEW).getResultList().size());
        Assert.assertEquals(6, eventStore.createAdminQuery().operation(OperationType.VIEW, OperationType.ACTION).getResultList().size());
        Assert.assertEquals(4, eventStore.createAdminQuery().authUser("userId").getResultList().size());

        Assert.assertEquals(1, eventStore.createAdminQuery().authUser("userId").operation(OperationType.ACTION).getResultList().size());

        Assert.assertEquals(2, eventStore.createAdminQuery().maxResults(2).getResultList().size());
        Assert.assertEquals(1, eventStore.createAdminQuery().firstResult(5).getResultList().size());

        Assert.assertEquals(newest, eventStore.createAdminQuery().maxResults(1).getResultList().get(0).getTime());
        Assert.assertEquals(oldest, eventStore.createAdminQuery().firstResult(5).maxResults(1).getResultList().get(0).getTime());
        
        eventStore.clearAdmin("realmId");
        eventStore.clearAdmin("realmId2");
        
        Assert.assertEquals(0, eventStore.createAdminQuery().getResultList().size());
        
        String d1 = new String("2015-03-04");
        String d2 = new String("2015-03-05");
        String d3 = new String("2015-03-06");
        String d4 = new String("2015-03-07");
        
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date date1 = null, date2 = null, date3 = null, date4 = null;
        
        try {
            date1 = formatter.parse(d1);
            date2 = formatter.parse(d2);
            date3 = formatter.parse(d3);
            date4 = formatter.parse(d4);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        eventStore.onEvent(create(date1, OperationType.VIEW, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(date1, OperationType.VIEW, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(date2, OperationType.ACTION, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(date2, OperationType.ACTION, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(date3, OperationType.UPDATE, "realmId", "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(date3, OperationType.DELETE, "realmId", "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(date4, OperationType.CREATE, "realmId2", "clientId2", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(date4, OperationType.CREATE, "realmId2", "clientId2", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        
        resetSession();
        
        Assert.assertEquals(6, eventStore.createAdminQuery().authClient("clientId").getResultList().size());
        Assert.assertEquals(2, eventStore.createAdminQuery().authClient("clientId2").getResultList().size());
        
        Assert.assertEquals(6, eventStore.createAdminQuery().authRealm("realmId").getResultList().size());
        Assert.assertEquals(2, eventStore.createAdminQuery().authRealm("realmId2").getResultList().size());
        
        Assert.assertEquals(4, eventStore.createAdminQuery().authUser("userId").getResultList().size());
        Assert.assertEquals(4, eventStore.createAdminQuery().authUser("userId2").getResultList().size());
        
        Assert.assertEquals(2, eventStore.createAdminQuery().operation(OperationType.VIEW).getResultList().size());
        Assert.assertEquals(2, eventStore.createAdminQuery().operation(OperationType.ACTION).getResultList().size());
        Assert.assertEquals(4, eventStore.createAdminQuery().operation(OperationType.VIEW, OperationType.ACTION).getResultList().size());
        Assert.assertEquals(1, eventStore.createAdminQuery().operation(OperationType.UPDATE).getResultList().size());
        Assert.assertEquals(1, eventStore.createAdminQuery().operation(OperationType.DELETE).getResultList().size());
        Assert.assertEquals(2, eventStore.createAdminQuery().operation(OperationType.CREATE).getResultList().size());
        
        Assert.assertEquals(8, eventStore.createAdminQuery().fromTime("2015-03-04").getResultList().size());
        Assert.assertEquals(8, eventStore.createAdminQuery().toTime("2015-03-07").getResultList().size());
        
        Assert.assertEquals(4, eventStore.createAdminQuery().fromTime("2015-03-06").getResultList().size());
        Assert.assertEquals(4, eventStore.createAdminQuery().toTime("2015-03-05").getResultList().size());
        
        Assert.assertEquals(0, eventStore.createAdminQuery().fromTime("2015-03-08").getResultList().size());
        Assert.assertEquals(0, eventStore.createAdminQuery().toTime("2015-03-03").getResultList().size());
        
        Assert.assertEquals(8, eventStore.createAdminQuery().fromTime("2015-03-04").toTime("2015-03-07").getResultList().size());
        Assert.assertEquals(6, eventStore.createAdminQuery().fromTime("2015-03-05").toTime("2015-03-07").getResultList().size());
        Assert.assertEquals(4, eventStore.createAdminQuery().fromTime("2015-03-04").toTime("2015-03-05").getResultList().size());
        Assert.assertEquals(4, eventStore.createAdminQuery().fromTime("2015-03-06").toTime("2015-03-07").getResultList().size());
        
        Assert.assertEquals(0, eventStore.createAdminQuery().fromTime("2015-03-01").toTime("2015-03-03").getResultList().size());
        Assert.assertEquals(0, eventStore.createAdminQuery().fromTime("2015-03-08").toTime("2015-03-10").getResultList().size());
        
    }
    
    @Test
    public void queryResourcePath() {
        long oldest = System.currentTimeMillis() - 30000;
        long newest = System.currentTimeMillis() + 30000;

        eventStore.onEvent(create(OperationType.VIEW, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(newest, OperationType.ACTION, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(newest, OperationType.ACTION, "realmId", "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(OperationType.VIEW, "realmId2", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(oldest, OperationType.VIEW, "realmId", "clientId2", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(OperationType.VIEW, "realmId", "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);

        resetSession();

        Assert.assertEquals(6, eventStore.createAdminQuery().resourcePath("/admin").getResultList().size());
        Assert.assertEquals(6, eventStore.createAdminQuery().resourcePath("/realms").getResultList().size());
        Assert.assertEquals(6, eventStore.createAdminQuery().resourcePath("/master").getResultList().size());
        Assert.assertEquals(6, eventStore.createAdminQuery().resourcePath("/admin/realms").getResultList().size());
        Assert.assertEquals(6, eventStore.createAdminQuery().resourcePath("/realms/master").getResultList().size());
        Assert.assertEquals(6, eventStore.createAdminQuery().resourcePath("/admin/realms/master").getResultList().size());
    }
    
    @Test
    public void clear() {
        eventStore.onEvent(create(System.currentTimeMillis() - 30000, OperationType.VIEW, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(System.currentTimeMillis() - 20000, OperationType.VIEW, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(System.currentTimeMillis(), OperationType.VIEW, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(System.currentTimeMillis(), OperationType.VIEW, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(System.currentTimeMillis() - 30000, OperationType.VIEW, "realmId2", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);

        resetSession();

        eventStore.clearAdmin("realmId");

        Assert.assertEquals(1, eventStore.createAdminQuery().getResultList().size());
    }

    @Test
    public void clearOld() {
        eventStore.onEvent(create(System.currentTimeMillis() - 30000, OperationType.VIEW, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(System.currentTimeMillis() - 20000, OperationType.VIEW, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(System.currentTimeMillis(), OperationType.VIEW, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(System.currentTimeMillis(), OperationType.VIEW, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(System.currentTimeMillis() - 30000, OperationType.VIEW, "realmId2", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);

        resetSession();

        eventStore.clearAdmin("realmId", System.currentTimeMillis() - 10000);

        Assert.assertEquals(3, eventStore.createAdminQuery().getResultList().size());
    }

    private AdminEvent create(OperationType operation, String realmId, String clientId, String userId, String ipAddress, String resourcePath, String error) {
        return create(System.currentTimeMillis(), operation, realmId, clientId, userId, ipAddress, resourcePath, error);
    }
    
    private AdminEvent create(Date date, OperationType operation, String realmId, String clientId, String userId, String ipAddress, String resourcePath, String error) {
        return create(date.getTime(), operation, realmId, clientId, userId, ipAddress, resourcePath, error);
    }

    private AdminEvent create(long time, OperationType operation, String realmId, String clientId, String userId, String ipAddress, String resourcePath, String error) {
        AdminEvent e = new AdminEvent();
        e.setTime(time);
        e.setOperationType(operation);
        AuthDetails authDetails = new AuthDetails();
        authDetails.setRealmId(realmId);
        authDetails.setClientId(clientId);
        authDetails.setUserId(userId);
        authDetails.setIpAddress(ipAddress);
        e.setAuthDetails(authDetails);
        e.setResourcePath(resourcePath);
        e.setError(error);

        return e;
    }

    private void resetSession() {
        kc.stopSession(session, true);
        session = kc.startSession();
        eventStore = session.getProvider(EventStoreProvider.class);
    }

}
