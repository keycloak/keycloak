/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.utils;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;

import org.junit.Assert;
import org.junit.Test;

/**
 * <p>Class to perform unit tests of the SessionExpirationUtils class.</p>
 *
 * @author rmartinc
 */
public class SessionExpirationUtilsTest {

    private static final Map<String, Object> realmMap = new HashMap<>();
    private static final Map<String, Object> clientMap = new HashMap<>();
    private static final Map<String, Object> orgMap = new HashMap<>();
    private static final Map<String, Object> userSessionMap = new HashMap<>();
    private static final RealmModel realm = createRealm();
    private static final ClientModel client = createClient();
    private static final OrganizationModel org = createOrg();
    private static final UserSessionModel userSession = createUserSession();

    private static RealmModel createRealm() {
        RealmModel realmModel = (RealmModel) Proxy.newProxyInstance(SessionExpirationUtilsTest.class.getClassLoader(),
                new Class[]{RealmModel.class}, (proxy, method, args) -> {

            Object result = realmMap.get(method.getName());
            if (result != null) {
                return result;
            }
            throw new UnsupportedOperationException("Realm method not in map: " + method.getName());
        });
        return realmModel;
    }

    private static ClientModel createClient() {
        ClientModel clientModel = (ClientModel) Proxy.newProxyInstance(SessionExpirationUtilsTest.class.getClassLoader(),
                new Class[]{ClientModel.class}, (proxy, method, args) -> {

            if ("getAttribute".equals(method.getName()) && args.length == 1) {
                return clientMap.get((String) args[0]);
            }
            throw new UnsupportedOperationException("Client method not in map: " + method.getName());
        });
        return clientModel;
    }

    private static OrganizationModel createOrg() {
        return (OrganizationModel) Proxy.newProxyInstance(SessionExpirationUtilsTest.class.getClassLoader(),
                new Class[]{OrganizationModel.class}, (proxy, method, args) -> {
            Object result = orgMap.get(method.getName());
            if (result != null) {
                return result;
            }
            throw new UnsupportedOperationException("Org method not in map: " + method.getName());
        });
    }
    
    private static UserModel createUser() {
        return (UserModel) Proxy.newProxyInstance(SessionExpirationUtilsTest.class.getClassLoader(),
                new Class[]{UserModel.class}, (proxy, method, args) -> null);
    }
    
    private static UserSessionModel createUserSession() {
        UserModel user = createUser();
        return (UserSessionModel) Proxy.newProxyInstance(SessionExpirationUtilsTest.class.getClassLoader(),
                new Class[]{UserSessionModel.class}, (proxy, method, args) -> {
            if ("getUser".equals(method.getName())) return user;
            Object result = userSessionMap.get(method.getName());
            if (result != null) return result;
            throw new UnsupportedOperationException("UserSession method not in map: " + method.getName());
        });
    }
    
    private static KeycloakSession createKeycloakSession(OrganizationModel orgToReturn) {
        return (KeycloakSession) Proxy.newProxyInstance(SessionExpirationUtilsTest.class.getClassLoader(),
                new Class[]{KeycloakSession.class}, (proxy, method, args) -> {
            if ("getProvider".equals(method.getName()) && args[0] == OrganizationProvider.class) {
                return createOrgProvider(orgToReturn);
            }
            throw new UnsupportedOperationException("KeycloakSession method not in map: " + method.getName());
        });
    }
    
    private static OrganizationProvider createOrgProvider(OrganizationModel orgToReturn) {
        return (OrganizationProvider) Proxy.newProxyInstance(SessionExpirationUtilsTest.class.getClassLoader(),
                new Class[]{OrganizationProvider.class}, (proxy, method, args) -> {
            if ("getByMember".equals(method.getName())) {
                return orgToReturn != null ? Stream.of(orgToReturn) : Stream.empty();
            }
            throw new UnsupportedOperationException("OrganizationProvider method not in map: " + method.getName());
        });
    }

    private static void resetRealm() {
        realmMap.put("isOfflineSessionMaxLifespanEnabled", false);
        realmMap.put("getOfflineSessionMaxLifespan", 0);
        realmMap.put("getOfflineSessionIdleTimeout", 0);
        realmMap.put("getClientOfflineSessionMaxLifespan", 0);
        realmMap.put("getClientOfflineSessionIdleTimeout", 0);
        realmMap.put("getSsoSessionMaxLifespan", 0);
        realmMap.put("getSsoSessionIdleTimeout", 0);
        realmMap.put("getClientSessionMaxLifespan", 0);
        realmMap.put("getClientSessionIdleTimeout", 0);
        realmMap.put("getSsoSessionMaxLifespanRememberMe", 0);
        realmMap.put("getSsoSessionIdleTimeoutRememberMe", 0);
    }

