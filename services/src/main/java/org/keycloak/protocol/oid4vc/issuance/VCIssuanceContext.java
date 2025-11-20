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
package org.keycloak.protocol.oid4vc.issuance;

import java.util.List;

import org.keycloak.jose.jwk.JWK;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBody;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.services.managers.AuthenticationManager;

/**
 * Holds the verifiable credential to sign and additional context information.
 *
 * Helps keeps the {@link VerifiableCredential} as clean pojo. Without any risk to
 * mistakenly serialize unwanted information.
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class VCIssuanceContext {

    private CredentialBody credentialBody;
    private SupportedCredentialConfiguration credentialConfig;
    private CredentialRequest credentialRequest;
    private AuthenticationManager.AuthResult authResult;

    private List<JWK> attestedKeys;


    public CredentialBody getCredentialBody() {
        return credentialBody;
    }

    public VCIssuanceContext setCredentialBody(CredentialBody credentialBody) {
        this.credentialBody = credentialBody;
        return this;
    }

    public SupportedCredentialConfiguration getCredentialConfig() {
        return credentialConfig;
    }

    public VCIssuanceContext setCredentialConfig(SupportedCredentialConfiguration credentialConfig) {
        this.credentialConfig = credentialConfig;
        return this;
    }

    public CredentialRequest getCredentialRequest() {
        return credentialRequest;
    }

    public VCIssuanceContext setCredentialRequest(CredentialRequest credentialRequest) {
        this.credentialRequest = credentialRequest;
        return this;
    }

    public AuthenticationManager.AuthResult getAuthResult() {
        return authResult;
    }

    public VCIssuanceContext setAuthResult(AuthenticationManager.AuthResult authResult) {
        this.authResult = authResult;
        return this;
    }

    public VCIssuanceContext setAttestedKeys(List<JWK> attestedKeys) {
        this.attestedKeys = attestedKeys;
        return this;
    }
}
