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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Represents the Credential Issuer Metadata as per OID4VCI spec
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-11.2.2}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialIssuer {

    @JsonProperty("credential_issuer")
    private String credentialIssuer;

    @JsonProperty("credential_endpoint")
    private String credentialEndpoint;

    @JsonProperty("credentials_supported")
    private Map<String, SupportedCredentialConfiguration> credentialsSupported;

    @JsonProperty("authorization_servers")
    private List<String> authorizationServers;

    @JsonProperty("credential_response_encryption")
    private CredentialResponseEncryption credentialResponseEncryption;

    public String getCredentialIssuer() {
        return credentialIssuer;
    }

    public CredentialIssuer setCredentialIssuer(String credentialIssuer) {
        this.credentialIssuer = credentialIssuer;
        return this;
    }

    public String getCredentialEndpoint() {
        return credentialEndpoint;
    }

    public CredentialIssuer setCredentialEndpoint(String credentialEndpoint) {
        this.credentialEndpoint = credentialEndpoint;
        return this;
    }

    public Map<String, SupportedCredentialConfiguration> getCredentialsSupported() {
        return credentialsSupported;
    }

    public CredentialIssuer setCredentialsSupported(Map<String, SupportedCredentialConfiguration> credentialsSupported) {
        this.credentialsSupported = credentialsSupported;
        return this;
    }

    public List<String> getAuthorizationServers() {
        return authorizationServers;
    }

    public CredentialIssuer setAuthorizationServers(List<String> authorizationServers) {
        this.authorizationServers = authorizationServers;
        return this;
    }

    public CredentialResponseEncryption getCredentialResponseEncryption() {
        return credentialResponseEncryption;
    }

    public CredentialIssuer setCredentialResponseEncryption(CredentialResponseEncryption credentialResponseEncryption) {
        this.credentialResponseEncryption = credentialResponseEncryption;
        return this;
    }

    /**
     * Represents the credential_response_encryption metadata as per OID4VCI spec
     * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-11.2.2}
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CredentialResponseEncryption {
        @JsonProperty("alg_values_supported")
        private List<String> algValuesSupported;

        @JsonProperty("enc_values_supported")
        private List<String> encValuesSupported;

        public List<String> getAlgValuesSupported() {
            return algValuesSupported;
        }

        public CredentialResponseEncryption setAlgValuesSupported(List<String> algValuesSupported) {
            this.algValuesSupported = algValuesSupported;
            return this;
        }

        public List<String> getEncValuesSupported() {
            return encValuesSupported;
        }

        public CredentialResponseEncryption setEncValuesSupported(List<String> encValuesSupported) {
            this.encValuesSupported = encValuesSupported;
            return this;
        }
    }
}
