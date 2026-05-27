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

package org.keycloak.representations.idm;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.crypto.Algorithm;

public class WebAuthnPolicyRepresentation {
    private String rpEntityName;
    private List<String> signatureAlgorithms;
    private String rpId;
    private String attestationConveyancePreference;
    private String authenticatorAttachment;
    private String requireResidentKey;
    private String userVerificationRequirement;
    private Integer createTimeout;
    private Boolean avoidSameAuthenticatorRegister;
    private List<String> acceptableAaguids;
    private List<String> extraOrigins;
    private Boolean passkeysEnabled;
    private String mediation;

    final private static Set<String> supportedSignatureAlgorithms = new LinkedHashSet<>(Arrays.asList(
            Algorithm.ES256,
            Algorithm.ES384,
            Algorithm.ES512,
            Algorithm.RS256,
            Algorithm.RS384,
            Algorithm.ES512,
            Algorithm.Ed25519,
            "RS1"
    ));

    public String getRpEntityName() {
        return rpEntityName;
    }

    public void setRpEntityName(String rpEntityName) {
        this.rpEntityName = rpEntityName;
    }

    public List<String> getSignatureAlgorithms() {
        return signatureAlgorithms == null ? null : signatureAlgorithms.stream().filter(supportedSignatureAlgorithms::contains).collect(Collectors.toList());
    }

    public void setSignatureAlgorithms(List<String> signatureAlgorithms) {
        this.signatureAlgorithms = signatureAlgorithms;
    }

    public String getRpId() {
        return rpId;
    }

    public void setRpId(String rpId) {
        this.rpId = rpId;
    }

    public String getAttestationConveyancePreference() {
        return attestationConveyancePreference;
    }

    public void setAttestationConveyancePreference(String attestationConveyancePreference) {
        this.attestationConveyancePreference = attestationConveyancePreference;
    }

    public String getAuthenticatorAttachment() {
        return authenticatorAttachment;
    }

    public void setAuthenticatorAttachment(String authenticatorAttachment) {
        this.authenticatorAttachment = authenticatorAttachment;
    }

    public String getRequireResidentKey() {
        return requireResidentKey;
    }

    public void setRequireResidentKey(String requireResidentKey) {
        this.requireResidentKey = requireResidentKey;
    }

    public String getUserVerificationRequirement() {
        return userVerificationRequirement;
    }

    public void setUserVerificationRequirement(String userVerificationRequirement) {
        this.userVerificationRequirement = userVerificationRequirement;
    }

    public Integer getCreateTimeout() {
        return createTimeout;
    }

    public void setCreateTimeout(Integer createTimeout) {
        this.createTimeout = createTimeout;
    }

    public Boolean getAvoidSameAuthenticatorRegister() {
        return avoidSameAuthenticatorRegister;
    }

    public void setAvoidSameAuthenticatorRegister(Boolean avoidSameAuthenticatorRegister) {
        this.avoidSameAuthenticatorRegister = avoidSameAuthenticatorRegister;
    }

    public List<String> getAcceptableAaguids() {
        return acceptableAaguids;
    }

    public void setAcceptableAaguids(List<String> acceptableAaguids) {
        this.acceptableAaguids = acceptableAaguids;
    }

    public List<String> getExtraOrigins() {
        return extraOrigins;
    }

    public void setExtraOrigins(List<String> extraOrigins) {
        this.extraOrigins = extraOrigins;
    }

    public Boolean getPasskeysEnabled() {
        return passkeysEnabled;
    }

    public void setPasskeysEnabled(Boolean passkeysEnabled) {
        this.passkeysEnabled = passkeysEnabled;
    }

    public String getMediation() {
        return mediation;
    }

    public void setMediation(String mediation) {
        this.mediation = mediation;
    }
}
