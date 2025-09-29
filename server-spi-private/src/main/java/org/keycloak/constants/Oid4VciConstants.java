/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.constants;

/**
 * @author Pascal Knüppel
 */
public final class Oid4VciConstants {

    public static final String OID4VC_PROTOCOL = "oid4vc";

    public static final String C_NONCE_LIFETIME_IN_SECONDS = "vc.c-nonce-lifetime-seconds";

    public static final String CREDENTIAL_SUBJECT = "credentialSubject";

    public static final String SIGNED_METADATA_JWT_TYPE = "openidvci-issuer-metadata+jwt";

    // --- Endpoints/Well-Known ---
    public static final String WELL_KNOWN_OPENID_CREDENTIAL_ISSUER = "openid-credential-issuer";
    public static final String RESPONSE_TYPE_IMG_PNG = "image/png";
    public static final String CREDENTIAL_OFFER_URI_CODE_SCOPE = "credential-offer";

    // --- Keybinding/Credential Builder ---
    public static final String SOURCE_ENDPOINT = "source_endpoint";
    public static final String BATCH_CREDENTIAL_ISSUANCE_BATCH_SIZE = "batch_credential_issuance.batch_size";

    private Oid4VciConstants() {
    }
}
