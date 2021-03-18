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

    // CIBA policy attribute key for persitent store
    public static final String CIBA_BACKCHANNEL_TOKENDELIVERY_MODE = "cibaBackchannelTokenDeliveryMode";
    public static final String CIBA_EXPIRES_IN = "cibaExpiresIn";
    public static final String CIBA_INTERVAL = "cibaInterval";
    public static final String CIBA_AUTH_REQUESTED_USER_HINT = "cibaAuthRequestedUserHint";

    // CIBA policy default value
    public static final String DEFAULT_CIBA_POLICY_TOKEN_DELIVERY_MODE = "poll";
    public static final int DEFAULT_CIBA_POLICY_EXPIRES_IN = 120;
    public static final int DEFAULT_CIBA_POLICY_INTERVAL = 5;
    public static final String DEFAULT_CIBA_POLICY_AUTH_REQUESTED_USER_HINT = "login_hint";

    private String backchannelTokenDeliveryMode = DEFAULT_CIBA_POLICY_TOKEN_DELIVERY_MODE;
    private int expiresIn = DEFAULT_CIBA_POLICY_EXPIRES_IN;
    private int poolingInterval = DEFAULT_CIBA_POLICY_INTERVAL;
    private String authRequestedUserHint = DEFAULT_CIBA_POLICY_AUTH_REQUESTED_USER_HINT;

    private transient Supplier<RealmModel> realm;

    public CibaConfig(RealmModel realm) {
        this.realm = () -> realm;

        setBackchannelTokenDeliveryMode(realm.getAttribute(CIBA_BACKCHANNEL_TOKENDELIVERY_MODE));

        String expiresIn = realm.getAttribute(CIBA_EXPIRES_IN);

        if (StringUtil.isNotBlank(expiresIn)) {
            setExpiresIn(Integer.parseInt(expiresIn));
        }

        String interval = realm.getAttribute(CIBA_INTERVAL);

        if (StringUtil.isNotBlank(interval)) {
            setPoolingInterval(Integer.parseInt(interval));
        }

        setAuthRequestedUserHint(realm.getAttribute(CIBA_AUTH_REQUESTED_USER_HINT));
    }

    public String getBackchannelTokenDeliveryMode() {
        return backchannelTokenDeliveryMode;
    }

    public void setBackchannelTokenDeliveryMode(String mode) {
        if (StringUtil.isBlank(mode)) {
            mode = DEFAULT_CIBA_POLICY_TOKEN_DELIVERY_MODE;
        }
        this.backchannelTokenDeliveryMode = mode;
        realm.get().setAttribute(CIBA_BACKCHANNEL_TOKENDELIVERY_MODE, mode);
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
        realm.get().setAttribute(CIBA_EXPIRES_IN, expiresIn);
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
        realm.get().setAttribute(CIBA_INTERVAL, interval);
    }

    public String getAuthRequestedUserHint() {
        return authRequestedUserHint;
    }

    public void setAuthRequestedUserHint(String hint) {
        if (hint == null) {
            hint = DEFAULT_CIBA_POLICY_AUTH_REQUESTED_USER_HINT;
        }
        this.authRequestedUserHint = hint;
        realm.get().setAttribute(CIBA_AUTH_REQUESTED_USER_HINT, hint);
    }
}
