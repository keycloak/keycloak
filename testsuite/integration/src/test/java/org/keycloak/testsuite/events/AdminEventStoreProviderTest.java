/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        eventStore.onEvent(create("realmId", OperationType.CREATE, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
    }

    @Test
    public void query() {
        long oldest = System.currentTimeMillis() - 30000;
        long newest = System.currentTimeMillis() + 30000;

        eventStore.onEvent(create("realmId", OperationType.CREATE, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(newest, "realmId", OperationType.ACTION, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(newest, "realmId", OperationType.ACTION, "realmId", "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create("realmId2", OperationType.CREATE, "realmId2", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(oldest, "realmId", OperationType.CREATE, "realmId", "clientId2", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create("realmId", OperationType.CREATE, "realmId", "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);

        resetSession();

        Assert.assertEquals(5, eventStore.createAdminQuery().authClient("clientId").getResultList().size());
        Assert.assertEquals(5, eventStore.createAdminQuery().authRealm("realmId").getResultList().size());
        Assert.assertEquals(4, eventStore.createAdminQuery().operation(OperationType.CREATE).getResultList().size());
        Assert.assertEquals(6, eventStore.createAdminQuery().operation(OperationType.CREATE, OperationType.ACTION).getResultList().size());
        Assert.assertEquals(4, eventStore.createAdminQuery().authUser("userId").getResultList().size());

        Assert.assertEquals(1, eventStore.createAdminQuery().authUser("userId").operation(OperationType.ACTION).getResultList().size());

        Assert.assertEquals(2, eventStore.createAdminQuery().maxResults(2).getResultList().size());
        Assert.assertEquals(1, eventStore.createAdminQuery().firstResult(5).maxResults(5).getResultList().size());

        Assert.assertEquals(newest, eventStore.createAdminQuery().maxResults(1).getResultList().get(0).getTime());
        Assert.assertEquals(oldest, eventStore.createAdminQuery().firstResult(5).maxResults(1).getResultList().get(0).getTime());
        
        eventStore.clearAdmin("realmId");
        eventStore.clearAdmin("realmId2");
        
        Assert.assertEquals(0, eventStore.createAdminQuery().getResultList().size());
        
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
        
        eventStore.onEvent(create(date1, "realmId", OperationType.CREATE, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(date1, "realmId", OperationType.CREATE, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(date2, "realmId", OperationType.ACTION, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(date2, "realmId", OperationType.ACTION, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(date3, "realmId", OperationType.UPDATE, "realmId", "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(date3, "realmId", OperationType.DELETE, "realmId", "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(date4, "realmId2", OperationType.CREATE, "realmId2", "clientId2", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(date4, "realmId2", OperationType.CREATE, "realmId2", "clientId2", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        
        resetSession();
        
        Assert.assertEquals(6, eventStore.createAdminQuery().authClient("clientId").getResultList().size());
        Assert.assertEquals(2, eventStore.createAdminQuery().authClient("clientId2").getResultList().size());
        
        Assert.assertEquals(6, eventStore.createAdminQuery().authRealm("realmId").getResultList().size());
        Assert.assertEquals(2, eventStore.createAdminQuery().authRealm("realmId2").getResultList().size());
        
        Assert.assertEquals(4, eventStore.createAdminQuery().authUser("userId").getResultList().size());
        Assert.assertEquals(4, eventStore.createAdminQuery().authUser("userId2").getResultList().size());
        
        Assert.assertEquals(2, eventStore.createAdminQuery().operation(OperationType.ACTION).getResultList().size());
        Assert.assertEquals(6, eventStore.createAdminQuery().operation(OperationType.CREATE, OperationType.ACTION).getResultList().size());
        Assert.assertEquals(1, eventStore.createAdminQuery().operation(OperationType.UPDATE).getResultList().size());
        Assert.assertEquals(1, eventStore.createAdminQuery().operation(OperationType.DELETE).getResultList().size());
        Assert.assertEquals(4, eventStore.createAdminQuery().operation(OperationType.CREATE).getResultList().size());
        
        Assert.assertEquals(8, eventStore.createAdminQuery().fromTime(date1).getResultList().size());
        Assert.assertEquals(8, eventStore.createAdminQuery().toTime(date4).getResultList().size());
        
        Assert.assertEquals(4, eventStore.createAdminQuery().fromTime(date3).getResultList().size());
        Assert.assertEquals(4, eventStore.createAdminQuery().toTime(date2).getResultList().size());
        
        Assert.assertEquals(0, eventStore.createAdminQuery().fromTime(date7).getResultList().size());
        Assert.assertEquals(0, eventStore.createAdminQuery().toTime(date6).getResultList().size());
        
        Assert.assertEquals(8, eventStore.createAdminQuery().fromTime(date1).toTime(date4).getResultList().size());
        Assert.assertEquals(6, eventStore.createAdminQuery().fromTime(date2).toTime(date4).getResultList().size());
        Assert.assertEquals(4, eventStore.createAdminQuery().fromTime(date1).toTime(date2).getResultList().size());
        Assert.assertEquals(4, eventStore.createAdminQuery().fromTime(date3).toTime(date4).getResultList().size());
        
        Assert.assertEquals(0, eventStore.createAdminQuery().fromTime(date5).toTime(date6).getResultList().size());
        Assert.assertEquals(0, eventStore.createAdminQuery().fromTime(date7).toTime(date8).getResultList().size());
        
    }
    
    @Test
    public void queryResourcePath() {
        long oldest = System.currentTimeMillis() - 30000;
        long newest = System.currentTimeMillis() + 30000;

        eventStore.onEvent(create("realmId", OperationType.CREATE, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(newest, "realmId", OperationType.ACTION, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(newest, "realmId", OperationType.ACTION, "realmId", "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create("realmId2", OperationType.CREATE, "realmId2", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(oldest, "realmId", OperationType.CREATE, "realmId", "clientId2", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create("realmId", OperationType.CREATE, "realmId", "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);

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
        eventStore.onEvent(create(System.currentTimeMillis() - 30000, "realmId", OperationType.CREATE, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(System.currentTimeMillis() - 20000, "realmId", OperationType.CREATE, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(System.currentTimeMillis(), "realmId", OperationType.CREATE, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(System.currentTimeMillis(), "realmId", OperationType.CREATE, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(System.currentTimeMillis() - 30000, "realmId2", OperationType.CREATE, "realmId2", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);

        resetSession();

        eventStore.clearAdmin("realmId");

        Assert.assertEquals(1, eventStore.createAdminQuery().getResultList().size());
    }

    @Test
    public void clearOld() {
        eventStore.onEvent(create(System.currentTimeMillis() - 30000, "realmId", OperationType.CREATE, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(System.currentTimeMillis() - 20000, "realmId", OperationType.CREATE, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(System.currentTimeMillis(), "realmId", OperationType.CREATE, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(System.currentTimeMillis(), "realmId", OperationType.CREATE, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventStore.onEvent(create(System.currentTimeMillis() - 30000, "realmId", OperationType.CREATE, "realmId", "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);

        resetSession();

        eventStore.clearAdmin("realmId", System.currentTimeMillis() - 10000);

        Assert.assertEquals(2, eventStore.createAdminQuery().getResultList().size());
    }

    private AdminEvent create(String realmId, OperationType operation, String authRealmId, String authClientId, String authUserId, String authIpAddress, String resourcePath, String error) {
        return create(System.currentTimeMillis(), realmId, operation, authRealmId, authClientId, authUserId, authIpAddress, resourcePath, error);
    }
    
    private AdminEvent create(Date date, String realmId, OperationType operation, String authRealmId, String authClientId, String authUserId, String authIpAddress, String resourcePath, String error) {
        return create(date.getTime(), realmId, operation, authRealmId, authClientId, authUserId, authIpAddress, resourcePath, error);
    }

    private AdminEvent create(long time, String realmId, OperationType operation, String authRealmId, String authClientId, String authUserId, String authIpAddress, String resourcePath, String error) {
        AdminEvent e = new AdminEvent();
        e.setTime(time);
        e.setRealmId(realmId);
        e.setOperationType(operation);
        AuthDetails authDetails = new AuthDetails();
        authDetails.setRealmId(authRealmId);
        authDetails.setClientId(authClientId);
        authDetails.setUserId(authUserId);
        authDetails.setIpAddress(authIpAddress);
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
