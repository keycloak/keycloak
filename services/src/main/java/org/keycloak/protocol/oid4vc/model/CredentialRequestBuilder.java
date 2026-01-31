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

public class CredentialRequestBuilder {

    private String credentialConfigurationId;
    private String credentialIdentifier;
    private CredentialDefinition credentialDefinition;
    private CredentialResponseEncryption credentialResponseEncryption;
    private Proofs proofs;

    public CredentialRequestBuilder withCredentialConfigurationId(String credentialConfigurationId) {
        this.credentialConfigurationId = credentialConfigurationId;
        return this;
    }

    public CredentialRequestBuilder withCredentialIdentifier(String credentialIdentifier) {
        this.credentialIdentifier = credentialIdentifier;
        return this;
    }

    public CredentialRequestBuilder withCredentialDefinition(CredentialDefinition credentialDefinition) {
        this.credentialDefinition = credentialDefinition;
        return this;
    }

    public CredentialRequestBuilder withCredentialResponseEncryption(CredentialResponseEncryption credentialResponseEncryption) {
        this.credentialResponseEncryption = credentialResponseEncryption;
        return this;
    }

    public CredentialRequestBuilder withProofs(Proofs proofs) {
        this.proofs = proofs;
        return this;
    }

    public CredentialRequest build() {
        CredentialRequest credRequest = new CredentialRequest();
        credRequest.setCredentialConfigurationId(credentialConfigurationId);
        credRequest.setCredentialIdentifier(credentialIdentifier);
        credRequest.setCredentialDefinition(credentialDefinition);
        credRequest.setCredentialResponseEncryption(credentialResponseEncryption);
        credRequest.setProofs(proofs);
        return credRequest;
    }
}
