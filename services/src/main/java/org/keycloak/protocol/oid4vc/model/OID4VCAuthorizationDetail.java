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
package org.keycloak.protocol.oid4vc.model;

import java.util.List;
import java.util.Objects;

import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an authorization_details object in the Token Request as per OID4VCI.
 * 
 * @author <a href="mailto:Forkim.Akwichek@adorsys.com">Forkim Akwichek</a>
 */
public class OID4VCAuthorizationDetail extends AuthorizationDetailsJSONRepresentation {

    public static final String CREDENTIAL_CONFIGURATION_ID = "credential_configuration_id";
    public static final String CREDENTIAL_IDENTIFIERS = "credential_identifiers";
    public static final String CLAIMS = "claims";

    @JsonProperty(CREDENTIAL_CONFIGURATION_ID)
    private String credentialConfigurationId;

    @JsonProperty(CLAIMS)
    private List<ClaimsDescription> claims;

    public String getCredentialConfigurationId() {
        return credentialConfigurationId;
    }

    public void setCredentialConfigurationId(String credentialConfigurationId) {
        this.credentialConfigurationId = credentialConfigurationId;
    }

    public List<ClaimsDescription> getClaims() {
        return claims;
    }

    public void setClaims(List<ClaimsDescription> claims) {
        this.claims = claims;
    }

    @Override
    public String toString() {
        return "OID4VCAuthorizationDetail {" +
                " type='" + getType() + '\'' +
                ", locations='" + getLocations() + '\'' +
                ", credentialConfigurationId='" + credentialConfigurationId + '\'' +
                ", claims=" + claims +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        OID4VCAuthorizationDetail that = (OID4VCAuthorizationDetail) o;
        return Objects.equals(credentialConfigurationId, that.credentialConfigurationId)
                && Objects.equals(claims, that.claims);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), credentialConfigurationId, claims);
    }
}
