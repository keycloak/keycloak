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

package org.keycloak.testsuite.webauthn.utils;

import org.keycloak.representations.idm.RealmRepresentation;

import java.util.List;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Helper class for retrieving WebAuthn data
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class WebAuthnRealmData {

    private final RealmRepresentation realm;
    private final boolean isPasswordless;

    public WebAuthnRealmData(RealmRepresentation realm, boolean isPasswordless) {
        assertThat("RealmRepresentation must not be NULL", realm, notNullValue());
        this.realm = realm;
        this.isPasswordless = isPasswordless;
    }

    public String getRpEntityName() {
        return isPasswordless ? realm.getWebAuthnPolicyPasswordlessRpEntityName() : realm.getWebAuthnPolicyRpEntityName();
    }

    public List<String> getSignatureAlgorithms() {
        return isPasswordless ? realm.getWebAuthnPolicyPasswordlessSignatureAlgorithms() : realm.getWebAuthnPolicySignatureAlgorithms();
    }

    public String getRpId() {
        return isPasswordless ? realm.getWebAuthnPolicyPasswordlessRpId() : realm.getWebAuthnPolicyRpId();
    }

    public String getAttestationConveyancePreference() {
        return isPasswordless ? realm.getWebAuthnPolicyPasswordlessAttestationConveyancePreference() : realm.getWebAuthnPolicyAttestationConveyancePreference();
    }

    public String getAuthenticatorAttachment() {
        return isPasswordless ? realm.getWebAuthnPolicyPasswordlessAuthenticatorAttachment() : realm.getWebAuthnPolicyAuthenticatorAttachment();
    }

    public String getRequireResidentKey() {
        return isPasswordless ? realm.getWebAuthnPolicyPasswordlessRequireResidentKey() : realm.getWebAuthnPolicyRequireResidentKey();
    }

    public String getUserVerificationRequirement() {
        return isPasswordless ? realm.getWebAuthnPolicyPasswordlessUserVerificationRequirement() : realm.getWebAuthnPolicyUserVerificationRequirement();
    }

    public Integer getCreateTimeout() {
        return isPasswordless ? realm.getWebAuthnPolicyPasswordlessCreateTimeout() : realm.getWebAuthnPolicyCreateTimeout();
    }

    public Boolean isAvoidSameAuthenticatorRegister() {
        return isPasswordless ? realm.isWebAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister() : realm.isWebAuthnPolicyAvoidSameAuthenticatorRegister();
    }

    public List<String> getAcceptableAaguids() {
        return isPasswordless ? realm.getWebAuthnPolicyPasswordlessAcceptableAaguids() : realm.getWebAuthnPolicyAcceptableAaguids();
    }

    public RealmRepresentation getRealm() {
        return realm;
    }

    public Builder builder() {
        return new Builder(realm, isPasswordless);
    }

    public static class Builder {
        private final RealmRepresentation realm;
        private final boolean isPasswordless;

        public Builder(RealmRepresentation realm, boolean isPasswordless) {
            this.realm = realm;
            this.isPasswordless = isPasswordless;
        }

        public Builder rpEntityName(String entityName) {
            setProperty(entityName, realm::setWebAuthnPolicyRpEntityName, realm::setWebAuthnPolicyPasswordlessRpEntityName);
            return this;
        }

        public Builder signatureAlgorithms(List<String> list) {
            setProperty(list, realm::setWebAuthnPolicySignatureAlgorithms, realm::setWebAuthnPolicyPasswordlessSignatureAlgorithms);
            return this;
        }

        public Builder rpId(String rpId) {
            setProperty(rpId, realm::setWebAuthnPolicyRpId, realm::setWebAuthnPolicyPasswordlessRpId);
            return this;
        }

        public Builder attestationConveyancePreference(String preference) {
            setProperty(preference, realm::setWebAuthnPolicyAttestationConveyancePreference, realm::setWebAuthnPolicyPasswordlessAttestationConveyancePreference);
            return this;
        }

        public Builder authenticatorAttachment(String attachment) {
            setProperty(attachment, realm::setWebAuthnPolicyAuthenticatorAttachment, realm::setWebAuthnPolicyPasswordlessAuthenticatorAttachment);
            return this;
        }

        public Builder requireResidentKey(String requirement) {
            setProperty(requirement, realm::setWebAuthnPolicyRequireResidentKey, realm::setWebAuthnPolicyPasswordlessRequireResidentKey);
            return this;
        }

        public Builder userVerificationRequirement(String requirement) {
            setProperty(requirement, realm::setWebAuthnPolicyUserVerificationRequirement, realm::setWebAuthnPolicyPasswordlessUserVerificationRequirement);
            return this;
        }

        public Builder timeout(Integer timeout) {
            setProperty(timeout, realm::setWebAuthnPolicyCreateTimeout, realm::setWebAuthnPolicyPasswordlessCreateTimeout);
            return this;
        }

        public Builder avoidSameAuthenticatorRegister(Boolean state) {
            setProperty(state, realm::setWebAuthnPolicyAvoidSameAuthenticatorRegister, realm::setWebAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister);
            return this;
        }

        public Builder acceptableAaguids(List<String> aaguids) {
            setProperty(aaguids, realm::setWebAuthnPolicyAcceptableAaguids, realm::setWebAuthnPolicyPasswordlessAcceptableAaguids);
            return this;
        }

        public RealmRepresentation build() {
            return realm;
        }

        private <T> void setProperty(T value, Consumer<T> webauthnSetter, Consumer<T> passwordlessSetter) {
            if (isPasswordless) {
                passwordlessSetter.accept(value);
            } else {
                webauthnSetter.accept(value);
            }
        }
    }
}
