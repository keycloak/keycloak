/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.jpa.entities;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface RealmAttributes {

    String DISPLAY_NAME = "displayName";

    String DISPLAY_NAME_HTML = "displayNameHtml";

    String ACTION_TOKEN_GENERATED_BY_ADMIN_LIFESPAN = "actionTokenGeneratedByAdminLifespan";

    String ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN = "actionTokenGeneratedByUserLifespan";

    // KEYCLOAK-7688 Offline Session Max for Offline Token
    String OFFLINE_SESSION_MAX_LIFESPAN_ENABLED = "offlineSessionMaxLifespanEnabled";

    String OFFLINE_SESSION_MAX_LIFESPAN = "offlineSessionMaxLifespan";
    
    String WEBAUTHN_POLICY_RP_ENTITY_NAME = "webAuthnPolicyRpEntityName";
    String WEBAUTHN_POLICY_SIGNATURE_ALGORITHMS = "webAuthnPolicySignatureAlgorithms";

    String WEBAUTHN_POLICY_RP_ID = "webAuthnPolicyRpId";
    String WEBAUTHN_POLICY_ATTESTATION_CONVEYANCE_PREFERENCE = "webAuthnPolicyAttestationConveyancePreference";
    String WEBAUTHN_POLICY_AUTHENTICATOR_ATTACHMENT = "webAuthnPolicyAuthenticatorAttachment";
    String WEBAUTHN_POLICY_REQUIRE_RESIDENT_KEY = "webAuthnPolicyRequireResidentKey";
    String WEBAUTHN_POLICY_USER_VERIFICATION_REQUIREMENT = "webAuthnPolicyUserVerificationRequirement";
    String WEBAUTHN_POLICY_CREATE_TIMEOUT = "webAuthnPolicyCreateTimeout";
    String WEBAUTHN_POLICY_AVOID_SAME_AUTHENTICATOR_REGISTER = "webAuthnPolicyAvoidSameAuthenticatorRegister";
    String WEBAUTHN_POLICY_ACCEPTABLE_AAGUIDS = "webAuthnPolicyAcceptableAaguids";

}
