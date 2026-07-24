/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authentication.actiontoken.inviteorg;

import org.keycloak.authentication.actiontoken.DefaultActionToken;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representation of a token that represents a time-limited verify e-mail action.
 *
 * @author hmlnarik
 */
public class InviteOrgActionToken extends DefaultActionToken {

    public static final String TOKEN_TYPE = "ORGIVT";

    private static final String JSON_FIELD_REDIRECT_URI = "reduri";
    private static final String JSON_ORG_ID = "org_id";

    @JsonProperty(JSON_FIELD_REDIRECT_URI)
    private String redirectUri;


    @JsonProperty(JSON_ORG_ID)
    private String orgId;

    public InviteOrgActionToken(String userId, int absoluteExpirationInSecs, String email, String clientId) {
        super(userId, TOKEN_TYPE, absoluteExpirationInSecs, null);
        setEmail(email);
        this.issuedFor = clientId;
    }

    private InviteOrgActionToken() {
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }
}
