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
public class WebAuthnRealmAttributeUpdater extends AbstractWebAuthnRealmUpdater<WebAuthnRealmAttributeUpdater> {
    public WebAuthnRealmAttributeUpdater(RealmResource resource) {
        super(resource);
    }

    @Override
    public WebAuthnRealmAttributeUpdater setWebAuthnPolicyRpEntityName(String webAuthnPolicyRpEntityName) {
        rep.setWebAuthnPolicyRpEntityName(webAuthnPolicyRpEntityName);
        return this;
    }

    @Override
    public WebAuthnRealmAttributeUpdater setWebAuthnPolicyCreateTimeout(Integer webAuthnPolicyCreateTimeout) {
        rep.setWebAuthnPolicyCreateTimeout(webAuthnPolicyCreateTimeout);
        return this;
    }

    @Override
    public WebAuthnRealmAttributeUpdater setWebAuthnPolicyAvoidSameAuthenticatorRegister(Boolean webAuthnPolicyAvoidSameAuthenticatorRegister) {
        rep.setWebAuthnPolicyAvoidSameAuthenticatorRegister(webAuthnPolicyAvoidSameAuthenticatorRegister);
        return this;
    }

    @Override
    public WebAuthnRealmAttributeUpdater setWebAuthnPolicySignatureAlgorithms(List<String> webAuthnPolicySignatureAlgorithms) {
        rep.setWebAuthnPolicySignatureAlgorithms(webAuthnPolicySignatureAlgorithms);
        return this;
    }

    @Override
    public WebAuthnRealmAttributeUpdater setWebAuthnPolicyAttestationConveyancePreference(String webAuthnPolicyAttestationConveyancePreference) {
        rep.setWebAuthnPolicyAttestationConveyancePreference(webAuthnPolicyAttestationConveyancePreference);
        return this;
    }

    @Override
    public WebAuthnRealmAttributeUpdater setWebAuthnPolicyAuthenticatorAttachment(String webAuthnPolicyAuthenticatorAttachment) {
        rep.setWebAuthnPolicyAuthenticatorAttachment(webAuthnPolicyAuthenticatorAttachment);
        return this;
    }

    @Override
    public WebAuthnRealmAttributeUpdater setWebAuthnPolicyRequireResidentKey(String webAuthnPolicyRequireResidentKey) {
        rep.setWebAuthnPolicyRequireResidentKey(webAuthnPolicyRequireResidentKey);
        return this;
    }

    @Override
    public WebAuthnRealmAttributeUpdater setWebAuthnPolicyRpId(String webAuthnPolicyRpId) {
        rep.setWebAuthnPolicyRpId(webAuthnPolicyRpId);
        return this;
    }

    @Override
    public WebAuthnRealmAttributeUpdater setWebAuthnPolicyUserVerificationRequirement(String webAuthnPolicyUserVerificationRequirement) {
        rep.setWebAuthnPolicyUserVerificationRequirement(webAuthnPolicyUserVerificationRequirement);
        return this;
    }

    @Override
    public WebAuthnRealmAttributeUpdater setWebAuthnPolicyAcceptableAaguids(List<String> webAuthnPolicyAcceptableAaguids) {
        rep.setWebAuthnPolicyAcceptableAaguids(webAuthnPolicyAcceptableAaguids);
        return this;
    }

    @Override
    public WebAuthnRealmAttributeUpdater setWebAuthnPolicyPasskeysEnabled(Boolean webAuthnPolicyPasskeysEnabled) {
        // passkeys are only used in passwordless policy
        return this;
    }

}
