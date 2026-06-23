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

import org.keycloak.representations.idm.RoleRepresentation;

/**
 * Keycloak specific constants related to OID4VC and related functionality. Useful for example for internal constants (EG. name of Keycloak realm attributes).
 * For protocol constants defined in the specification, see {@link org.keycloak.OID4VCConstants}
 *
 * @author Pascal Knüppel
 */
public final class OID4VCIConstants {

    public static final String OID4VC_PROTOCOL = "oid4vc";

    public static final String C_NONCE_LIFETIME_IN_SECONDS = "vc.c-nonce-lifetime-seconds";

    public static final String TIME_CLAIMS_STRATEGY = "oid4vci.time.claims.strategy";
    public static final String TIME_RANDOMIZE_WINDOW_SECONDS = "oid4vci.time.randomize.window.seconds";
    public static final String TIME_ROUND_UNIT = "oid4vci.time.round.unit";

    // --- Keybinding/Credential Builder ---
    public static final String SOURCE_ENDPOINT = "source_endpoint";
    public static final String BATCH_CREDENTIAL_ISSUANCE_BATCH_SIZE = "batch_credential_issuance.batch_size";
    // Comma-separated aliases of trust-material identity providers that expose trusted keys for key attestation proof validation
    public static final String OID4VCI_ATTESTER_TRUST_IDPS_ATTR = "oid4vci.attester_trust_idps";

    public static final RoleRepresentation CREDENTIAL_OFFER_CREATE =
            new RoleRepresentation("credential-offer-create", "Allow credential offer creation", false);

    // Required-action provider ID
    public static final String VERIFIABLE_CREDENTIAL_OFFER_PROVIDER_ID = "verifiable_credential_offer";

    // authenticationSession note with the stored handle for the credential-offer
    public static final String CREDENTIAL_OFFER_NONCE = "credential_offer_nonce";

    // authenticationSession note with the info whether the credential-offer action was initiated by admin (true) or initiated by user through AIA (false or missing)
    public static final String IS_ADMIN_INITIATED = "is_admin_initiated";

    // Attribute used in the credential-offer email, which is sent by the admin to the user
    public static final String EMAIL_TEMPLATE_ATTR_CREDENTIAL_SCOPE_DISPLAY_NAME = "credentialScopeDisplayName";

    private OID4VCIConstants() {
    }
}
