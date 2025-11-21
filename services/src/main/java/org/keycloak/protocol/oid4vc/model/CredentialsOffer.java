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
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    // current implementation only supports pre-authorized codes.
    private PreAuthorizedGrant grants;

    public String getCredentialIssuer() {
        return credentialIssuer;
    }

    public CredentialsOffer setCredentialIssuer(String credentialIssuer) {
        this.credentialIssuer = credentialIssuer;
        return this;
    }

    public List<String> getCredentialConfigurationIds() {
        return credentialConfigurationIds;
    }

    public CredentialsOffer setCredentialConfigurationIds(List<String> credentialConfigurationIds) {
        this.credentialConfigurationIds = Collections.unmodifiableList(credentialConfigurationIds);
        return this;
    }

    public PreAuthorizedGrant getGrants() {
        return grants;
    }

    public CredentialsOffer setGrants(PreAuthorizedGrant grants) {
        this.grants = grants;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CredentialsOffer that)) return false;
        return Objects.equals(getCredentialIssuer(), that.getCredentialIssuer()) && Objects.equals(getCredentialConfigurationIds(), that.getCredentialConfigurationIds()) && Objects.equals(getGrants(), that.getGrants());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCredentialIssuer(), getCredentialConfigurationIds(), getGrants());
    }
}
