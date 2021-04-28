/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import java.io.Serializable;
import java.util.function.Supplier;

import org.keycloak.utils.StringUtil;

public class CibaConfig implements Serializable {

    // realm attribute names
    public static final String CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE = "cibaBackchannelTokenDeliveryMode";
	public static final String CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT = "ciba.backchannel.token.delivery.mode";
    public static final String CIBA_EXPIRES_IN = "cibaExpiresIn";
    public static final String CIBA_INTERVAL = "cibaInterval";
    public static final String CIBA_AUTH_REQUESTED_USER_HINT = "cibaAuthRequestedUserHint";

    // default value
    public static final String DEFAULT_CIBA_POLICY_TOKEN_DELIVERY_MODE = "poll";
    public static final int DEFAULT_CIBA_POLICY_EXPIRES_IN = 120;
    public static final int DEFAULT_CIBA_POLICY_INTERVAL = 5;
    public static final String DEFAULT_CIBA_POLICY_AUTH_REQUESTED_USER_HINT = "login_hint";

    private String backchannelTokenDeliveryMode = DEFAULT_CIBA_POLICY_TOKEN_DELIVERY_MODE;
    private int expiresIn = DEFAULT_CIBA_POLICY_EXPIRES_IN;
    private int poolingInterval = DEFAULT_CIBA_POLICY_INTERVAL;
    private String authRequestedUserHint = DEFAULT_CIBA_POLICY_AUTH_REQUESTED_USER_HINT;

    // client attribute names
    public static final String OIDC_CIBA_GRANT_ENABLED = "oidc.ciba.grant.enabled";

    private transient Supplier<RealmModel> realm;

    // Make sure setters are not called when calling this from constructor to avoid DB updates
    private transient Supplier<RealmModel> realmForWrite;

    public CibaConfig(RealmModel realm) {
        this.realm = () -> realm;

        setBackchannelTokenDeliveryMode(realm.getAttribute(CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE));

        String expiresIn = realm.getAttribute(CIBA_EXPIRES_IN);

        if (StringUtil.isNotBlank(expiresIn)) {
            setExpiresIn(Integer.parseInt(expiresIn));
        }

        String interval = realm.getAttribute(CIBA_INTERVAL);

        if (StringUtil.isNotBlank(interval)) {
            setPoolingInterval(Integer.parseInt(interval));
        }

        setAuthRequestedUserHint(realm.getAttribute(CIBA_AUTH_REQUESTED_USER_HINT));

        this.realmForWrite = () -> realm;
    }

    public String getBackchannelTokenDeliveryMode(ClientModel client) {
        String mode = client.getAttribute(CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT);
        if (StringUtil.isBlank(mode)) {
            mode = getBackchannelTokenDeliveryMode();
        }
        return mode;
    }

    public String getBackchannelTokenDeliveryMode() {
        return backchannelTokenDeliveryMode;
    }

    public void setBackchannelTokenDeliveryMode(String mode) {
        if (StringUtil.isBlank(mode)) {
            mode = DEFAULT_CIBA_POLICY_TOKEN_DELIVERY_MODE;
        }
        this.backchannelTokenDeliveryMode = mode;
        persistRealmAttribute(CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE, mode);
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(String expiresIn) {
        if (expiresIn == null) {
            setExpiresIn((Integer) null);
        } else {
            setExpiresIn(Integer.parseInt(expiresIn));
        }
    }

    public void setExpiresIn(Integer expiresIn) {
        if (expiresIn == null) {
            expiresIn = DEFAULT_CIBA_POLICY_EXPIRES_IN;
        }
        this.expiresIn = expiresIn;
        persistRealmAttribute(CIBA_EXPIRES_IN, expiresIn);
    }

    public int getPoolingInterval() {
        return poolingInterval;
    }

    public void setPoolingInterval(String poolingInterval) {
        if (poolingInterval == null) {
            setPoolingInterval((Integer) null);
        } else {
            setPoolingInterval(Integer.parseInt(poolingInterval));
        }
    }

    public void setPoolingInterval(Integer interval) {
        if (interval == null) {
            interval = DEFAULT_CIBA_POLICY_INTERVAL;
        }
        this.poolingInterval = interval;
        persistRealmAttribute(CIBA_INTERVAL, interval);
    }

    public String getAuthRequestedUserHint() {
        return authRequestedUserHint;
    }

    public void setAuthRequestedUserHint(String hint) {
        if (hint == null) {
            hint = DEFAULT_CIBA_POLICY_AUTH_REQUESTED_USER_HINT;
        }
        this.authRequestedUserHint = hint;
        persistRealmAttribute(CIBA_AUTH_REQUESTED_USER_HINT, hint);
    }

    public boolean isOIDCCIBAGrantEnabled(ClientModel client) {
        String enabled = client.getAttribute(OIDC_CIBA_GRANT_ENABLED);
        return Boolean.parseBoolean(enabled);
    }

    private void persistRealmAttribute(String name, String value) {
        RealmModel realm = realmForWrite == null ? null : this.realmForWrite.get();
        if (realm != null) {
            realm.setAttribute(name, value);
        }
    }

    private void persistRealmAttribute(String name, Integer value) {
        RealmModel realm = realmForWrite == null ? null : this.realmForWrite.get();
        if (realm != null) {
            realm.setAttribute(name, value);
        }
    }
}
