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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a credentials issuer according to the OID4VCI Credentials Issuer Metadata
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0-15.html#name-credential-issuer-metadata}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialIssuer {

    @JsonProperty("credential_issuer")
    private String credentialIssuer;

    @JsonProperty("credential_endpoint")
    private String credentialEndpoint;

    @JsonProperty("nonce_endpoint")
    private String nonceEndpoint;

    @JsonProperty("deferred_credential_endpoint")
    private String deferredCredentialEndpoint;

    @JsonProperty("authorization_servers")
    private List<String> authorizationServers;

    @JsonProperty("notification_endpoint")
    private String notificationEndpoint;

    @JsonProperty("credential_response_encryption")
    private CredentialResponseEncryption credentialResponseEncryption;

    @JsonProperty("batch_credential_issuance")
    private BatchCredentialIssuance batchCredentialIssuance;

    @JsonProperty("signed_metadata")
    private String signedMetadata;

    @JsonProperty("credential_configurations_supported")
    private Map<String, SupportedCredentialConfiguration> credentialsSupported;

    @JsonProperty("display")
    private List<DisplayObject> display;

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

    public String getNonceEndpoint() {
        return nonceEndpoint;
    }

    public CredentialIssuer setNonceEndpoint(String nonceEndpoint) {
        this.nonceEndpoint = nonceEndpoint;
        return this;
    }

    public String getDeferredCredentialEndpoint() {
        return deferredCredentialEndpoint;
    }

    public CredentialIssuer setDeferredCredentialEndpoint(String deferredCredentialEndpoint) {
        this.deferredCredentialEndpoint = deferredCredentialEndpoint;
        return this;
    }

    public List<String> getAuthorizationServers() {
        return authorizationServers;
    }

    public CredentialIssuer setAuthorizationServers(List<String> authorizationServers) {
        this.authorizationServers = authorizationServers;
        return this;
    }

    public String getNotificationEndpoint() {
        return notificationEndpoint;
    }

    public CredentialIssuer setNotificationEndpoint(String notificationEndpoint) {
        this.notificationEndpoint = notificationEndpoint;
        return this;
    }

    public CredentialResponseEncryption getCredentialResponseEncryption() {
        return credentialResponseEncryption;
    }

    public CredentialIssuer setCredentialResponseEncryption(CredentialResponseEncryption credentialResponseEncryption) {
        this.credentialResponseEncryption = credentialResponseEncryption;
        return this;
    }

    public BatchCredentialIssuance getBatchCredentialIssuance() {
        return batchCredentialIssuance;
    }

    public CredentialIssuer setBatchCredentialIssuance(BatchCredentialIssuance batchCredentialIssuance) {
        this.batchCredentialIssuance = batchCredentialIssuance;
        return this;
    }

    public String getSignedMetadata() {
        return signedMetadata;
    }

    public CredentialIssuer setSignedMetadata(String signedMetadata) {
        this.signedMetadata = signedMetadata;
        return this;
    }

    public Map<String, SupportedCredentialConfiguration> getCredentialsSupported() {
        return credentialsSupported;
    }

    public CredentialIssuer setCredentialsSupported(Map<String, SupportedCredentialConfiguration> credentialsSupported) {
        if (credentialsSupported == null) {
            throw new IllegalArgumentException("credentialsSupported cannot be null");
        }
        this.credentialsSupported = Collections.unmodifiableMap(new HashMap<>(credentialsSupported));
        return this;
    }

    public List<DisplayObject> getDisplay() {
        return display;
    }

    public CredentialIssuer setDisplay(List<DisplayObject> display) {
        this.display = display;
        return this;
    }

    /**
     * Represents the credential_response_encryption metadata parameter.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CredentialResponseEncryption {
        @JsonProperty("alg_values_supported")
        private List<String> algValuesSupported;

        @JsonProperty("enc_values_supported")
        private List<String> encValuesSupported;

        @JsonProperty("encryption_required")
        private Boolean encryptionRequired;

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

        public Boolean getEncryptionRequired() {
            return encryptionRequired;
        }

        public CredentialResponseEncryption setEncryptionRequired(Boolean encryptionRequired) {
            this.encryptionRequired = encryptionRequired;
            return this;
        }
    }

    /**
     * Represents the batch_credential_issuance metadata parameter.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BatchCredentialIssuance {
        @JsonProperty("batch_size")
        private Integer batchSize;

        public Integer getBatchSize() {
            return batchSize;
        }

        public BatchCredentialIssuance setBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
            return this;
        }
    }
}
