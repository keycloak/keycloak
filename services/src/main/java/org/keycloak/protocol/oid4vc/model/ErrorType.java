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


/**
 * Enum to handle potential errors in issuing credentials with the error types defined in OID4VCI
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0-16.html}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public enum ErrorType {

    INVALID_CREDENTIAL_REQUEST("invalid_credential_request"),
    INVALID_TOKEN("invalid_token"),
    UNKNOWN_CREDENTIAL_CONFIGURATION("unknown_credential_configuration"),
    UNKNOWN_CREDENTIAL_IDENTIFIER("unknown_credential_identifier"),
    INVALID_PROOF("invalid_proof"),
    INVALID_NONCE("invalid_nonce"),
    INVALID_ENCRYPTION_PARAMETERS("invalid_encryption_parameters"),
    MISSING_CREDENTIAL_CONFIG("missing_credential_config"),
    MISSING_CREDENTIAL_IDENTIFIER_AND_CONFIGURATION_ID("missing_credential_identifier_and_configuration_id");

    private final String value;

    ErrorType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