    private static void resetClient() {
        clientMap.clear();
    }

    private static void resetOrg() {
        orgMap.put("getSessionMaxLifespan", 0);
        orgMap.put("getSessionIdleTimeout", 0);
        orgMap.put("getSessionMaxLifespanRememberMe", 0);
        orgMap.put("getSessionIdleTimeoutRememberMe", 0);
    }

    @Test
    public void testCalculateUserSessionMaxLifespanTimestampOnline() {
        long t = Time.currentTimeMillis();
        resetRealm();

        // non valid lifespan 0 or negative is default 36000
        Assert.assertEquals(Constants.DEFAULT_SESSION_MAX_LIFESPAN * 1000L,
                SessionExpirationUtils.calculateUserSessionMaxLifespanTimestamp(false, false, t, realm) - t);
        // normal lifespan to 1000s
        realmMap.put("getSsoSessionMaxLifespan", 1000);
        Assert.assertEquals(1000 * 1000L, SessionExpirationUtils.calculateUserSessionMaxLifespanTimestamp(false, false, t, realm) - t);
        // use remember me
        realmMap.put("getSsoSessionMaxLifespanRememberMe", 2000);
        Assert.assertEquals(2000 * 1000L, SessionExpirationUtils.calculateUserSessionMaxLifespanTimestamp(false, true, t, realm) - t);
    }

    @Test
    public void testCalculateUserSessionMaxLifespanTimestampOffline() {
        long t = Time.currentTimeMillis();
        resetRealm();

        // not activated expiration for offline
        Assert.assertEquals(-1L, SessionExpirationUtils.calculateUserSessionMaxLifespanTimestamp(true, false, t, realm));
        // activate and 0 should be default
        realmMap.put("isOfflineSessionMaxLifespanEnabled", true);
        Assert.assertEquals(Constants.DEFAULT_OFFLINE_SESSION_MAX_LIFESPAN * 1000L,
                SessionExpirationUtils.calculateUserSessionMaxLifespanTimestamp(true, false, t, realm) - t);
        // normal lifespan 2000
        realmMap.put("getOfflineSessionMaxLifespan", 2000);
        Assert.assertEquals(2000 * 1000L,
                SessionExpirationUtils.calculateUserSessionMaxLifespanTimestamp(true, false, t, realm) - t);
        // remember me does not affect offline
        realmMap.put("getSsoSessionMaxLifespanRememberMe", 4000);
        Assert.assertEquals(2000 * 1000L,
                SessionExpirationUtils.calculateUserSessionMaxLifespanTimestamp(true, true, t, realm) - t);
    }

    @Test
    public void testCalculateUserSessionIdleTimestampOnline() {
        long t = Time.currentTimeMillis();
        resetRealm();

        // non valid, default value
        Assert.assertEquals(Constants.DEFAULT_SESSION_IDLE_TIMEOUT * 1000L,
                SessionExpirationUtils.calculateUserSessionIdleTimestamp(false, false, t, realm) - t);
        // normal value 2000s
        realmMap.put("getSsoSessionIdleTimeout", 1000);
        Assert.assertEquals(1000 * 1000L, SessionExpirationUtils.calculateUserSessionIdleTimestamp(false, false, t, realm) - t);
        // use bigger remember me
        realmMap.put("getSsoSessionIdleTimeoutRememberMe", 2000);
        Assert.assertEquals(2000 * 1000L, SessionExpirationUtils.calculateUserSessionIdleTimestamp(false, true, t, realm) - t);
    }

    @Test
    public void testCalculateUserSessionIdleTimestampOffline() {
        long t = Time.currentTimeMillis();
        resetRealm();

        // non valid, default value
        Assert.assertEquals(Constants.DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT * 1000L,
                SessionExpirationUtils.calculateUserSessionIdleTimestamp(true, false, t, realm) - t);
        // normal value 2000s
        realmMap.put("getOfflineSessionIdleTimeout", 1000);
        Assert.assertEquals(1000 * 1000L, SessionExpirationUtils.calculateUserSessionIdleTimestamp(true, false, t, realm) - t);
        // use bigger remember me does not affect
        realmMap.put("getSsoSessionIdleTimeoutRememberMe", 2000);
        Assert.assertEquals(1000 * 1000L, SessionExpirationUtils.calculateUserSessionIdleTimestamp(true, true, t, realm) - t);
    }

