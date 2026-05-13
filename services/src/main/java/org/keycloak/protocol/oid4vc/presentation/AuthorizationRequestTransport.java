/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oid4vc.presentation;

import java.util.Arrays;

// Configures whether the Authorization Request is sent by value (Section 5) or by reference with request_uri (Section 5.10).
public enum AuthorizationRequestTransport {

    QUERY_PARAMETERS("query_parameters"),
    REQUEST_URI("request_uri");

    private final String value;

    AuthorizationRequestTransport(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AuthorizationRequestTransport fromConfig(OID4VPIdentityProviderConfig config) {
        String configured = config.getConfig().get(OID4VPIdentityProviderConfig.AUTHORIZATION_REQUEST_TRANSPORT);
        if (configured == null || configured.isBlank()) {
            return QUERY_PARAMETERS;
        }

        return Arrays.stream(values())
                .filter(method -> method.value.equals(configured))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported OID4VP authorization request transport: " + configured));
    }
}
