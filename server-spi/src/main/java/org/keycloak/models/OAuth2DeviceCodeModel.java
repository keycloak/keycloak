/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models;

import org.keycloak.common.util.Time;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 */
public class OAuth2DeviceCodeModel {

    private static final String CLIENT_ID = "cid";
    private static final String EXPIRATION_NOTE = "exp";
    private static final String POLLING_INTERVAL_NOTE = "int";
    private static final String NONCE_NOTE = "nonce";
    private static final String SCOPE_NOTE = "scope";
    private static final String USER_SESSION_ID_NOTE = "uid";
    private static final String DENIED_NOTE = "denied";

    private final RealmModel realm;
    private final String clientId;
    private final String deviceCode;
    private final int expiration;
    private final int pollingInterval;
    private final String scope;
    private final String nonce;
    private final String userSessionId;
    private final boolean denied;

    public static OAuth2DeviceCodeModel create(RealmModel realm, ClientModel client,
                                               String deviceCode, String scope, String nonce) {
        int expiresIn = realm.getOAuth2DeviceCodeLifespan();
        int expiration = Time.currentTime() + expiresIn;
        int pollingInterval = realm.getOAuth2DevicePollingInterval();
        return new OAuth2DeviceCodeModel(realm, client.getClientId(), deviceCode, scope, nonce, expiration, pollingInterval,  null, false);
    }

    public OAuth2DeviceCodeModel approve(String userSessionId) {
        return new OAuth2DeviceCodeModel(realm, clientId, deviceCode, scope, nonce, expiration,  pollingInterval, userSessionId, false);
    }

    public OAuth2DeviceCodeModel deny() {
        return new OAuth2DeviceCodeModel(realm, clientId, deviceCode, scope, nonce, expiration, pollingInterval,  null, true);
    }

    private OAuth2DeviceCodeModel(RealmModel realm, String clientId,
                                  String deviceCode, String scope, String nonce, int expiration, int pollingInterval,
                                  String userSessionId, boolean denied) {
        this.realm = realm;
        this.clientId = clientId;
        this.deviceCode = deviceCode;
        this.scope = scope;
        this.nonce = nonce;
        this.expiration = expiration;
        this.pollingInterval = pollingInterval;
        this.userSessionId = userSessionId;
        this.denied = denied;
    }

    public static OAuth2DeviceCodeModel fromCache(RealmModel realm, String deviceCode, Map<String, String> data) {
        return new OAuth2DeviceCodeModel(realm, deviceCode, data);
    }

    private OAuth2DeviceCodeModel(RealmModel realm, String deviceCode, Map<String, String> data) {
        this.realm = realm;
        this.clientId = data.get(CLIENT_ID);
        this.deviceCode = deviceCode;
        this.scope = data.get(SCOPE_NOTE);
        this.nonce = data.get(NONCE_NOTE);
        this.expiration = Integer.parseInt(data.get(EXPIRATION_NOTE));
        this.pollingInterval = Integer.parseInt(data.get(POLLING_INTERVAL_NOTE));
        this.userSessionId = data.get(USER_SESSION_ID_NOTE);
        this.denied = Boolean.parseBoolean(data.get(DENIED_NOTE));
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public String getScope() {
        return scope;
    }

    public String getNonce() {
        return nonce;
    }

    public int getExpiration() {
        return expiration;
    }

    public int getPollingInterval() {
        return pollingInterval;
    }

    public String getClientId() {
        return clientId;
    }

    public boolean isPending() {
        return userSessionId == null;
    }

    public boolean isApproved() {
        return userSessionId != null && !denied;
    }

    public boolean isDenied() {
        return userSessionId != null && denied;
    }

    public String getUserSessionId() {
        return userSessionId;
    }

    public static String createKey(RealmModel realm, String deviceCode) {
        return String.format("%s.dc.%s", realm.getId(), deviceCode);
    }

    public String serializeKey() {
        return createKey(realm, deviceCode);
    }

    public String serializePollingKey() {
        return createKey(realm, deviceCode) + ".polling";
    }

    public Map<String, String> serializeValue() {
        Map<String, String> result = new HashMap<>();
        result.put(CLIENT_ID, clientId);
        result.put(EXPIRATION_NOTE, String.valueOf(expiration));
        result.put(POLLING_INTERVAL_NOTE, String.valueOf(pollingInterval));
        result.put(SCOPE_NOTE, scope);
        result.put(NONCE_NOTE, nonce);
        return result;
    }

    public Map<String, String> serializeApprovedValue() {
        Map<String, String> result = new HashMap<>();
        result.put(EXPIRATION_NOTE, String.valueOf(expiration));
        result.put(POLLING_INTERVAL_NOTE, String.valueOf(pollingInterval));
        result.put(SCOPE_NOTE, scope);
        result.put(NONCE_NOTE, nonce);
        result.put(USER_SESSION_ID_NOTE, userSessionId);
        return result;
    }

    public Map<String, String> serializeDeniedValue() {
        Map<String, String> result = new HashMap<>();
        result.put(EXPIRATION_NOTE, String.valueOf(expiration));
        result.put(POLLING_INTERVAL_NOTE, String.valueOf(pollingInterval));
        result.put(DENIED_NOTE, String.valueOf(denied));
        return result;
    }

    public MultivaluedMap<String, String> getParams() {
        MultivaluedHashMap<String, String> params = new MultivaluedHashMap<>();
        params.putSingle(SCOPE_NOTE, scope);
        params.putSingle(NONCE_NOTE, nonce);
        return params;
    }
}
