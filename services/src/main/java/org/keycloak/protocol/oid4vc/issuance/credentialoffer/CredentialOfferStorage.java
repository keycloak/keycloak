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
import java.util.Objects;

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
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
        private long expiration;
        private OID4VCAuthorizationDetail authorizationDetails;

        public CredentialOfferState(CredentialsOffer credOffer, String clientId, String userId, long expiration) {
            this.credentialsOffer = credOffer;
            this.clientId = clientId;
            this.userId = userId;
            this.expiration = expiration;
            this.nonce = Base64Url.encode(RandomSecret.createRandomSecret(64));
        }

        // For json serialization
        public CredentialOfferState() {
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

        public long getExpiration() {
            return expiration;
        }

        public OID4VCAuthorizationDetail getAuthorizationDetails() {
            return authorizationDetails;
        }

        public CredentialOfferState setAuthorizationDetails(OID4VCAuthorizationDetail authorizationDetails) {
            this.authorizationDetails = authorizationDetails;
            return this;
        }

        public CredentialOfferState setCredentialsOffer(CredentialsOffer credentialsOffer) {
            this.credentialsOffer = credentialsOffer;
            return this;
        }

        public CredentialOfferState setClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public CredentialOfferState setUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public CredentialOfferState setNonce(String nonce) {
            this.nonce = nonce;
            return this;
        }

        public CredentialOfferState setExpiration(long expiration) {
            this.expiration = expiration;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            CredentialOfferState that = (CredentialOfferState) o;
            return getExpiration() == that.getExpiration() && Objects.equals(getCredentialsOffer(), that.getCredentialsOffer()) && Objects.equals(getClientId(), that.getClientId()) && Objects.equals(getUserId(), that.getUserId()) && Objects.equals(getNonce(), that.getNonce()) && Objects.equals(getAuthorizationDetails(), that.getAuthorizationDetails());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getCredentialsOffer(), getClientId(), getUserId(), getNonce(), getExpiration(), getAuthorizationDetails());
        }
    }

    void putOfferState(KeycloakSession session, CredentialOfferState entry);

    CredentialOfferState findOfferStateByNonce(KeycloakSession session, String nonce);

    CredentialOfferState findOfferStateByCredentialId(KeycloakSession session, String credId);

    void replaceOfferState(KeycloakSession session, CredentialOfferState entry);

    void removeOfferState(KeycloakSession session, CredentialOfferState entry);

    @Override
    default void close() {
    }
}
