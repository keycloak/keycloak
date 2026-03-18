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

package org.keycloak.protocol.oid4vc.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import static org.keycloak.OID4VCConstants.WELL_KNOWN_OPENID_CREDENTIAL_ISSUER;
import static org.keycloak.protocol.oid4vc.model.AuthorizationCodeGrant.AUTH_CODE_GRANT_TYPE;
import static org.keycloak.protocol.oid4vc.model.PreAuthorizedCodeGrant.PRE_AUTH_GRANT_TYPE;

/**
 * Represents a CredentialsOffer according to the OID4VCI Spec
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-offer}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialsOffer {

    @JsonProperty("credential_issuer")
    private String credentialIssuer;

    //ids of credentials as offered in the issuer metadata
    @JsonProperty("credential_configuration_ids")
    private List<String> credentialConfigurationIds;

    @JsonProperty("grants")
    @JsonDeserialize(using = CredentialOfferGrantsDeserializer.class)
    private Map<String, CredentialOfferGrant> grants = new HashMap<>();

    public String getCredentialIssuer() {
        return credentialIssuer;
    }

    public CredentialsOffer setCredentialIssuer(String credentialIssuer) {
        this.credentialIssuer = credentialIssuer;
        return this;
    }

    @JsonIgnore
    public String getIssuerMetadataUrl() {
        var metadataUrl = KeycloakUriBuilder
                .fromUri(credentialIssuer)
                .path("/.well-known/" + WELL_KNOWN_OPENID_CREDENTIAL_ISSUER);
        var idx = credentialIssuer.indexOf("/realms");
        if (idx > 0) {
            var baseUrl = credentialIssuer.substring(0, idx);
            var realmPath = credentialIssuer.substring(idx);
            metadataUrl = KeycloakUriBuilder
                    .fromUri(baseUrl)
                    .path("/.well-known/" + WELL_KNOWN_OPENID_CREDENTIAL_ISSUER)
                    .path(realmPath);
        }
        return metadataUrl.buildAsString();
    }

    public List<String> getCredentialConfigurationIds() {
        return credentialConfigurationIds;
    }

    public CredentialsOffer setCredentialConfigurationIds(List<String> credentialConfigurationIds) {
        this.credentialConfigurationIds = Collections.unmodifiableList(credentialConfigurationIds);
        return this;
    }

    public CredentialOfferGrant getGrant(String grantType) {
        return grants.get(grantType);
    }

    public CredentialsOffer addGrant(CredentialOfferGrant grant) {
        grants.put(grant.getGrantType(), grant);
        return this;
    }

    @JsonIgnore
    public AuthorizationCodeGrant getAuthorizationCodeGrant() {
        return (AuthorizationCodeGrant) grants.get(AUTH_CODE_GRANT_TYPE);
    }

    @JsonIgnore
    public String getIssuerState() {
        return Optional.ofNullable(getAuthorizationCodeGrant())
                .map(AuthorizationCodeGrant::getIssuerState)
                .orElse(null);
    }

    @JsonIgnore
    public PreAuthorizedCodeGrant getPreAuthorizedGrant() {
        return (PreAuthorizedCodeGrant) grants.get(PRE_AUTH_GRANT_TYPE);
    }

    @JsonIgnore
    public String getPreAuthorizedCode() {
        return Optional.ofNullable(getPreAuthorizedGrant())
                .map(PreAuthorizedCodeGrant::getPreAuthorizedCode)
                .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CredentialsOffer that)) return false;
        boolean match = Objects.equals(credentialIssuer, that.credentialIssuer);
        match &= Objects.equals(credentialConfigurationIds, that.credentialConfigurationIds);
        match &= Objects.equals(grants, that.grants);
        return match;
    }

    @Override
    public int hashCode() {
        return Objects.hash(credentialIssuer, credentialConfigurationIds, grants);
    }

    public String toString() {
        return JsonSerialization.valueAsString(this);
    }
}
