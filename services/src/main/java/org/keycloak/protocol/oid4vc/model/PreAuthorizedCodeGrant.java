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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Container for the pre-authorized code to be used in a Credential Offer
 * <p>
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-offer}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreAuthorizedCodeGrant implements CredentialOfferGrant {

    public static final String PRE_AUTH_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:pre-authorized_code";
    public static final String AUTHORIZATION_SERVER_PARAM = "authorization_server";
    public static final String CODE_REQUEST_PARAM = "pre-authorized_code";
    public static final String TX_CODE_PARAM = "tx_code";

    @Override
    @JsonIgnore
    public String getGrantType() {
        return PRE_AUTH_GRANT_TYPE;
    }

    @JsonProperty(CODE_REQUEST_PARAM)
    private String preAuthorizedCode;

    @JsonProperty(TX_CODE_PARAM)
    private TxCode txCode;

    @JsonProperty(AUTHORIZATION_SERVER_PARAM)
    private String authorizationServer;

    public String getPreAuthorizedCode() {
        return preAuthorizedCode;
    }

    public PreAuthorizedCodeGrant setPreAuthorizedCode(String preAuthorizedCode) {
        this.preAuthorizedCode = preAuthorizedCode;
        return this;
    }

    public TxCode getTxCode() {
        return txCode;
    }

    public PreAuthorizedCodeGrant setTxCode(TxCode txCode) {
        this.txCode = txCode;
        return this;
    }

    public String getAuthorizationServer() {
        return authorizationServer;
    }

    public PreAuthorizedCodeGrant setAuthorizationServer(String authorizationServer) {
        this.authorizationServer = authorizationServer;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PreAuthorizedCodeGrant that)) return false;
        return Objects.equals(preAuthorizedCode, that.preAuthorizedCode)
                && Objects.equals(txCode, that.txCode) && Objects.equals(authorizationServer, that.authorizationServer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(preAuthorizedCode, txCode, authorizationServer);
    }
}
