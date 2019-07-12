/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.credential;

import com.webauthn4j.data.WebAuthnAuthenticationContext;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.statement.AttestationStatement;

public class WebAuthnCredentialModel implements CredentialInput {

    public static final String WEBAUTHN_CREDENTIAL_TYPE = "webauthn";
    private AttestedCredentialData attestedCredentialData;
    private AttestationStatement attestationStatement;
    private WebAuthnAuthenticationContext authenticationContext;
    private long count;
    private String authenticatorId;

    @Override
    public String getType() {
        return WEBAUTHN_CREDENTIAL_TYPE;
    }

    public WebAuthnCredentialModel() {

    }

    public AttestedCredentialData getAttestedCredentialData() {
        return attestedCredentialData;
    }

    public AttestationStatement getAttestationStatement() {
        return attestationStatement;
    }

    public long getCount() {
        return count;
    }

    public WebAuthnAuthenticationContext getAuthenticationContext() {
        return authenticationContext;
    }

    public void setAuthenticationContext(WebAuthnAuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
    }

    public void setAttestedCredentialData(AttestedCredentialData attestedCredentialData) {
        this.attestedCredentialData = attestedCredentialData;
    }

    public void setAttestationStatement(AttestationStatement attestationStatement) {
        this.attestationStatement = attestationStatement;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public String getAuthenticatorId() {
        return authenticatorId;
    }

    public void setAuthenticatorId(String authenticatorId) {
        this.authenticatorId = authenticatorId;
    }
}
