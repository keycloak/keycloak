/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.webauthn.realm;

import org.keycloak.representations.idm.RealmRepresentation;

import java.util.List;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class WebAuthnRealmConfigBuilder extends AbstractWebAuthnRealmConfigBuilder<WebAuthnRealmConfigBuilder> {
    public WebAuthnRealmConfigBuilder(RealmRepresentation rep) {
        super(rep);
    }

    public RealmRepresentation build() {
        return rep;
    }

    @Override
    public WebAuthnRealmConfigBuilder setWebAuthnPolicyRpEntityName(String webAuthnPolicyRpEntityName) {
        rep.setWebAuthnPolicyRpEntityName(webAuthnPolicyRpEntityName);
        return this;
    }

    @Override
    public WebAuthnRealmConfigBuilder setWebAuthnPolicyCreateTimeout(Integer webAuthnPolicyCreateTimeout) {
        rep.setWebAuthnPolicyCreateTimeout(webAuthnPolicyCreateTimeout);
        return this;
    }

    @Override
    public WebAuthnRealmConfigBuilder setWebAuthnPolicyAvoidSameAuthenticatorRegister(Boolean webAuthnPolicyAvoidSameAuthenticatorRegister) {
        rep.setWebAuthnPolicyAvoidSameAuthenticatorRegister(webAuthnPolicyAvoidSameAuthenticatorRegister);
        return this;
    }

    @Override
    public WebAuthnRealmConfigBuilder setWebAuthnPolicySignatureAlgorithms(List<String> webAuthnPolicySignatureAlgorithms) {
        rep.setWebAuthnPolicySignatureAlgorithms(webAuthnPolicySignatureAlgorithms);
        return this;
    }

    @Override
    public WebAuthnRealmConfigBuilder setWebAuthnPolicyAttestationConveyancePreference(String webAuthnPolicyAttestationConveyancePreference) {
        rep.setWebAuthnPolicyAttestationConveyancePreference(webAuthnPolicyAttestationConveyancePreference);
        return this;
    }

    @Override
    public WebAuthnRealmConfigBuilder setWebAuthnPolicyAuthenticatorAttachment(String webAuthnPolicyAuthenticatorAttachment) {
        rep.setWebAuthnPolicyAuthenticatorAttachment(webAuthnPolicyAuthenticatorAttachment);
        return this;
    }

    @Override
    public WebAuthnRealmConfigBuilder setWebAuthnPolicyRequireResidentKey(String webAuthnPolicyRequireResidentKey) {
        rep.setWebAuthnPolicyRequireResidentKey(webAuthnPolicyRequireResidentKey);
        return this;
    }

    @Override
    public WebAuthnRealmConfigBuilder setWebAuthnPolicyRpId(String webAuthnPolicyRpId) {
        rep.setWebAuthnPolicyRpId(webAuthnPolicyRpId);
        return this;
    }

    @Override
    public WebAuthnRealmConfigBuilder setWebAuthnPolicyUserVerificationRequirement(String webAuthnPolicyUserVerificationRequirement) {
        rep.setWebAuthnPolicyUserVerificationRequirement(webAuthnPolicyUserVerificationRequirement);
        return this;
    }

    @Override
    public WebAuthnRealmConfigBuilder setWebAuthnPolicyAcceptableAaguids(List<String> webAuthnPolicyAcceptableAaguids) {
        rep.setWebAuthnPolicyAcceptableAaguids(webAuthnPolicyAcceptableAaguids);
        return this;
    }

    @Override
    public WebAuthnRealmConfigBuilder setWebAuthnPolicyPasskeysEnabled(Boolean webAuthnPolicyPasskeysEnabled) {
        // passkeys are only used in passwordless policy
        return this;
    }

}
