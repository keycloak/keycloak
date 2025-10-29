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

import java.util.Arrays;
import java.util.List;

import org.keycloak.jose.jws.Algorithm;
import org.keycloak.utils.StringUtil;

public class CibaConfig extends AbstractConfig {

    // Constants
    public static final String CIBA_POLL_MODE = "poll";
    public static final String CIBA_PING_MODE = "ping";
    public static final String CIBA_PUSH_MODE = "push";
    public static final List<String> CIBA_SUPPORTED_MODES = Arrays.asList(CIBA_POLL_MODE, CIBA_PING_MODE);

    // realm attribute names
    public static final String CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE = "cibaBackchannelTokenDeliveryMode";
    public static final String CIBA_EXPIRES_IN = "cibaExpiresIn";
    public static final String CIBA_INTERVAL = "cibaInterval";
    public static final String CIBA_AUTH_REQUESTED_USER_HINT = "cibaAuthRequestedUserHint";

    // default value
    public static final String DEFAULT_CIBA_POLICY_TOKEN_DELIVERY_MODE = CIBA_POLL_MODE;
    public static final int DEFAULT_CIBA_POLICY_EXPIRES_IN = 120;
    public static final int DEFAULT_CIBA_POLICY_INTERVAL = 5;
    public static final String DEFAULT_CIBA_POLICY_AUTH_REQUESTED_USER_HINT = "login_hint";

    private String backchannelTokenDeliveryMode;
    private int expiresIn;
    private int poolingInterval;
    private String authRequestedUserHint;

    // client attribute names
    public static final String OIDC_CIBA_GRANT_ENABLED = "oidc.ciba.grant.enabled";
    public static final String CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT = "ciba.backchannel.token.delivery.mode";
    public static final String CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT = "ciba.backchannel.client.notification.endpoint";
    public static final String CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG = "ciba.backchannel.auth.request.signing.alg";

    public CibaConfig(RealmModel realm) {
        this.backchannelTokenDeliveryMode = realm.getAttribute(CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE);
        if (this.backchannelTokenDeliveryMode == null) {
            this.backchannelTokenDeliveryMode =  DEFAULT_CIBA_POLICY_TOKEN_DELIVERY_MODE;
        }

        this.expiresIn = realm.getAttribute(CIBA_EXPIRES_IN, DEFAULT_CIBA_POLICY_EXPIRES_IN);

        this.poolingInterval = realm.getAttribute(CIBA_INTERVAL, DEFAULT_CIBA_POLICY_INTERVAL);

        this.authRequestedUserHint = realm.getAttribute(CIBA_AUTH_REQUESTED_USER_HINT);
        if (authRequestedUserHint == null) {
            authRequestedUserHint = DEFAULT_CIBA_POLICY_AUTH_REQUESTED_USER_HINT;
        }

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

    public Algorithm getBackchannelAuthRequestSigningAlg(ClientModel client) {
        String alg = client.getAttribute(CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG);
        return alg==null ? null : Enum.valueOf(Algorithm.class, alg);
    }

    public String getBackchannelClientNotificationEndpoint(ClientModel client) {
        return client.getAttribute(CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT);
    }
}
