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
package org.keycloak.protocol.oid4vc.issuance.credentialoffer;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.saml.RandomSecret;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialOfferState {

    private String credentialsOfferId;
    private CredentialsOffer credentialsOffer;
    private String targetClientId;
    private String targetUserId;
    private String nonce;
    private String txCode;
    private long expiresAt;
    private List<OID4VCAuthorizationDetail> authDetails;

    /**
     * Create a new CredentialOfferState.
     * <p>
     * This should only be called from the configured  {@code CredentialOfferProvider}.
     * The constructor is public for testing purposes only.
     *
     * @param credOffer   The credential offer
     * @param clientId    The target client_id
     * @param userId      The target user id
     * @param expiresAt    The expiry date of the offer in seconds
     * @param authDetailsProvider A provider function for authorization details, (optionally) one for each credential_configuration_id
     */
    public CredentialOfferState(
            CredentialsOffer credOffer,
            String clientId,
            String userId,
            long expiresAt,
            Function<String, List<OID4VCAuthorizationDetail>> authDetailsProvider
    ) {
        this.credentialsOfferId = Base64Url.encode(RandomSecret.createRandomSecret(64));
        this.credentialsOffer = credOffer;
        this.targetClientId = clientId;
        this.targetUserId = userId;
        this.expiresAt = expiresAt;
        this.nonce = Base64Url.encode(RandomSecret.createRandomSecret(64));
        if (authDetailsProvider != null) {
            this.authDetails = authDetailsProvider.apply(credentialsOfferId);
        }
    }

    @JsonIgnore
    public Optional<String> getPreAuthorizedCode() {
        return Optional.ofNullable(credentialsOffer.getPreAuthorizedCode());
    }

    public String getCredentialsOfferId() {
        return credentialsOfferId;
    }

    public CredentialsOffer getCredentialsOffer() {
        return credentialsOffer;
    }

    public String getTargetClientId() {
        return targetClientId;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public String getNonce() {
        return nonce;
    }

    public String getTxCode() {
        return txCode;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public List<OID4VCAuthorizationDetail> getAuthorizationDetails() {
        return Collections.unmodifiableList(authDetails != null ? authDetails : List.of());
    }

    public OID4VCAuthorizationDetail getAuthorizationDetails(String credConfigId) {
        return getAuthorizationDetails().stream()
                .filter(it -> it.getCredentialConfigurationId().equals(credConfigId))
                .findFirst()
                .map(OID4VCAuthorizationDetail::clone)
                .orElse(null);
    }

    @JsonIgnore
    public boolean isExpired() {
        int currentTime = Time.currentTime();
        return expiresAt <= currentTime;
    }

    // Private ---------------------------------------------------------------------------------------------------------

    // For json serialization
    private CredentialOfferState() {
    }

    // For json serialization
    private void setAuthorizationDetails(List<OID4VCAuthorizationDetail> authDetails) {
        this.authDetails = authDetails;
    }
}
