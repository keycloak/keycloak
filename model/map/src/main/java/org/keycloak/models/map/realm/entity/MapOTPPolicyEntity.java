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

import org.keycloak.models.OTPPolicy;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;

@GenerateEntityImplementations
@DeepCloner.Root
public interface MapOTPPolicyEntity extends UpdatableEntity {
    static MapOTPPolicyEntity fromModel(OTPPolicy model) {
        if (model == null) return null;
        MapOTPPolicyEntity entity = new MapOTPPolicyEntityImpl();
        entity.setOtpPolicyAlgorithm(model.getAlgorithm());
        entity.setOtpPolicyDigits(model.getDigits());
        entity.setOtpPolicyInitialCounter(model.getInitialCounter());
        entity.setOtpPolicyLookAheadWindow(model.getLookAheadWindow());
        entity.setOtpPolicyType(model.getType());
        entity.setOtpPolicyPeriod(model.getPeriod());
        return entity;
    }

    static OTPPolicy toModel(MapOTPPolicyEntity entity) {
        if (entity == null) return null;
        OTPPolicy model = new OTPPolicy();
        Integer otpPolicyDigits = entity.getOtpPolicyDigits();
        model.setDigits(otpPolicyDigits == null ? 0 : otpPolicyDigits);
        model.setAlgorithm(entity.getOtpPolicyAlgorithm());
        Integer otpPolicyInitialCounter = entity.getOtpPolicyInitialCounter();
        model.setInitialCounter(otpPolicyInitialCounter == null ? 0 : otpPolicyInitialCounter);
        Integer otpPolicyLookAheadWindow = entity.getOtpPolicyLookAheadWindow();
        model.setLookAheadWindow(otpPolicyLookAheadWindow == null ? 0 : otpPolicyLookAheadWindow);
        model.setType(entity.getOtpPolicyType());
        Integer otpPolicyPeriod = entity.getOtpPolicyPeriod();
        model.setPeriod(otpPolicyPeriod == null ? 0 : otpPolicyPeriod);
        return model;
    }

    Integer getOtpPolicyInitialCounter();
    void setOtpPolicyInitialCounter(Integer otpPolicyInitialCounter);

    Integer getOtpPolicyDigits();
    void setOtpPolicyDigits(Integer otpPolicyDigits);

    Integer getOtpPolicyLookAheadWindow();
    void setOtpPolicyLookAheadWindow(Integer otpPolicyLookAheadWindow);

    Integer getOtpPolicyPeriod();
    void setOtpPolicyPeriod(Integer otpPolicyPeriod);

    String getOtpPolicyType();
    void setOtpPolicyType(String otpPolicyType);

    String getOtpPolicyAlgorithm();
    void setOtpPolicyAlgorithm(String otpPolicyAlgorithm);
}
