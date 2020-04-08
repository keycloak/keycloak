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

public class CIBAPolicy implements Serializable {

    // CIBA policy attribute key for persitent store
    public static final String CIBA_AUTHENTICATION_FLOW_ALIAS = "cibaAuthenticationFlowAlias";
    public static final String CIBA_BACKCHANNEL_TOKENDELIVERY_MODE = "cibaBackchannelTokenDeliveryMode";
    public static final String CIBA_EXPIRES_IN = "cibaExpiresIn";
    public static final String CIBA_INTERVAL = "cibaInterval";
    public static final String CIBA_AUTH_REQUESTED_USER_HINT = "cibaAuthRequestedUserHint";

    // CIBA policy default value
    public static final String DEFAULT_CIBA_FLOW_ALIAS = "ciba";
    public static final String DEFAULT_CIBA_POLICY_TOKEN_DELIVERY_MODE = "poll";
    public static final int DEFAULT_CIBA_POLICY_EXPIRES_IN = 120;
    public static final int DEFAULT_CIBA_POLICY_INTERVAL = 0;
    public static final String DEFAULT_CIBA_POLICY_AUTH_REQUESTED_USER_HINT = "login_hint";

    private String cibaFlow = DEFAULT_CIBA_FLOW_ALIAS;
    private String backchannelTokenDeliveryMode = DEFAULT_CIBA_POLICY_TOKEN_DELIVERY_MODE;
    private int expiresIn = DEFAULT_CIBA_POLICY_EXPIRES_IN;
    private int interval = DEFAULT_CIBA_POLICY_INTERVAL;
    private String authRequestedUserHint = DEFAULT_CIBA_POLICY_AUTH_REQUESTED_USER_HINT;

    public String getCibaFlow() {
        return cibaFlow;
    }

    public void setCibaFlow(String cibaFlow) {
        this.cibaFlow = cibaFlow;
    }

    public String getBackchannelTokenDeliveryMode() {
        return backchannelTokenDeliveryMode;
    }

    public void setBackchannelTokenDeliveryMode(String backchannelTokenDeliveryMode) {
        this.backchannelTokenDeliveryMode = backchannelTokenDeliveryMode;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public String getAuthRequestedUserHint() {
        return authRequestedUserHint;
    }

    public void setAuthRequestedUserHint(String authRequestedUserHint) {
        this.authRequestedUserHint = authRequestedUserHint;
    }
}
