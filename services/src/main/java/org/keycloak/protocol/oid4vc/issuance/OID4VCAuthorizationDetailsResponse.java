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
package org.keycloak.protocol.oid4vc.issuance;

import java.util.List;

import org.keycloak.protocol.oid4vc.model.ClaimsDescription;
import org.keycloak.protocol.oidc.rar.AuthorizationDetailsResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OID4VCI-specific authorization details response that extends the generic response
 * with OID4VCI-specific fields like credential_identifiers.
 *
 * @author <a href="mailto:Forkim.Akwichek@adorsys.com">Forkim Akwichek</a>
 */
public class OID4VCAuthorizationDetailsResponse extends AuthorizationDetailsResponse {

    @JsonProperty("type")
    private String type;

    @JsonProperty("credential_configuration_id")
    private String credentialConfigurationId;

    @JsonProperty("locations")
    private List<String> locations;

    @JsonProperty("credential_identifiers")
    private List<String> credentialIdentifiers;

    @JsonProperty("claims")
    private List<ClaimsDescription> claims;


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCredentialConfigurationId() {
        return credentialConfigurationId;
    }

    public void setCredentialConfigurationId(String credentialConfigurationId) {
        this.credentialConfigurationId = credentialConfigurationId;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public List<String> getCredentialIdentifiers() {
        return credentialIdentifiers;
    }

    public void setCredentialIdentifiers(List<String> credentialIdentifiers) {
        this.credentialIdentifiers = credentialIdentifiers;
    }

    public List<ClaimsDescription> getClaims() {
        return claims;
    }

    public void setClaims(List<ClaimsDescription> claims) {
        this.claims = claims;
    }
}
