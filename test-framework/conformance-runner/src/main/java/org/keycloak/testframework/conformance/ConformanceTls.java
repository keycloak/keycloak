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

package org.keycloak.testframework.conformance;

/**
 * TLS settings shared by the conformance test servers.
 *
 * FAPI2 requires the TLS layer to only offer BCP195 (RFC 9325) recommended cipher suites for TLS 1.2. The default JVM
 * cipher list includes non-recommended suites, which the conformance suite TLS checks reject, so the test servers are
 * restricted to the recommended AEAD suites. The TLS 1.3 suites must be listed too, as restricting the cipher list
 * would otherwise disable TLS 1.3 entirely and the suite checks that TLS 1.3 is negotiable where offered.
 */
public final class ConformanceTls {

    public static final String PROTOCOLS = "TLSv1.3,TLSv1.2";
    public static final String BCP195_CIPHERS = String.join(",",
            "TLS_AES_128_GCM_SHA256",
            "TLS_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");

    private ConformanceTls() {
    }
}
