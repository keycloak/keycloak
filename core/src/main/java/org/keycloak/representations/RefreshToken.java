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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.TokenCategory;
import org.keycloak.util.TokenUtil;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RefreshToken extends AccessToken {

    public static final String ONLINE_SESSION_ID = "online_sid";

    /**
     * Holds the (user) sessionId of the originating online session, in case of an offline token.
     */
    @JsonProperty(ONLINE_SESSION_ID)
    protected String onlineSessionId;

    private RefreshToken() {
        type(TokenUtil.TOKEN_TYPE_REFRESH);
    }

    /**
     * Deep copies issuer, subject, issuedFor, sessionState from AccessToken.
     *
     * @param token
     */
    public RefreshToken(AccessToken token) {
        this();
        this.issuer = token.issuer;
        this.subject = token.subject;
        this.issuedFor = token.issuedFor;
        this.sessionState = token.sessionState;
        this.nonce = token.nonce;
        this.audience = new String[] { token.issuer };
        this.scope = token.scope;
    }

    @Override
    public TokenCategory getCategory() {
        return TokenCategory.INTERNAL;
    }

    public String getOnlineSessionId() {
        /*
         * Backwards-compatibility: For tokens created after onlineSessionId was introduced, it would be ok to just
         * return the onlineSessionId.
         * The sessionId is returned for older tokens.
         */
        return onlineSessionId != null ? onlineSessionId : sessionState;
    }

    public void setOnlineSessionId(String onlineSessionId) {
        this.onlineSessionId = onlineSessionId;
    }
}