    @Test
    public void testCalculateClientSessionMaxLifespanTimestampOnline() {
        long t = Time.currentTimeMillis();
        resetRealm();
        resetClient();

        // default
        Assert.assertEquals(Constants.DEFAULT_SESSION_MAX_LIFESPAN * 1000L,
                SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(false, false, t, t, realm, client) - t);
        // normal value in realm
        realmMap.put("getSsoSessionMaxLifespan", 5000);
        Assert.assertEquals(5000 * 1000L, SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(false, false, t, t, realm, client) - t);
        // use remember me
        realmMap.put("getSsoSessionMaxLifespanRememberMe", 6000);
        Assert.assertEquals(6000 * 1000L, SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(false, true, t, t, realm, client) - t);
        // override client value in realm
        realmMap.put("getClientSessionMaxLifespan", 4000);
        Assert.assertEquals(4000 * 1000L, SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(false, false, t, t, realm, client) - t);
        // override value in client
        clientMap.put(OIDCConfigAttributes.CLIENT_SESSION_MAX_LIFESPAN, "3000");
        Assert.assertEquals(3000 * 1000L, SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(false, false, t, t, realm, client) - t);
        // client max lifespan cannot be bigger than user lifespan
        realmMap.put("getSsoSessionMaxLifespan", 2000);
        Assert.assertEquals(2000 * 1000L, SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(false, false, t, t, realm, client) - t);
        // the same but using remember me
        realmMap.put("getSsoSessionMaxLifespan", 1000);
        realmMap.put("getSsoSessionMaxLifespanRememberMe", 2000);
        Assert.assertEquals(2000 * 1000L, SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(false, true, t, t, realm, client) - t);
        // set -1 in the client and should be not taken into account
        clientMap.put(OIDCConfigAttributes.CLIENT_SESSION_MAX_LIFESPAN, "-1");
        Assert.assertEquals(2000 * 1000L, SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(false, true, t, t, realm, client) - t);
    }

    @Test
    public void testCalculateClientSessionMaxLifespanTimestampOffline() {
        long t = Time.currentTimeMillis();
        resetRealm();
        resetClient();

        // no expiration
        Assert.assertEquals(-1, SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(true, false, t, t, realm, client));
        // default
        realmMap.put("isOfflineSessionMaxLifespanEnabled", true);
        Assert.assertEquals(Constants.DEFAULT_OFFLINE_SESSION_MAX_LIFESPAN * 1000L,
                SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(true, false, t, t, realm, client) - t);
        // normal value in realm
        realmMap.put("getOfflineSessionMaxLifespan", 5000);
        Assert.assertEquals(5000 * 1000L, SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(true, false, t, t, realm, client) - t);
        // override client value in realm
        realmMap.put("getClientOfflineSessionMaxLifespan", 4000);
        Assert.assertEquals(4000 * 1000L, SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(true, false, t, t, realm, client) - t);
        // override value in client
        clientMap.put(OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_MAX_LIFESPAN, "3000");
        Assert.assertEquals(3000 * 1000L, SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(true, false, t, t, realm, client) - t);
        // client max lifespan cannot be bigger than user lifespan
        long t2 = t - 100;
        realmMap.put("getOfflineSessionMaxLifespan", 2000);
        Assert.assertEquals(2000 * 1000L, SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(true, false, t, t2, realm, client) - t2);
        // set -1 in the client and should be not taken into account
        clientMap.put(OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_MAX_LIFESPAN, "-1");
        Assert.assertEquals(2000 * 1000L, SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(true, false, t, t, realm, client) - t);
        // set no expiration at realm but set expiration at client level
        realmMap.put("isOfflineSessionMaxLifespanEnabled", false);
        clientMap.put(OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_MAX_LIFESPAN, "2000");
        Assert.assertEquals(2000 * 1000L, SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(true, false, t, t, realm, client) - t);
    }

