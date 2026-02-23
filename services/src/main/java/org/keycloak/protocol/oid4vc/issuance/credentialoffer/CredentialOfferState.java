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
import java.security.SecureRandom;
import java.util.Optional;

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCode;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedGrant;
import org.keycloak.saml.RandomSecret;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialOfferState {

    private CredentialsOffer credentialsOffer;
    private String clientId;
    private String userId;
    private String nonce;
    private String txCode;
    private int expireAt;
    private OID4VCAuthorizationDetail authorizationDetails;

    public CredentialOfferState(CredentialsOffer credOffer, String clientId, String userId, int expireAt) {
        this.credentialsOffer = credOffer;
        this.clientId = clientId;
        this.userId = userId;
        this.expireAt = expireAt;
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

    public void generateTxCode() {
        SecureRandom rnd = new SecureRandom();
        char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(alphabet[rnd.nextInt(alphabet.length)]);
        }
        txCode = sb.toString();
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

    public String getTxCode() {
        return txCode;
    }

    public int getExpireAt() {
        return expireAt;
    }

    public OID4VCAuthorizationDetail getAuthorizationDetails() {
        return authorizationDetails;
    }

    public void setAuthorizationDetails(OID4VCAuthorizationDetail authorizationDetails) {
        this.authorizationDetails = authorizationDetails;
    }

    @Transient
    public boolean isExpired() {
        return expireAt < Time.currentTime();
    }
}
