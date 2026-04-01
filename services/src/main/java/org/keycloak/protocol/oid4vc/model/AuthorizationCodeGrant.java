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

package org.keycloak.protocol.oid4vc.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Container for the authorization code grant to be used in a Credential Offer
 * <p>
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-offer}
 *
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorizationCodeGrant implements CredentialOfferGrant {

    public static final String AUTH_CODE_GRANT_TYPE = "authorization_code";
    public static final String ISSUER_STATE = "issuer_state";

    @Override
    @JsonIgnore
    public String getGrantType() {
        return AUTH_CODE_GRANT_TYPE;
    }

    @JsonProperty(ISSUER_STATE)
    private String issuerState;

    public String getIssuerState() {
        return issuerState;
    }

    public AuthorizationCodeGrant setIssuerState(String issuerState) {
        this.issuerState = issuerState;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthorizationCodeGrant grant)) return false;
        return Objects.equals(issuerState, grant.issuerState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issuerState);
    }
}
