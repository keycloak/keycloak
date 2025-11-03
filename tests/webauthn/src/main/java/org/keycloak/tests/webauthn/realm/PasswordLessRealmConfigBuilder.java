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
public class PasswordLessRealmConfigBuilder extends AbstractWebAuthnRealmConfigBuilder<PasswordLessRealmConfigBuilder> {
    public PasswordLessRealmConfigBuilder(RealmRepresentation rep) {
        super(rep);
    }

    @Override
    public PasswordLessRealmConfigBuilder setWebAuthnPolicyRpEntityName(String webAuthnPolicyRpEntityName) {
        rep.setWebAuthnPolicyPasswordlessRpEntityName(webAuthnPolicyRpEntityName);
        return this;
    }

    @Override
    public PasswordLessRealmConfigBuilder setWebAuthnPolicyCreateTimeout(Integer webAuthnPolicyCreateTimeout) {
        rep.setWebAuthnPolicyPasswordlessCreateTimeout(webAuthnPolicyCreateTimeout);
        return this;
    }

    @Override
    public PasswordLessRealmConfigBuilder setWebAuthnPolicyAvoidSameAuthenticatorRegister(Boolean webAuthnPolicyAvoidSameAuthenticatorRegister) {
        rep.setWebAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister(webAuthnPolicyAvoidSameAuthenticatorRegister);
        return this;
    }

    @Override
    public PasswordLessRealmConfigBuilder setWebAuthnPolicySignatureAlgorithms(List<String> webAuthnPolicySignatureAlgorithms) {
        rep.setWebAuthnPolicyPasswordlessSignatureAlgorithms(webAuthnPolicySignatureAlgorithms);
        return this;
    }

    @Override
    public PasswordLessRealmConfigBuilder setWebAuthnPolicyAttestationConveyancePreference(String webAuthnPolicyAttestationConveyancePreference) {
        rep.setWebAuthnPolicyPasswordlessAttestationConveyancePreference(webAuthnPolicyAttestationConveyancePreference);
        return this;
    }

    @Override
    public PasswordLessRealmConfigBuilder setWebAuthnPolicyAuthenticatorAttachment(String webAuthnPolicyAuthenticatorAttachment) {
        rep.setWebAuthnPolicyPasswordlessAuthenticatorAttachment(webAuthnPolicyAuthenticatorAttachment);
        return this;
    }

    @Override
    public PasswordLessRealmConfigBuilder setWebAuthnPolicyRequireResidentKey(String webAuthnPolicyRequireResidentKey) {
        rep.setWebAuthnPolicyPasswordlessRequireResidentKey(webAuthnPolicyRequireResidentKey);
        return this;
    }

    @Override
    public PasswordLessRealmConfigBuilder setWebAuthnPolicyRpId(String webAuthnPolicyRpId) {
        rep.setWebAuthnPolicyPasswordlessRpId(webAuthnPolicyRpId);
        return this;
    }

    @Override
    public PasswordLessRealmConfigBuilder setWebAuthnPolicyUserVerificationRequirement(String webAuthnPolicyUserVerificationRequirement) {
        rep.setWebAuthnPolicyPasswordlessUserVerificationRequirement(webAuthnPolicyUserVerificationRequirement);
        return this;
    }

    @Override
    public PasswordLessRealmConfigBuilder setWebAuthnPolicyAcceptableAaguids(List<String> webAuthnPolicyAcceptableAaguids) {
        rep.setWebAuthnPolicyPasswordlessAcceptableAaguids(webAuthnPolicyAcceptableAaguids);
        return this;
    }

    @Override
    public PasswordLessRealmConfigBuilder setWebAuthnPolicyPasskeysEnabled(Boolean webAuthnPolicyPasskeysEnabled) {
        rep.setWebAuthnPolicyPasswordlessPasskeysEnabled(webAuthnPolicyPasskeysEnabled);
        return this;
    }
}
