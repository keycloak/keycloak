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

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.oid4vci.CredentialScopeModel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents credential_metadata as defined in the OID4VCI specification.
 * Contains information relevant to the usage and display of issued Credentials.
 * Format-specific mechanisms can overwrite the information in this object.
 *
 * @author <a href="https://github.com/forkimenjeckayang">Forkim Akwichek</a>
 * @see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0-16.html#name-credential-issuer-metadata-p
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialMetadata {

    @JsonProperty("display")
    private List<DisplayObject> display;

    @JsonProperty("claims")
    private Claims claims;

    /**
     * Parse credential metadata from a credential scope model.
     * Format-specific mechanisms (like SD-JWT VC display metadata) are always preferred by the Wallet
     * over the information in this object, which serves as the default fallback.
     *
     * @param keycloakSession The Keycloak session
     * @param credentialScope The credential scope model
     * @return The parsed credential metadata, or null if no metadata is available
     */
    public static CredentialMetadata parse(KeycloakSession keycloakSession, CredentialScopeModel credentialScope) {
        CredentialMetadata metadata = new CredentialMetadata();

        // Parse format-specific display metadata (prioritized)
        List<DisplayObject> formatSpecificDisplay = DisplayObject.parse(credentialScope);
        if (formatSpecificDisplay != null && !formatSpecificDisplay.isEmpty()) {
            metadata.setDisplay(formatSpecificDisplay);
        }

        // Parse format-specific claims metadata (prioritized)
        Claims formatSpecificClaims = Claims.parse(keycloakSession, credentialScope);
        if (formatSpecificClaims != null && !formatSpecificClaims.isEmpty()) {
            metadata.setClaims(formatSpecificClaims);
        }

        // Only return metadata if we have some content
        if (metadata.getDisplay() != null || metadata.getClaims() != null) {
            return metadata;
        }

        return null;
    }

    public List<DisplayObject> getDisplay() {
        return display;
    }

    public CredentialMetadata setDisplay(List<DisplayObject> display) {
        this.display = display;
        return this;
    }

    public Claims getClaims() {
        return claims;
    }

    public CredentialMetadata setClaims(Claims claims) {
        this.claims = claims;
        return this;
    }
} 