    @Test
    public void testCalculateClientSessionIdleTimestampOnline() {
        long t = Time.currentTimeMillis();
        resetRealm();
        resetClient();

        // default
        Assert.assertEquals(Constants.DEFAULT_SESSION_IDLE_TIMEOUT * 1000L,
                SessionExpirationUtils.calculateClientSessionIdleTimestamp(false, false, t, realm, client) - t);
        // normal value in realm
        realmMap.put("getSsoSessionIdleTimeout", 5000);
        Assert.assertEquals(5000 * 1000L, SessionExpirationUtils.calculateClientSessionIdleTimestamp(false, false, t, realm, client) - t);
        // use remember me
        realmMap.put("getSsoSessionIdleTimeoutRememberMe", 6000);
        Assert.assertEquals(6000 * 1000L, SessionExpirationUtils.calculateClientSessionIdleTimestamp(false, true, t, realm, client) - t);
        // override client value in realm
        realmMap.put("getClientSessionIdleTimeout", 4000);
        Assert.assertEquals(4000 * 1000L, SessionExpirationUtils.calculateClientSessionIdleTimestamp(false, false, t, realm, client) - t);
        // override value in client
        clientMap.put(OIDCConfigAttributes.CLIENT_SESSION_IDLE_TIMEOUT, "3000");
        Assert.assertEquals(3000 * 1000L, SessionExpirationUtils.calculateClientSessionIdleTimestamp(false, false, t, realm, client) - t);
        // set -1 in the client and should be not taken into account
        clientMap.put(OIDCConfigAttributes.CLIENT_SESSION_IDLE_TIMEOUT, "-1");
        Assert.assertEquals(4000 * 1000L, SessionExpirationUtils.calculateClientSessionIdleTimestamp(false, false, t, realm, client) - t);
    }

    @Test
    public void testCalculateClientSessionIdleTimestampOffline() {
        long t = Time.currentTimeMillis();
        resetRealm();
        resetClient();

        // default
        Assert.assertEquals(Constants.DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT * 1000L,
                SessionExpirationUtils.calculateClientSessionIdleTimestamp(true, false, t, realm, client) - t);
        // normal value in realm
        realmMap.put("getOfflineSessionIdleTimeout", 5000);
        Assert.assertEquals(5000 * 1000L, SessionExpirationUtils.calculateClientSessionIdleTimestamp(true, false, t, realm, client) - t);
        // use remember me does not affect
        realmMap.put("getSsoSessionIdleTimeoutRememberMe", 6000);
        Assert.assertEquals(5000 * 1000L, SessionExpirationUtils.calculateClientSessionIdleTimestamp(true, true, t, realm, client) - t);
        // override client value in realm
        realmMap.put("getClientOfflineSessionIdleTimeout", 4000);
        Assert.assertEquals(4000 * 1000L, SessionExpirationUtils.calculateClientSessionIdleTimestamp(true, false, t, realm, client) - t);
        // override value in client
        clientMap.put(OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT, "3000");
        Assert.assertEquals(3000 * 1000L, SessionExpirationUtils.calculateClientSessionIdleTimestamp(true, false, t, realm, client) - t);
        // set -1 in the client and should be not taken into account
        clientMap.put(OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT, "-1");
        Assert.assertEquals(4000 * 1000L, SessionExpirationUtils.calculateClientSessionIdleTimestamp(true, false, t, realm, client) - t);
    }

    @Test
    public void testGetSsoSessionMaxLifespan_noOrg_returnsRealmDefault() {
        resetRealm();
        realmMap.put("getSsoSessionMaxLifespan", 36000);
        KeycloakSession session = createKeycloakSession(null);
        Assert.assertEquals(36000, SessionExpirationUtils.getSsoSessionMaxLifespan(realm, userSession, session));
    }
    
    @Test
    public void testGetSsoSessionMaxLifespan_orgWithPositiveValue_returnsOrgValue() {
        resetRealm();
        resetOrg();
        realmMap.put("getSsoSessionMaxLifespan", 36000);
        orgMap.put("getSessionMaxLifespan", 3600);
        KeycloakSession session = createKeycloakSession(org);
        Assert.assertEquals(3600, SessionExpirationUtils.getSsoSessionMaxLifespan(realm, userSession, session));
    }
    
    @Test
    public void testGetSsoSessionMaxLifespan_orgWithZeroValue_returnsRealmDefault() {
        resetRealm();
        resetOrg();
        realmMap.put("getSsoSessionMaxLifespan", 36000);
        orgMap.put("getSessionMaxLifespan", 0);
        KeycloakSession session = createKeycloakSession(org);
        Assert.assertEquals(36000, SessionExpirationUtils.getSsoSessionMaxLifespan(realm, userSession, session));
    }
    
    @Test
    public void testGetSsoSessionMaxLifespan_nullUserSession_returnsRealmDefault() {
        resetRealm();
        realmMap.put("getSsoSessionMaxLifespan", 36000);
        KeycloakSession session = createKeycloakSession(null);
        Assert.assertEquals(36000, SessionExpirationUtils.getSsoSessionMaxLifespan(realm, null, session));
    }
    
