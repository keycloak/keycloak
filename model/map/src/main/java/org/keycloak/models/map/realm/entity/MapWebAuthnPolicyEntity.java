/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.models.Constants;
import org.keycloak.models.WebAuthnPolicy;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@GenerateEntityImplementations
@DeepCloner.Root
public interface MapWebAuthnPolicyEntity extends UpdatableEntity {
    static MapWebAuthnPolicyEntity fromModel(WebAuthnPolicy model) {
        if (model == null) return null;
        MapWebAuthnPolicyEntity entity = new MapWebAuthnPolicyEntityImpl();
        entity.setRpEntityName(model.getRpEntityName());
        entity.setSignatureAlgorithms(model.getSignatureAlgorithm());
        entity.setRpId(model.getRpId());
        entity.setAttestationConveyancePreference(model.getAttestationConveyancePreference());
        entity.setAuthenticatorAttachment(model.getAuthenticatorAttachment());
        entity.setRequireResidentKey(model.getRequireResidentKey());
        entity.setUserVerificationRequirement(model.getUserVerificationRequirement());
        entity.setCreateTimeout(model.getCreateTimeout());
        entity.setAvoidSameAuthenticatorRegister(model.isAvoidSameAuthenticatorRegister());
        entity.setAcceptableAaguids(model.getAcceptableAaguids());
        return entity;
    }

    static WebAuthnPolicy toModel(MapWebAuthnPolicyEntity entity) {
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
        List<String> acceptableAaguids = entity.getAcceptableAaguids();
        model.setAcceptableAaguids(acceptableAaguids == null ? new LinkedList<>() : new LinkedList<>(acceptableAaguids));
        return model;
    }

    static MapWebAuthnPolicyEntity defaultWebAuthnPolicy() {
        MapWebAuthnPolicyEntity entity = new MapWebAuthnPolicyEntityImpl();
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

    String getRpEntityName();
    void setRpEntityName(String rpEntityName);

    List<String> getSignatureAlgorithms();
    void setSignatureAlgorithms(List<String> signatureAlgorithms);

    String getRpId();
    void setRpId(String rpId);

    String getAttestationConveyancePreference();
    void setAttestationConveyancePreference(String attestationConveyancePreference);

    String getAuthenticatorAttachment();
    void setAuthenticatorAttachment(String authenticatorAttachment);

    String getRequireResidentKey();
    void setRequireResidentKey(String requireResidentKey);

    String getUserVerificationRequirement();
    void setUserVerificationRequirement(String userVerificationRequirement);

    Integer getCreateTimeout();
    void setCreateTimeout(Integer createTimeout);

    Boolean isAvoidSameAuthenticatorRegister();
    void setAvoidSameAuthenticatorRegister(Boolean avoidSameAuthenticatorRegister);

    List<String> getAcceptableAaguids();
    void setAcceptableAaguids(List<String> acceptableAaguids);
}
