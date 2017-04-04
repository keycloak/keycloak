/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authentication.actiontoken.resetcred;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import org.keycloak.authentication.actiontoken.DefaultActionToken;

/**
 * Representation of a token that represents a time-limited reset credentials action.
 *
 * @author hmlnarik
 */
public class ResetCredentialsActionToken extends DefaultActionToken {

    public static final String TOKEN_TYPE = "reset-credentials";
    private static final String JSON_FIELD_LAST_CHANGE_PASSWORD_TIMESTAMP = "lcpt";

    @JsonProperty(value = JSON_FIELD_LAST_CHANGE_PASSWORD_TIMESTAMP)
    private Long lastChangedPasswordTimestamp;

    public ResetCredentialsActionToken(String userId, int absoluteExpirationInSecs, UUID actionVerificationNonce, String authenticationSessionId, Long lastChangedPasswordTimestamp) {
        super(userId, TOKEN_TYPE, absoluteExpirationInSecs, actionVerificationNonce);
        setAuthenticationSessionId(authenticationSessionId);
        this.lastChangedPasswordTimestamp = lastChangedPasswordTimestamp;
    }

    private ResetCredentialsActionToken() {
        super(null, TOKEN_TYPE, -1, null);
    }

    public Long getLastChangedPasswordTimestamp() {
        return lastChangedPasswordTimestamp;
    }

    public final void setLastChangedPasswordTimestamp(Long lastChangedPasswordTimestamp) {
        this.lastChangedPasswordTimestamp = lastChangedPasswordTimestamp;
    }
}
