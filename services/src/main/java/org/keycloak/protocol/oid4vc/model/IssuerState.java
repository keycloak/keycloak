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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Container for the issuer state used by the authorization code grant in a Credential Offer
 * <p>
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-offer}
 *
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IssuerState {

    @JsonProperty("credential_offer_id")
    private String credentialOfferId;

    public String getCredentialOfferId() {
        return credentialOfferId;
    }

    public IssuerState setCredentialOfferId(String credentialOfferId) {
        this.credentialOfferId = credentialOfferId;
        return this;
    }

    public static IssuerState fromEncodedString(String encoded) {
        byte[] encodedBytes = encoded.getBytes(StandardCharsets.UTF_8);
        String value = new String(Base64.getUrlDecoder().decode(encodedBytes));
        return JsonSerialization.valueFromString(value, IssuerState.class);
    }

    public String encodeToString() {
        String value = JsonSerialization.valueAsString(this);
        return Base64.getUrlEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IssuerState grant)) return false;
        return Objects.equals(credentialOfferId, grant.credentialOfferId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(credentialOfferId);
    }
}
