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
import java.util.List;
import java.util.Map;

/**
 * Represents a credentials issuer according to the OID4VCI Credentials Issuer Metadata
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-issuer-metadata}
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

    @JsonProperty("authorization_servers")
    private List<String> authorizationServers;

    @JsonProperty("notification_endpoint")
    private String notificationEndpoint;

    @JsonProperty("credential_configurations_supported")
    private Map<String, SupportedCredentialConfiguration> credentialsSupported;

    private DisplayObject display;

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

    public Map<String, SupportedCredentialConfiguration> getCredentialsSupported() {
        return credentialsSupported;
    }

    public CredentialIssuer setCredentialsSupported(Map<String, SupportedCredentialConfiguration> credentialsSupported) {
        this.credentialsSupported = Collections.unmodifiableMap(credentialsSupported);
        return this;
    }

    public DisplayObject getDisplay() {
        return display;
    }

    public CredentialIssuer setDisplay(DisplayObject display) {
        this.display = display;
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
}

