/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.representations;

import org.keycloak.TokenCategory;
import org.keycloak.json.StringOrArrayDeserializer;
import org.keycloak.json.StringOrArraySerializer;
import org.keycloak.util.TokenUtil;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RefreshToken extends AccessToken {

    public static final String ORIGINAL_AUD = "aud_x";

    @JsonProperty(ORIGINAL_AUD)
    @JsonSerialize(using = StringOrArraySerializer.class)
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    protected String[] originalAudience;

    private RefreshToken() {
        type(TokenUtil.TOKEN_TYPE_REFRESH);
    }

    /**
     * Deep copies issuer, subject, issuedFor, sessionState from AccessToken.
     *
     */
    public RefreshToken(AccessToken token) {
        this();
        this.issuer = token.issuer;
        this.subject = token.subject;
        this.issuedFor = token.issuedFor;
        this.sessionId = token.sessionId;
        this.nonce = token.nonce;
        this.audience = new String[] { token.issuer };
        this.originalAudience = token.audience;
        this.scope = token.scope;
        this.authorizationDetails = token.authorizationDetails;
    }

    /**
     * Deep copies issuer, subject, issuedFor, sessionState from AccessToken.
     *
     * @param token
     * @param confirmation optional confirmation parameter that might be processed during authentication but should not
     *                     always be included in the response
     */
    public RefreshToken(AccessToken token, Confirmation confirmation) {
        this(token);
        this.confirmation = confirmation;
    }

    @Override
    public TokenCategory getCategory() {
        return TokenCategory.INTERNAL;
    }

    @Override
    public String getSessionId() {
        String sessionId = super.getSessionId();
        // Fallback as offline tokens created in Keycloak 14 or earlier have only the "session_state" claim, but not "sid"
        return sessionId != null ? sessionId : (String) getOtherClaims().get(IDToken.SESSION_STATE);
    }

    public String[] getOriginalAudience() {
        return originalAudience;
    }
}
