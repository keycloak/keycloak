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

import org.keycloak.common.util.Base64;
import org.keycloak.models.ActionTokenKeyModel;
import org.keycloak.representations.JsonWebToken;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 *
 * @author hmlnarik
 */
public class DefaultActionTokenKey extends JsonWebToken implements ActionTokenKeyModel {

    /** The authenticationSession note with ID of the user authenticated via the action token */
    public static final String ACTION_TOKEN_USER_ID = "ACTION_TOKEN_USER";

    public static final String JSON_FIELD_ACTION_VERIFICATION_NONCE = "nonce";

    @JsonProperty(value = JSON_FIELD_ACTION_VERIFICATION_NONCE, required = true)
    private UUID actionVerificationNonce;

    public DefaultActionTokenKey() {
    }

    public DefaultActionTokenKey(String userId, String actionId, int absoluteExpirationInSecs, UUID actionVerificationNonce) {
        this.subject = userId;
        this.type = actionId;
        this.expiration = absoluteExpirationInSecs;
        this.actionVerificationNonce = actionVerificationNonce == null ? UUID.randomUUID() : actionVerificationNonce;
    }

    @JsonIgnore
    @Override
    public String getUserId() {
        return getSubject();
    }

    @JsonIgnore
    @Override
    public String getActionId() {
        return getType();
    }

    @Override
    public UUID getActionVerificationNonce() {
        return actionVerificationNonce;
    }

    private static final Pattern DOT = Pattern.compile("\\.");

    public static DefaultActionTokenKey from(String serializedKey) {
        if (serializedKey == null) {
            return null;
        }
        String[] parsed = DOT.split(serializedKey, 4);
        if (parsed.length != 4) {
            return null;
        }

        String userId;
        try {
            userId = new String(Base64.decode(parsed[0]), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            userId = parsed[0];
        }
        return new DefaultActionTokenKey(userId, parsed[3], Integer.parseInt(parsed[1]), UUID.fromString(parsed[2]));
    }

}
