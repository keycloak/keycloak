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

import org.keycloak.jose.jwk.JWK;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents the credential_response_encryption object in a Credential Request.
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-request}
 *
 * @author <a href="mailto:Bertrand.Ogen@adorsys.com">Bertrand Ogen</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialResponseEncryption {

    /**
     * REQUIRED. A string specifying the content encryption algorithm to be used for encrypting the
     * Credential Response, as per the supported content encryption algorithms in the Credential Issuer Metadata.
     */
    private String enc;

    /**
     * OPTIONAL. A string specifying the compression algorithm to be used for compressing the
     * Credential Response prior to encryption.
     */
    private String zip;

    /**
     * REQUIRED if credential_response_encryption is included in the Credential Request.
     * A JSON Web Key (JWK) that represents the public key to which the Credential Response will be encrypted.
     */
    private JWK jwk;

    public String getEnc() {
        return enc;
    }

    public CredentialResponseEncryption setEnc(String enc) {
        this.enc = enc;
        return this;
    }

    public String getZip() {
        return zip;
    }

    public CredentialResponseEncryption setZip(String zip) {
        this.zip = zip;
        return this;
    }

    public JWK getJwk() {
        return jwk;
    }

    public CredentialResponseEncryption setJwk(JWK jwk) {
        this.jwk = jwk;
        return this;
    }
}
