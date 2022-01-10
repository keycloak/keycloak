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

package org.keycloak.models.map.realm.entity;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.keycloak.models.Constants;
import org.keycloak.models.WebAuthnPolicy;
import org.keycloak.models.map.common.UpdatableEntity;

public class MapWebAuthnPolicyEntity extends UpdatableEntity.Impl {

    // mandatory
    private String rpEntityName;
    private List<String> signatureAlgorithms = new LinkedList<>();

    // optional
    private String rpId;
    private String attestationConveyancePreference;
    private String authenticatorAttachment;
    private String requireResidentKey;
    private String userVerificationRequirement;
    private Integer createTimeout = 0;
    private Boolean avoidSameAuthenticatorRegister = false;
    private List<String> acceptableAaguids = new LinkedList<>();


    private MapWebAuthnPolicyEntity() {}

    public static MapWebAuthnPolicyEntity fromModel(WebAuthnPolicy model) {
        if (model == null) return null;
        MapWebAuthnPolicyEntity entity = new MapWebAuthnPolicyEntity();
        entity.setRpEntityName(model.getRpEntityName());
        entity.setSignatureAlgorithms(model.getSignatureAlgorithm());
        entity.setRpId(model.getRpId());
        entity.setAttestationConveyancePreference(model.getAttestationConveyancePreference());
        entity.setAuthenticatorAttachment(model.getAuthenticatorAttachment());
        entity.setRequireResidentKey(model.getRequireResidentKey());
        entity.setUserVerificationRequirement(model.getUserVerificationRequirement());
        entity.setCreateTimeout(model.getCreateTimeout());
        entity.setAvoidSameAuthenticatorRegister(model.isAvoidSameAuthenticatorRegister());
        entity.setAcceptableAaguids(model.getAcceptableAaguids() == null ? null : new LinkedList<>(model.getAcceptableAaguids()));
        return entity;
    }

    public static WebAuthnPolicy toModel(MapWebAuthnPolicyEntity entity) {
        if (entity == null) return null;
        WebAuthnPolicy model = new WebAuthnPolicy();
        model.setRpEntityName(entity.getRpEntityName());
        model.setSignatureAlgorithm(entity.getSignatureAlgorithms());
        model.setRpId(entity.getRpId());
        model.setAttestationConveyancePreference(entity.getAttestationConveyancePreference());
        model.setAuthenticatorAttachment(entity.getAuthenticatorAttachment());
        model.setRequireResidentKey(entity.getRequireResidentKey());
        model.setUserVerificationRequirement(entity.getUserVerificationRequirement());
        model.setCreateTimeout(entity.getCreateTimeout());
        model.setAvoidSameAuthenticatorRegister(entity.isAvoidSameAuthenticatorRegister());
        model.setAcceptableAaguids(entity.getAcceptableAaguids() == null ? null : new LinkedList<>(entity.getAcceptableAaguids()));
        return model;
    }

    public static MapWebAuthnPolicyEntity defaultWebAuthnPolicy() {
        MapWebAuthnPolicyEntity entity = new MapWebAuthnPolicyEntity();
        entity.setRpEntityName(Constants.DEFAULT_WEBAUTHN_POLICY_RP_ENTITY_NAME);
        entity.setSignatureAlgorithms(Arrays.asList(Constants.DEFAULT_WEBAUTHN_POLICY_SIGNATURE_ALGORITHMS.split(",")));
        entity.setRpId("");
        entity.setAttestationConveyancePreference(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED);
        entity.setAuthenticatorAttachment(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED);
        entity.setRequireResidentKey(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED);
        entity.setUserVerificationRequirement(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED);
        entity.setCreateTimeout(0);
        entity.setAvoidSameAuthenticatorRegister(false);
        entity.setAcceptableAaguids(new LinkedList<>());
        return entity;
    }

    public String getRpEntityName() {
        return rpEntityName;
    }

    public void setRpEntityName(String rpEntityName) {
        this.updated = !Objects.equals(this.rpEntityName, rpEntityName);
        this.rpEntityName = rpEntityName;
    }

    public List<String> getSignatureAlgorithms() {
        return signatureAlgorithms;
    }

    public void setSignatureAlgorithms(List<String> signatureAlgorithms) {
        this.updated = !Objects.equals(this.signatureAlgorithms, signatureAlgorithms);
        this.signatureAlgorithms = signatureAlgorithms;
    }

    public String getRpId() {
        return rpId;
    }

    public void setRpId(String rpId) {
        this.updated = !Objects.equals(this.rpId, rpId);
        this.rpId = rpId;
    }

    public String getAttestationConveyancePreference() {
        return attestationConveyancePreference;
    }

    public void setAttestationConveyancePreference(String attestationConveyancePreference) {
        this.updated = !Objects.equals(this.attestationConveyancePreference, attestationConveyancePreference);
        this.attestationConveyancePreference = attestationConveyancePreference;
    }

    public String getAuthenticatorAttachment() {
        return authenticatorAttachment;
    }

    public void setAuthenticatorAttachment(String authenticatorAttachment) {
        this.updated = !Objects.equals(this.authenticatorAttachment, authenticatorAttachment);
        this.authenticatorAttachment = authenticatorAttachment;
    }

    public String getRequireResidentKey() {
        return requireResidentKey;
    }

    public void setRequireResidentKey(String requireResidentKey) {
        this.updated = !Objects.equals(this.requireResidentKey, requireResidentKey);
        this.requireResidentKey = requireResidentKey;
    }

    public String getUserVerificationRequirement() {
        return userVerificationRequirement;
    }

    public void setUserVerificationRequirement(String userVerificationRequirement) {
        this.updated = !Objects.equals(this.userVerificationRequirement, userVerificationRequirement);
        this.userVerificationRequirement = userVerificationRequirement;
    }

    public Integer getCreateTimeout() {
        return createTimeout;
    }

    public void setCreateTimeout(int createTimeout) {
        this.updated = !Objects.equals(this.createTimeout, createTimeout);
        this.createTimeout = createTimeout;
    }

    public Boolean isAvoidSameAuthenticatorRegister() {
        return avoidSameAuthenticatorRegister;
    }

    public void setAvoidSameAuthenticatorRegister(boolean avoidSameAuthenticatorRegister) {
        this.updated = !Objects.equals(this.avoidSameAuthenticatorRegister, avoidSameAuthenticatorRegister);
        this.avoidSameAuthenticatorRegister = avoidSameAuthenticatorRegister;
    }

    public List<String> getAcceptableAaguids() {
        return acceptableAaguids;
    }

    public void setAcceptableAaguids(List<String> acceptableAaguids) {
        this.updated = !Objects.equals(this.acceptableAaguids, acceptableAaguids);
        this.acceptableAaguids = acceptableAaguids;
    }

    @Override
    public int hashCode() {
        return getRpEntityName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MapWebAuthnPolicyEntity)) return false;
        final MapWebAuthnPolicyEntity other = (MapWebAuthnPolicyEntity) obj;
        return Objects.equals(other.getRpEntityName(), getRpEntityName());
    }
}
