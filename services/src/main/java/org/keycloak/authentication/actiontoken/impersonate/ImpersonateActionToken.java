/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.actiontoken.impersonate;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.authentication.actiontoken.DefaultActionToken;

public class ImpersonateActionToken extends DefaultActionToken {

    public static final String TOKEN_TYPE = "impersonate";

    @JsonProperty("impersonator")
    private String impersonatorUsername;

    @JsonProperty("impersonatorId")
    private String impersonatorId;

    @JsonProperty("impersonatorRealm")
    private String impersonatorRealm;

    @JsonProperty("reduri")
    private String redirectUri;

    public ImpersonateActionToken(String userId, String impersonatorUsername, String impersonatorId,
            String impersonatorRealm, String redirectUri,
            int absoluteExpirationInSecs) {
        super(userId, TOKEN_TYPE, absoluteExpirationInSecs, null);
        this.impersonatorUsername = impersonatorUsername;
        this.impersonatorId = impersonatorId;
        this.impersonatorRealm = impersonatorRealm;
        this.redirectUri = redirectUri;
    }

    private ImpersonateActionToken() {
    }

    public String getImpersonatorId() {
        return impersonatorId;
    }

    public void setImpersonatorId(String impersonatorId) {
        this.impersonatorId = impersonatorId;
    }

    public String getImpersonatorUsername() {
        return impersonatorUsername;
    }

    public void setImpersonatorUsername(String impersonatorUsername) {
        this.impersonatorUsername = impersonatorUsername;
    }

    public void setImpersonatorRealm(String impersonatorRealm) {
        this.impersonatorRealm = impersonatorRealm;
    }

    public String getImpersonatorRealm() {
        return impersonatorRealm;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
