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

import java.beans.Transient;
import java.util.Optional;

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCode;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedGrant;
import org.keycloak.provider.Provider;
import org.keycloak.saml.RandomSecret;

import com.fasterxml.jackson.annotation.JsonInclude;

public interface CredentialOfferStorage extends Provider {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    class CredentialOfferState {

        private CredentialsOffer credentialsOffer;
        private String clientId;
        private String userId;
        private String nonce;
        private int expiration;
        private OID4VCAuthorizationDetailResponse authorizationDetails;

        public CredentialOfferState(CredentialsOffer credOffer, String clientId, String userId, int expiration) {
            this.credentialsOffer = credOffer;
            this.clientId = clientId;
            this.userId = userId;
            this.expiration = expiration;
            this.nonce = Base64Url.encode(RandomSecret.createRandomSecret(64));
        }

        // For json serialization
        CredentialOfferState() {
        }

        @Transient
        public Optional<String> getPreAuthorizedCode() {
            return Optional.ofNullable(credentialsOffer.getGrants())
                    .map(PreAuthorizedGrant::getPreAuthorizedCode)
                    .map(PreAuthorizedCode::getPreAuthorizedCode);
        }

        @Transient
        public boolean isExpired() {
            int currentTime = Time.currentTime();
            return currentTime > expiration;
        }

        public CredentialsOffer getCredentialsOffer() {
            return credentialsOffer;
        }

        public String getClientId() {
            return clientId;
        }

        public String getUserId() {
            return userId;
        }

        public String getNonce() {
            return nonce;
        }

        public int getExpiration() {
            return expiration;
        }

        public OID4VCAuthorizationDetailResponse getAuthorizationDetails() {
            return authorizationDetails;
        }

        public void setAuthorizationDetails(OID4VCAuthorizationDetailResponse authorizationDetails) {
            this.authorizationDetails = authorizationDetails;
        }

        void setCredentialsOffer(CredentialsOffer credentialsOffer) {
            this.credentialsOffer = credentialsOffer;
        }

        void setClientId(String clientId) {
            this.clientId = clientId;
        }

        void setUserId(String userId) {
            this.userId = userId;
        }

        void setNonce(String nonce) {
            this.nonce = nonce;
        }

        void setExpiration(int expiration) {
            this.expiration = expiration;
        }
    }

    void putOfferState(KeycloakSession session, CredentialOfferState entry);

    CredentialOfferState findOfferStateByNonce(KeycloakSession session, String nonce);

    CredentialOfferState findOfferStateByCode(KeycloakSession session, String code);

    CredentialOfferState findOfferStateByCredentialId(KeycloakSession session, String credId);

    void replaceOfferState(KeycloakSession session, CredentialOfferState entry);

    void removeOfferState(KeycloakSession session, CredentialOfferState entry);

    @Override
    default void close() {
    }
}
