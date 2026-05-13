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
package org.keycloak.protocol.oid4vc.presentation.verification;

import java.util.LinkedHashMap;
import java.util.Map;

public class CredentialVerificationResult {

    private String format;
    private String issuer;
    private String credentialType;
    private Map<String, Object> claims = new LinkedHashMap<>();

    public String getFormat() {
        return format;
    }

    public CredentialVerificationResult setFormat(String format) {
        this.format = format;
        return this;
    }

    public String getIssuer() {
        return issuer;
    }

    public CredentialVerificationResult setIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public String getCredentialType() {
        return credentialType;
    }

    public CredentialVerificationResult setCredentialType(String credentialType) {
        this.credentialType = credentialType;
        return this;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    public CredentialVerificationResult setClaims(Map<String, Object> claims) {
        this.claims = claims != null ? new LinkedHashMap<>(claims) : new LinkedHashMap<>();
        return this;
    }
}
