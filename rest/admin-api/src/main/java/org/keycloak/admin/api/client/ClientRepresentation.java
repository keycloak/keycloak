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

package org.keycloak.admin.api.client;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.Set;

public record ClientRepresentation(
    @JsonPropertyDescription("ID uniquely identifying this client")
    String clientId,

    @JsonPropertyDescription("Human readable name of the client")
    String displayName,

    @JsonPropertyDescription("Human readable description of the client")
    String description,

    @JsonPropertyDescription("The protocol used to communicate with the client")
    String protocol,

    @JsonPropertyDescription("Whether this client is enabled")
    Boolean enabled,

    @JsonPropertyDescription("URL to the application's homepage that is represented by this client")
    String appUrl,

    @JsonPropertyDescription("URLs that the browser can redirect to after login")
    Set<String> appRedirectUrls,

    @JsonPropertyDescription("Login flows that are enabled for this client")
    Set<String> loginFlows,

    @JsonPropertyDescription("Authentication configuration for this client")
    Auth auth,

    @JsonPropertyDescription("Web origins that are allowed to make requests to this client")
    Set<String> webOrigins,

    @JsonPropertyDescription("Roles associated with this client")
    Set<String> roles,

    @JsonPropertyDescription("Service account configuration for this client")
    ServiceAccount serviceAccount
) {
    public record Auth(
            @JsonPropertyDescription("Whether authentication is enabled for this client")
            Boolean enabled,

            @JsonPropertyDescription("Which authentication method is used for this client")
            String method,

            @JsonPropertyDescription("Secret used to authenticate this client with Secret authentication")
            String secret,

            @JsonPropertyDescription("Public key used to authenticate this client with Signed JWT authentication")
            String certificate
    ) {}

    public record ServiceAccount(
            @JsonPropertyDescription("Whether the service account is enabled")
            Boolean enabled,

            @JsonPropertyDescription("Roles assigned to the service account")
            Set<String> roles
    ) {}
}