    @Test
    public void testGetSsoSessionIdleTimeout_noOrg_returnsRealmDefault() {
        resetRealm();
        realmMap.put("getSsoSessionIdleTimeout", 1800);
        KeycloakSession session = createKeycloakSession(null);
        Assert.assertEquals(1800, SessionExpirationUtils.getSsoSessionIdleTimeout(realm, userSession, session));
    }
    
    @Test
    public void testGetSsoSessionIdleTimeout_orgWithPositiveValue_returnsOrgValue() {
        resetRealm();
        resetOrg();
        realmMap.put("getSsoSessionIdleTimeout", 1800);
        orgMap.put("getSessionIdleTimeout", 900);
        KeycloakSession session = createKeycloakSession(org);
        Assert.assertEquals(900, SessionExpirationUtils.getSsoSessionIdleTimeout(realm, userSession, session));
    }
    
    @Test
    public void testGetSsoSessionIdleTimeout_orgWithZeroValue_returnsRealmDefault() {
        resetRealm();
        resetOrg();
        realmMap.put("getSsoSessionIdleTimeout", 1800);
        orgMap.put("getSessionIdleTimeout", 0);
        KeycloakSession session = createKeycloakSession(org);
        Assert.assertEquals(1800, SessionExpirationUtils.getSsoSessionIdleTimeout(realm, userSession, session));
    }
    
    @Test
    public void testGetSsoSessionMaxLifespanRememberMe_noOrg_returnsRealmDefault() {
        resetRealm();
        realmMap.put("getSsoSessionMaxLifespanRememberMe", 72000);
        KeycloakSession session = createKeycloakSession(null);
        Assert.assertEquals(72000, SessionExpirationUtils.getSsoSessionMaxLifespanRememberMe(realm, userSession, session));
    }
    
    @Test
    public void testGetSsoSessionMaxLifespanRememberMe_orgWithPositiveValue_returnsOrgValue() {
        resetRealm();
        resetOrg();
        realmMap.put("getSsoSessionMaxLifespanRememberMe", 72000);
        orgMap.put("getSessionMaxLifespanRememberMe", 14400);
        KeycloakSession session = createKeycloakSession(org);
        Assert.assertEquals(14400, SessionExpirationUtils.getSsoSessionMaxLifespanRememberMe(realm, userSession, session));
    }
    
    @Test
    public void testGetSsoSessionMaxLifespanRememberMe_orgWithZeroValue_returnsRealmDefault() {
        resetRealm();
        resetOrg();
        realmMap.put("getSsoSessionMaxLifespanRememberMe", 72000);
        orgMap.put("getSessionMaxLifespanRememberMe", 0);
        KeycloakSession session = createKeycloakSession(org);
        Assert.assertEquals(72000, SessionExpirationUtils.getSsoSessionMaxLifespanRememberMe(realm, userSession, session));
    }
    
    @Test
    public void testGetSsoSessionIdleTimeoutRememberMe_noOrg_returnsRealmDefault() {
        resetRealm();
        realmMap.put("getSsoSessionIdleTimeoutRememberMe", 7200);
        KeycloakSession session = createKeycloakSession(null);
        Assert.assertEquals(7200, SessionExpirationUtils.getSsoSessionIdleTimeoutRememberMe(realm, userSession, session));
    }
    
    @Test
    public void testGetSsoSessionIdleTimeoutRememberMe_orgWithPositiveValue_returnsOrgValue() {
        resetRealm();
        resetOrg();
        realmMap.put("getSsoSessionIdleTimeoutRememberMe", 7200);
        orgMap.put("getSessionIdleTimeoutRememberMe", 3600);
        KeycloakSession session = createKeycloakSession(org);
        Assert.assertEquals(3600, SessionExpirationUtils.getSsoSessionIdleTimeoutRememberMe(realm, userSession, session));
    }
    
    @Test
    public void testGetSsoSessionIdleTimeoutRememberMe_orgWithZeroValue_returnsRealmDefault() {
        resetRealm();
        resetOrg();
        realmMap.put("getSsoSessionIdleTimeoutRememberMe", 7200);
        orgMap.put("getSessionIdleTimeoutRememberMe", 0);
        KeycloakSession session = createKeycloakSession(org);
        Assert.assertEquals(7200, SessionExpirationUtils.getSsoSessionIdleTimeoutRememberMe(realm, userSession, session));
    }
}
