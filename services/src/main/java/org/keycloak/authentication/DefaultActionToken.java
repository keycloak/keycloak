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

package org.keycloak.authentication;

import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.common.VerificationException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;

/**
 * Part of action token that is intended to be used e.g. in link sent in password-reset email.
 * The token encapsulates user, expected action and its time of expiry.
 *
 * @author hmlnarik
 */
public class DefaultActionToken extends DefaultActionTokenKey {

    public static final String JSON_FIELD_ACTION_VERIFICATION_NONCE = "nonce";

    public static Predicate<DefaultActionToken> ACTION_TOKEN_BASIC_CHECKS = t -> {
        if (t.getActionVerificationNonce() == null) {
            throw new VerificationException("Nonce not present.");
        }

        return true;
    };

    /**
     * Single-use random value used for verification whether the relevant action is allowed.
     */
    @JsonProperty(value = JSON_FIELD_ACTION_VERIFICATION_NONCE, required = true)
    private final UUID actionVerificationNonce;

    public DefaultActionToken(String userId, String actionId, int expirationInSecs) {
        this(userId, actionId, expirationInSecs, UUID.randomUUID());
    }

    /**
     *
     * @param userId User ID
     * @param actionId Action ID
     * @param absoluteExpirationInSecs Absolute expiration time in seconds in timezone of Keycloak.
     * @param actionVerificationNonce
     */
    protected DefaultActionToken(String userId, String actionId, int absoluteExpirationInSecs, UUID actionVerificationNonce) {
        super(userId, actionId);
        this.actionVerificationNonce = actionVerificationNonce == null ? UUID.randomUUID() : actionVerificationNonce;
        expiration = absoluteExpirationInSecs;
    }

    public UUID getActionVerificationNonce() {
        return actionVerificationNonce;
    }

    @JsonIgnore
    public Map<String, String> getNotes() {
        Map<String, String> res = new HashMap<>();
        return res;
    }

    public String getNote(String name) {
        Object res = getOtherClaims().get(name);
        return res instanceof String ? (String) res : null;
    }

    /**
     * Sets value of the given note
     * @return original value (or {@code null} when no value was present)
     */
    public final String setNote(String name, String value) {
        Object res = value == null
          ? getOtherClaims().remove(name)
          : getOtherClaims().put(name, value);
        return res instanceof String ? (String) res : null;
    }

    /**
     * Removes given note, and returns original value (or {@code null} when no value was present)
     * @return see description
     */
    public final String removeNote(String name) {
        Object res = getOtherClaims().remove(name);
        return res instanceof String ? (String) res : null;
    }

}
