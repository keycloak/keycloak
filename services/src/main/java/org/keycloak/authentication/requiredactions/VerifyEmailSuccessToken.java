/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authentication.requiredactions;

import org.keycloak.Token;
import org.keycloak.TokenCategory;
import org.keycloak.common.util.Time;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Identifies the registration behind a verify-email-success request.
 *
 * The tab that started the registration reaches that endpoint from session polling, which fires as soon as any
 * session cookie appears and cannot tell whether it was this registration that completed. The authentication
 * session cannot be used to identify it either, because when the email is verified in another tab of the same
 * browser both tabs share one authentication session and completing the login consumes it.
 *
 * This token is therefore handed to the verify-email page when it is rendered, and travels in the polling URL.
 * It is signed with the realm key, so the endpoint can trust the user it names and re-check that user's verified
 * status before confirming anything.
 */
public class VerifyEmailSuccessToken implements Token {

    @JsonProperty("uid")
    private String userId;

    @JsonProperty("exp")
    private long expiration;

    public VerifyEmailSuccessToken() {
    }

    public VerifyEmailSuccessToken(String userId, long expiration) {
        this.userId = userId;
        this.expiration = expiration;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    /**
     * {@link org.keycloak.models.TokenManager#decode} only verifies the signature, so expiry is checked here.
     */
    @JsonIgnore
    public boolean isExpired() {
        return Time.currentTime() > expiration;
    }

    @Override
    public TokenCategory getCategory() {
        return TokenCategory.INTERNAL;
    }
}
