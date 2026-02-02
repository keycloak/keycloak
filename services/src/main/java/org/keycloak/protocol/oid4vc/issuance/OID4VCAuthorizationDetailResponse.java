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
import java.util.Objects;

import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OID4VCI-specific authorization details response that extends the generic response
 * with OID4VCI-specific fields like credential_identifiers.
 *
 * @author <a href="mailto:Forkim.Akwichek@adorsys.com">Forkim Akwichek</a>
 */
public class OID4VCAuthorizationDetailResponse extends OID4VCAuthorizationDetail {

    public static final String CREDENTIAL_IDENTIFIERS = "credential_identifiers";

    @JsonProperty(CREDENTIAL_IDENTIFIERS)
    private List<String> credentialIdentifiers;

    public List<String> getCredentialIdentifiers() {
        return credentialIdentifiers;
    }

    public void setCredentialIdentifiers(List<String> credentialIdentifiers) {
        this.credentialIdentifiers = credentialIdentifiers;
    }

    @Override
    public String toString() {
        return "OID4VCAuthorizationDetailsResponse {" +
                " type='" + getType() + '\'' +
                ", locations='" + getLocations() + '\'' +
                ", credentialConfigurationId='" + getCredentialConfigurationId() + '\'' +
                ", credentialIdentifiers=" + credentialIdentifiers +
                ", claims=" + getClaims() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        OID4VCAuthorizationDetailResponse that = (OID4VCAuthorizationDetailResponse) o;
        return Objects.equals(credentialIdentifiers, that.credentialIdentifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), credentialIdentifiers);
    }
}
