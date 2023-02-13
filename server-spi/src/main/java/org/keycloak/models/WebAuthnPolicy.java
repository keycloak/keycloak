/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.crypto.Algorithm;

public class WebAuthnPolicy implements Serializable {

    protected static final Logger logger = Logger.getLogger(WebAuthnPolicy.class);
    // required
    protected String rpEntityName;
    protected List<String> signatureAlgorithms;
    // optional
    protected String rpId;
    protected String attestationConveyancePreference;
    protected String authenticatorAttachment;
    protected String requireResidentKey;
    protected String userVerificationRequirement;
    protected int createTimeout = 0; // not specified as option
    protected boolean avoidSameAuthenticatorRegister = false;
    protected List<String> acceptableAaguids;

    public WebAuthnPolicy() {
    }

    public WebAuthnPolicy(List<String> signatureAlgorithms) {
        this.signatureAlgorithms = signatureAlgorithms;
    }

    // TODO : must be thread safe list
    public static WebAuthnPolicy DEFAULT_POLICY = new WebAuthnPolicy(new ArrayList<>(Arrays.asList(Algorithm.ES256)));

    public String getRpEntityName() {
        return rpEntityName;
    }

    public void setRpEntityName(String rpEntityName) {
        this.rpEntityName = rpEntityName;
    }

    public List<String> getSignatureAlgorithm() {
        return signatureAlgorithms;
    }

    public void setSignatureAlgorithm(List<String> signatureAlgorithms) {
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

    public int getCreateTimeout() {
        return createTimeout;
    }

    public void setCreateTimeout(int createTimeout) {
        this.createTimeout = createTimeout;
    }

    public boolean isAvoidSameAuthenticatorRegister() {
        return avoidSameAuthenticatorRegister;
    }

    public void setAvoidSameAuthenticatorRegister(boolean avoidSameAuthenticatorRegister) {
        this.avoidSameAuthenticatorRegister = avoidSameAuthenticatorRegister;
    }

    public List<String> getAcceptableAaguids() {
        return acceptableAaguids;
    }

    public void setAcceptableAaguids(List<String> acceptableAaguids) {
        this.acceptableAaguids = acceptableAaguids;
    }
}
