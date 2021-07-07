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

    private static final String REALM_ID = "rid";
    private static final String CLIENT_ID = "cid";
    private static final String EXPIRATION_NOTE = "exp";
    private static final String POLLING_INTERVAL_NOTE = "int";
    private static final String NONCE_NOTE = "nonce";
    private static final String SCOPE_NOTE = "scope";
    private static final String USER_SESSION_ID_NOTE = "uid";
    private static final String DENIED_NOTE = "denied";
    private static final String ADDITIONAL_PARAM_PREFIX = "additional_param_";

    private final RealmModel realm;
    private final String clientId;
    private final String deviceCode;
    private final int expiration;
    private final int pollingInterval;
    private final String scope;
    private final String nonce;
    private final String userSessionId;
    private final Boolean denied;
    private final Map<String, String> additionalParams;

    public static OAuth2DeviceCodeModel create(RealmModel realm, ClientModel client,
                                               String deviceCode, String scope, String nonce, int expiresIn, int pollingInterval, Map<String, String> additionalParams) {
        
        int expiration = Time.currentTime() + expiresIn;
        return new OAuth2DeviceCodeModel(realm, client.getClientId(), deviceCode, scope, nonce, expiration, pollingInterval,  null, null, additionalParams);
    }

    public OAuth2DeviceCodeModel approve(String userSessionId) {
        return new OAuth2DeviceCodeModel(realm, clientId, deviceCode, scope, nonce, expiration,  pollingInterval, userSessionId, false, additionalParams);
    }

    public OAuth2DeviceCodeModel approve(String userSessionId, Map<String, String> additionalParams) {
        if (additionalParams != null) {
            this.additionalParams.putAll(additionalParams);
        }
        return new OAuth2DeviceCodeModel(realm, clientId, deviceCode, scope, nonce, expiration, pollingInterval, userSessionId, false, this.additionalParams);
    }

    public OAuth2DeviceCodeModel deny() {
        return new OAuth2DeviceCodeModel(realm, clientId, deviceCode, scope, nonce, expiration, pollingInterval,  null, true, additionalParams);
    }

    private OAuth2DeviceCodeModel(RealmModel realm, String clientId,
                                  String deviceCode, String scope, String nonce, int expiration, int pollingInterval,
                                  String userSessionId, Boolean denied, Map<String, String> additionalParams) {
        this.realm = realm;
        this.clientId = clientId;
        this.deviceCode = deviceCode;
        this.scope = scope;
        this.nonce = nonce;
        this.expiration = expiration;
        this.pollingInterval = pollingInterval;
        this.userSessionId = userSessionId;
        this.denied = denied;
        this.additionalParams = additionalParams;
    }

    public static OAuth2DeviceCodeModel fromCache(RealmModel realm, String deviceCode, Map<String, String> data) {
        OAuth2DeviceCodeModel model = new OAuth2DeviceCodeModel(realm, deviceCode, data);

        if (!realm.getId().equals(data.get(REALM_ID))) {
            return null;
        }

        return model;
    }

    private OAuth2DeviceCodeModel(RealmModel realm, String deviceCode, Map<String, String> data) {
        this(realm, data.get(CLIENT_ID), deviceCode, data.get(SCOPE_NOTE), data.get(NONCE_NOTE),
            Integer.parseInt(data.get(EXPIRATION_NOTE)), Integer.parseInt(data.get(POLLING_INTERVAL_NOTE)), data.get(USER_SESSION_ID_NOTE),
            Boolean.parseBoolean(data.get(DENIED_NOTE)), extractAdditionalParams(data));
    }

    private static Map<String, String> extractAdditionalParams(Map<String, String> data) {
        Map<String, String> additionalParams = new HashMap<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (entry.getKey().startsWith(ADDITIONAL_PARAM_PREFIX)) {
                additionalParams.put(entry.getKey().substring(ADDITIONAL_PARAM_PREFIX.length()), entry.getValue());
            }
        }
        return additionalParams;
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

    public boolean isDenied() {
        return denied;
    }

    public String getUserSessionId() {
        return userSessionId;
    }

    public static String createKey(String deviceCode) {
        return String.format("dc.%s", deviceCode);
    }

    public String serializeKey() {
        return createKey(deviceCode);
    }

    public String serializePollingKey() {
        return createKey(deviceCode) + ".polling";
    }

    public Map<String, String> toMap() {
        Map<String, String> result = new HashMap<>();

        result.put(REALM_ID, realm.getId());

        if (denied == null) {
            result.put(CLIENT_ID, clientId);
            result.put(EXPIRATION_NOTE, String.valueOf(expiration));
            result.put(POLLING_INTERVAL_NOTE, String.valueOf(pollingInterval));
            result.put(SCOPE_NOTE, scope);
            result.put(NONCE_NOTE, nonce);
        } else if (denied) {
            result.put(EXPIRATION_NOTE, String.valueOf(expiration));
            result.put(POLLING_INTERVAL_NOTE, String.valueOf(pollingInterval));
            result.put(DENIED_NOTE, String.valueOf(denied));
        } else {
            result.put(EXPIRATION_NOTE, String.valueOf(expiration));
            result.put(POLLING_INTERVAL_NOTE, String.valueOf(pollingInterval));
            result.put(SCOPE_NOTE, scope);
            result.put(NONCE_NOTE, nonce);
            result.put(USER_SESSION_ID_NOTE, userSessionId);
        }

        additionalParams.forEach((key, value) -> result.put(ADDITIONAL_PARAM_PREFIX + key, value));

        return result;
    }

    public MultivaluedMap<String, String> getParams() {
        MultivaluedHashMap<String, String> params = new MultivaluedHashMap<>();
        params.putSingle(SCOPE_NOTE, scope);
        if (nonce != null) {
            params.putSingle(NONCE_NOTE, nonce);
        }
        this.additionalParams.forEach(params::putSingle);
        return params;
    }

    public Map<String, String> getAdditionalParams() {
        return additionalParams;
    }

    public boolean isExpired() {
        return getExpiration() - Time.currentTime() < 0;
    }
}
