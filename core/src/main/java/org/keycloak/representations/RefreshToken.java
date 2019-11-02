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
import org.keycloak.util.TokenUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RefreshToken extends AccessToken {

    private RefreshToken() {
        type(TokenUtil.TOKEN_TYPE_REFRESH);
    }

    /**
     * Deep copies issuer, subject, issuedFor, sessionState, realmAccess, and resourceAccess
     * from AccessToken.
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
        if (token.realmAccess != null) {
            realmAccess = token.realmAccess.clone();
        }
        if (token.resourceAccess != null) {
            resourceAccess = new HashMap<>();
            for (Map.Entry<String, Access> entry : token.resourceAccess.entrySet()) {
                resourceAccess.put(entry.getKey(), entry.getValue().clone());
            }
        }
    }

    @Override
    public TokenCategory getCategory() {
        return TokenCategory.INTERNAL;
    }
}
