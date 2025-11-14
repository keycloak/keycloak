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

package org.keycloak.testsuite.webauthn.updaters;

import java.util.List;

import org.keycloak.admin.client.resource.RealmResource;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class PasswordLessRealmAttributeUpdater extends AbstractWebAuthnRealmUpdater<PasswordLessRealmAttributeUpdater> {
    public PasswordLessRealmAttributeUpdater(RealmResource resource) {
        super(resource);
    }

    @Override
    public PasswordLessRealmAttributeUpdater setWebAuthnPolicyRpEntityName(String webAuthnPolicyRpEntityName) {
        rep.setWebAuthnPolicyPasswordlessRpEntityName(webAuthnPolicyRpEntityName);
        return this;
    }

    @Override
    public PasswordLessRealmAttributeUpdater setWebAuthnPolicyCreateTimeout(Integer webAuthnPolicyCreateTimeout) {
        rep.setWebAuthnPolicyPasswordlessCreateTimeout(webAuthnPolicyCreateTimeout);
        return this;
    }

    @Override
    public PasswordLessRealmAttributeUpdater setWebAuthnPolicyAvoidSameAuthenticatorRegister(Boolean webAuthnPolicyAvoidSameAuthenticatorRegister) {
        rep.setWebAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister(webAuthnPolicyAvoidSameAuthenticatorRegister);
        return this;
    }

    @Override
    public PasswordLessRealmAttributeUpdater setWebAuthnPolicySignatureAlgorithms(List<String> webAuthnPolicySignatureAlgorithms) {
        rep.setWebAuthnPolicyPasswordlessSignatureAlgorithms(webAuthnPolicySignatureAlgorithms);
        return this;
    }

    @Override
    public PasswordLessRealmAttributeUpdater setWebAuthnPolicyAttestationConveyancePreference(String webAuthnPolicyAttestationConveyancePreference) {
        rep.setWebAuthnPolicyPasswordlessAttestationConveyancePreference(webAuthnPolicyAttestationConveyancePreference);
        return this;
    }

    @Override
    public PasswordLessRealmAttributeUpdater setWebAuthnPolicyAuthenticatorAttachment(String webAuthnPolicyAuthenticatorAttachment) {
        rep.setWebAuthnPolicyPasswordlessAuthenticatorAttachment(webAuthnPolicyAuthenticatorAttachment);
        return this;
    }

    @Override
    public PasswordLessRealmAttributeUpdater setWebAuthnPolicyRequireResidentKey(String webAuthnPolicyRequireResidentKey) {
        rep.setWebAuthnPolicyPasswordlessRequireResidentKey(webAuthnPolicyRequireResidentKey);
        return this;
    }

    @Override
    public PasswordLessRealmAttributeUpdater setWebAuthnPolicyRpId(String webAuthnPolicyRpId) {
        rep.setWebAuthnPolicyPasswordlessRpId(webAuthnPolicyRpId);
        return this;
    }

    @Override
    public PasswordLessRealmAttributeUpdater setWebAuthnPolicyUserVerificationRequirement(String webAuthnPolicyUserVerificationRequirement) {
        rep.setWebAuthnPolicyPasswordlessUserVerificationRequirement(webAuthnPolicyUserVerificationRequirement);
        return this;
    }

    @Override
    public PasswordLessRealmAttributeUpdater setWebAuthnPolicyAcceptableAaguids(List<String> webAuthnPolicyAcceptableAaguids) {
        rep.setWebAuthnPolicyPasswordlessAcceptableAaguids(webAuthnPolicyAcceptableAaguids);
        return this;
    }

    @Override
    public PasswordLessRealmAttributeUpdater setWebAuthnPolicyPasskeysEnabled(Boolean webAuthnPolicyPasskeysEnabled) {
        rep.setWebAuthnPolicyPasswordlessPasskeysEnabled(webAuthnPolicyPasskeysEnabled);
        return this;
    }
}
