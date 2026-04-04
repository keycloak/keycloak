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
 */

package org.keycloak.models;

import java.util.Collections;
import java.util.List;

import org.keycloak.storage.ReadOnlyException;

/**
 * Default values for the WebAuthn configuration when used as Two Factor.
 *
 * @author rmartinc
 */
public class WebAuthnPolicyTwoFactorDefaults extends WebAuthnPolicy {

    public static WebAuthnPolicy get() {
        return new WebAuthnPolicyTwoFactorDefaults();
    }

    WebAuthnPolicyTwoFactorDefaults() {
        this.rpEntityName = Constants.DEFAULT_WEBAUTHN_POLICY_RP_ENTITY_NAME;
        this.signatureAlgorithms = List.of(Constants.DEFAULT_WEBAUTHN_POLICY_SIGNATURE_ALGORITHMS.split(","));
        this.rpId = "";
        this.attestationConveyancePreference = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        this.authenticatorAttachment = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        this.requireResidentKey = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        this.userVerificationRequirement = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        this.createTimeout = 0;
        this.avoidSameAuthenticatorRegister = false;
        this.acceptableAaguids = Collections.emptyList();
        this.extraOrigins = Collections.emptyList();
        this.passkeysEnabled = null;
    }

    @Override
    public void setRpEntityName(String rpEntityName) {
        throwReadOnlyException();
    }

    @Override
    public void setSignatureAlgorithm(List<String> signatureAlgorithms) {
        throwReadOnlyException();
    }

    @Override
    public void setRpId(String rpId) {
        throwReadOnlyException();
    }

    @Override
    public void setAttestationConveyancePreference(String attestationConveyancePreference) {
        throwReadOnlyException();
    }

    @Override
    public void setAuthenticatorAttachment(String authenticatorAttachment) {
        throwReadOnlyException();
    }

    @Override
    public void setRequireResidentKey(String requireResidentKey) {
        throwReadOnlyException();
    }

    @Override
    public void setUserVerificationRequirement(String userVerificationRequirement) {
        throwReadOnlyException();
    }

    @Override
    public void setCreateTimeout(int createTimeout) {
        throwReadOnlyException();
    }

    @Override
    public void setAvoidSameAuthenticatorRegister(boolean avoidSameAuthenticatorRegister) {
        throwReadOnlyException();
    }

    @Override
    public void setAcceptableAaguids(List<String> acceptableAaguids) {
        throwReadOnlyException();
    }

    @Override
    public void setExtraOrigins(List<String> extraOrigins) {
        throwReadOnlyException();
    }

    @Override
    public void setPasskeysEnabled(Boolean passkeysEnabled) {
        throwReadOnlyException();
    }

    private void throwReadOnlyException() {
        throw new ReadOnlyException("Default WebAuthnPolicy!");
    }
}
