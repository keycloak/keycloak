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
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public abstract class AbstractWebAuthnRealmUpdater<T extends AbstractWebAuthnRealmUpdater> extends RealmAttributeUpdater {

    public AbstractWebAuthnRealmUpdater(RealmResource resource) {
        super(resource);
    }

    public abstract T setWebAuthnPolicyRpEntityName(String webAuthnPolicyRpEntityName);

    public abstract T setWebAuthnPolicyCreateTimeout(Integer webAuthnPolicyCreateTimeout);

    public abstract T setWebAuthnPolicyAvoidSameAuthenticatorRegister(Boolean webAuthnPolicyAvoidSameAuthenticatorRegister);

    public abstract T setWebAuthnPolicySignatureAlgorithms(List<String> webAuthnPolicySignatureAlgorithms);

    public abstract T setWebAuthnPolicyAttestationConveyancePreference(String webAuthnPolicyAttestationConveyancePreference);

    public abstract T setWebAuthnPolicyAuthenticatorAttachment(String webAuthnPolicyAuthenticatorAttachment);

    public abstract T setWebAuthnPolicyRequireResidentKey(String webAuthnPolicyRequireResidentKey);

    public abstract T setWebAuthnPolicyRpId(String webAuthnPolicyRpId);

    public abstract T setWebAuthnPolicyUserVerificationRequirement(String webAuthnPolicyUserVerificationRequirement);

    public abstract T setWebAuthnPolicyAcceptableAaguids(List<String> webAuthnPolicyAcceptableAaguids);

    public abstract T setWebAuthnPolicyPasskeysEnabled(Boolean webAuthnPolicyPasskeysEnabled);

    @Override
    @SuppressWarnings("unchecked")
    public AbstractWebAuthnRealmUpdater<T> update() {
        return (AbstractWebAuthnRealmUpdater<T>) super.update();
    }
}
