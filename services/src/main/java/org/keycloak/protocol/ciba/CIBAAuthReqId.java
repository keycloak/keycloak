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
package org.keycloak.protocol.ciba;

import org.keycloak.OAuth2Constants;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CIBAAuthReqId extends JsonWebToken {

    public static final String SCOPE = OAuth2Constants.SCOPE;
    public static final String SESSION_STATE = IDToken.SESSION_STATE;
    public static final String AUTH_RESULT_ID = "auth_result_id";
    public static final String THROTTLING_ID = "throttling_id";

    @JsonProperty(SCOPE)
    protected String scope;

    @JsonProperty(SESSION_STATE)
    protected String sessionState;

    @JsonProperty(AUTH_RESULT_ID)
    protected String authResultId;

    @JsonProperty(THROTTLING_ID)
    protected String throttlingId;

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getSessionState() {
        return sessionState;
    }

    public void setSessionState(String sessionState) {
        this.sessionState = sessionState;
    }

    public String getAuthResultId() {
        return authResultId;
    }

    public void setAuthResultId(String authResultId) {
        this.authResultId = authResultId;
    }

    public String getThrottlingId() {
        return throttlingId;
    }

    public void setThrottlingId(String throttlingId) {
        this.throttlingId = throttlingId;
    }

}
