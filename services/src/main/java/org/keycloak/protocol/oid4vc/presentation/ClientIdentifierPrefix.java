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

// OpenID4VP 1.0 Section 5.9 defines Client Identifier Prefixes and their client_id syntax.
public enum ClientIdentifierPrefix {

    REDIRECT_URI("redirect_uri"),
    X509_SAN_DNS("x509_san_dns"),
    X509_HASH("x509_hash");

    private final String value;

    ClientIdentifierPrefix(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ClientIdentifierPrefix fromConfig(OID4VPIdentityProviderConfig config) {
        String configured = config.getConfig().get(OID4VPIdentityProviderConfig.CLIENT_IDENTIFIER_PREFIX);
        if (configured == null || configured.isBlank()) {
            return REDIRECT_URI;
        }

        return Arrays.stream(values())
                .filter(prefix -> prefix.value.equals(configured))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported OID4VP Client Identifier Prefix: " + configured));
    }
}
