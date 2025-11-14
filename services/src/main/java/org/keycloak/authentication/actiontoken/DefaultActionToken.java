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

package org.keycloak.authentication.actiontoken;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.UriInfo;

import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.common.VerificationException;
import org.keycloak.models.DefaultActionTokenKey;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectValueModel;
import org.keycloak.services.Urls;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Part of action token that is intended to be used e.g. in link sent in password-reset email.
 * The token encapsulates user, expected action and its time of expiry.
 *
 * @author hmlnarik
 */
public class DefaultActionToken extends DefaultActionTokenKey implements SingleUseObjectValueModel {

    public static final String JSON_FIELD_AUTHENTICATION_SESSION_ID = "asid";
    public static final String JSON_FIELD_EMAIL = "eml";

    @JsonProperty(value = JSON_FIELD_AUTHENTICATION_SESSION_ID)
    private String compoundAuthenticationSessionId;

    @JsonProperty(value = JSON_FIELD_EMAIL)
    private String email;

    public static final Predicate<DefaultActionTokenKey> ACTION_TOKEN_BASIC_CHECKS = t -> {
        if (t.getActionVerificationNonce() == null) {
            throw new VerificationException("Nonce not present.");
        }

        return true;
    };

    /**
     * Single-use random value used for verification whether the relevant action is allowed.
     */
    public DefaultActionToken() {
        super(null, null, 0, null);
    }

    /**
     *
     * @param userId User ID
     * @param actionId Action ID
     * @param absoluteExpirationInSecs Absolute expiration time in seconds in timezone of Keycloak.
     * @param actionVerificationNonce
     */
    protected DefaultActionToken(String userId, String actionId, int absoluteExpirationInSecs, UUID actionVerificationNonce) {
        super(userId, actionId, absoluteExpirationInSecs, actionVerificationNonce);
    }

    /**
     *
     * @param userId User ID
     * @param actionId Action ID
     * @param absoluteExpirationInSecs Absolute expiration time in seconds in timezone of Keycloak.
     * @param actionVerificationNonce
     */
    protected DefaultActionToken(String userId, String actionId, int absoluteExpirationInSecs, UUID actionVerificationNonce, String compoundAuthenticationSessionId) {
        super(userId, actionId, absoluteExpirationInSecs, actionVerificationNonce);
        setCompoundAuthenticationSessionId(compoundAuthenticationSessionId);
    }

    public String getCompoundAuthenticationSessionId() {
        return compoundAuthenticationSessionId;
    }

    public void setCompoundAuthenticationSessionId(String compoundAuthenticationSessionId) {
        this.compoundAuthenticationSessionId = compoundAuthenticationSessionId;
    }

    @JsonIgnore
    @Override
    public Map<String, String> getNotes() {
        Map<String, String> res = new HashMap<>();
        if (getCompoundAuthenticationSessionId() != null) {
            res.put(JSON_FIELD_AUTHENTICATION_SESSION_ID, getCompoundAuthenticationSessionId());
        }
        return res;
    }

    @Override
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

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    /**
     * Updates the following fields and serializes this token into a signed JWT. The list of updated fields follows:
     * <ul>
     * <li>{@code id}: random nonce</li>
     * <li>{@code issuedAt}: Current time</li>
     * <li>{@code issuer}: URI of the given realm</li>
     * <li>{@code audience}: URI of the given realm (same as issuer)</li>
     * </ul>
     *
     * @param session
     * @param realm
     * @param uri
     * @return
     */
    public String serialize(KeycloakSession session, RealmModel realm, UriInfo uri) {
        String issuerUri = getIssuer(realm, uri);

        this
          .issuedNow()
          .id(UUID.randomUUID().toString())
          .issuer(issuerUri)
          .audience(issuerUri);

        return session.tokens().encode(this);
    }

    private static String getIssuer(RealmModel realm, UriInfo uri) {
        return Urls.realmIssuer(uri.getBaseUri(), realm.getName());
    }

}
