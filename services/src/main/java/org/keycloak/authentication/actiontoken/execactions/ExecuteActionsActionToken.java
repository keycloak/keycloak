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
package org.keycloak.authentication.actiontoken.execactions;

import org.keycloak.TokenVerifier;
import org.keycloak.authentication.actiontoken.DefaultActionToken;
import org.keycloak.common.VerificationException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author hmlnarik
 */
public class ExecuteActionsActionToken extends DefaultActionToken {

    public static final String TOKEN_TYPE = "execute-actions";
    private static final String JSON_FIELD_REQUIRED_ACTIONS = "rqac";
    private static final String JSON_FIELD_REDIRECT_URI = "reduri";

    public ExecuteActionsActionToken(String userId, int absoluteExpirationInSecs, UUID actionVerificationNonce, List<String> requiredActions, String redirectUri, String clientId) {
        super(userId, TOKEN_TYPE, absoluteExpirationInSecs, actionVerificationNonce);
        setRequiredActions(requiredActions == null ? new LinkedList<>() : new LinkedList<>(requiredActions));
        setRedirectUri(redirectUri);
        this.issuedFor = clientId;
    }

    private ExecuteActionsActionToken() {
        super(null, TOKEN_TYPE, -1, null);
    }

    @JsonProperty(value = JSON_FIELD_REQUIRED_ACTIONS)
    public List<String> getRequiredActions() {
        return (List<String>) getOtherClaims().get(JSON_FIELD_REQUIRED_ACTIONS);
    }

    @JsonProperty(value = JSON_FIELD_REQUIRED_ACTIONS)
    public final void setRequiredActions(List<String> requiredActions) {
        if (requiredActions == null) {
            getOtherClaims().remove(JSON_FIELD_REQUIRED_ACTIONS);
        } else {
            setOtherClaims(JSON_FIELD_REQUIRED_ACTIONS, requiredActions);
        }
    }

    @JsonProperty(value = JSON_FIELD_REDIRECT_URI)
    public String getRedirectUri() {
        return (String) getOtherClaims().get(JSON_FIELD_REDIRECT_URI);
    }

    @JsonProperty(value = JSON_FIELD_REDIRECT_URI)
    public final void setRedirectUri(String redirectUri) {
        if (redirectUri == null) {
            getOtherClaims().remove(JSON_FIELD_REDIRECT_URI);
        } else {
            setOtherClaims(JSON_FIELD_REDIRECT_URI, redirectUri);
        }
    }

    /**
     * Returns a {@code ExecuteActionsActionToken} instance decoded from the given string. If decoding fails, returns {@code null}
     *
     * @param actionTokenString
     * @return
     */
    public static ExecuteActionsActionToken deserialize(String actionTokenString) throws VerificationException {
        return TokenVerifier.create(actionTokenString, ExecuteActionsActionToken.class).getToken();
    }
}
