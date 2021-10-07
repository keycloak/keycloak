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

import java.util.Objects;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.map.common.UpdatableEntity;

public class MapOTPPolicyEntity extends UpdatableEntity.Impl {

    private Integer otpPolicyInitialCounter = 0;
    private Integer otpPolicyDigits = 0;
    private Integer otpPolicyLookAheadWindow = 0;
    private Integer otpPolicyPeriod = 0;
    private String otpPolicyType;
    private String otpPolicyAlgorithm;


    private MapOTPPolicyEntity() {}

    public static MapOTPPolicyEntity fromModel(OTPPolicy model) {
        if (model == null) return null;
        MapOTPPolicyEntity entity = new MapOTPPolicyEntity();
        entity.setOtpPolicyAlgorithm(model.getAlgorithm());
        entity.setOtpPolicyDigits(model.getDigits());
        entity.setOtpPolicyInitialCounter(model.getInitialCounter());
        entity.setOtpPolicyLookAheadWindow(model.getLookAheadWindow());
        entity.setOtpPolicyType(model.getType());
        entity.setOtpPolicyPeriod(model.getPeriod());
        return entity;
    }

    public static OTPPolicy toModel(MapOTPPolicyEntity entity) {
        if (entity == null) return null;
        OTPPolicy model = new OTPPolicy();
        model.setDigits(entity.getOtpPolicyDigits());
        model.setAlgorithm(entity.getOtpPolicyAlgorithm());
        model.setInitialCounter(entity.getOtpPolicyInitialCounter());
        model.setLookAheadWindow(entity.getOtpPolicyLookAheadWindow());
        model.setType(entity.getOtpPolicyType());
        model.setPeriod(entity.getOtpPolicyPeriod());
        return model;
    }

    public Integer getOtpPolicyInitialCounter() {
        return otpPolicyInitialCounter;
    }

    public void setOtpPolicyInitialCounter(int otpPolicyInitialCounter) {
        this.updated = !Objects.equals(this.otpPolicyInitialCounter, otpPolicyInitialCounter);
        this.otpPolicyInitialCounter = otpPolicyInitialCounter;
    }

    public Integer getOtpPolicyDigits() {
        return otpPolicyDigits;
    }

    public void setOtpPolicyDigits(int otpPolicyDigits) {
        this.updated = !Objects.equals(this.otpPolicyDigits, otpPolicyDigits);
        this.otpPolicyDigits = otpPolicyDigits;
    }

    public Integer getOtpPolicyLookAheadWindow() {
        return otpPolicyLookAheadWindow;
    }

    public void setOtpPolicyLookAheadWindow(int otpPolicyLookAheadWindow) {
        this.updated = !Objects.equals(this.otpPolicyLookAheadWindow, otpPolicyLookAheadWindow);
        this.otpPolicyLookAheadWindow = otpPolicyLookAheadWindow;
    }

    public Integer getOtpPolicyPeriod() {
        return otpPolicyPeriod;
    }

    public void setOtpPolicyPeriod(int otpPolicyPeriod) {
        this.updated = !Objects.equals(this.otpPolicyPeriod, otpPolicyPeriod);
        this.otpPolicyPeriod = otpPolicyPeriod;
    }

    public String getOtpPolicyType() {
        return otpPolicyType;
    }

    public void setOtpPolicyType(String otpPolicyType) {
        this.updated = !Objects.equals(this.otpPolicyType, otpPolicyType);
        this.otpPolicyType = otpPolicyType;
    }

    public String getOtpPolicyAlgorithm() {
        return otpPolicyAlgorithm;
    }

    public void setOtpPolicyAlgorithm(String otpPolicyAlgorithm) {
        this.updated = !Objects.equals(this.otpPolicyAlgorithm, otpPolicyAlgorithm);
        this.otpPolicyAlgorithm = otpPolicyAlgorithm;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + this.otpPolicyInitialCounter;
        hash = 59 * hash + this.otpPolicyDigits;
        hash = 59 * hash + this.otpPolicyLookAheadWindow;
        hash = 59 * hash + this.otpPolicyPeriod;
        hash = 59 * hash + Objects.hashCode(this.otpPolicyType);
        hash = 59 * hash + Objects.hashCode(this.otpPolicyAlgorithm);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MapOTPPolicyEntity)) return false;
        final MapOTPPolicyEntity other = (MapOTPPolicyEntity) obj;
        return Objects.equals(other.getOtpPolicyAlgorithm(), getOtpPolicyAlgorithm()) &&
               Objects.equals(other.getOtpPolicyDigits(), getOtpPolicyDigits()) &&
               Objects.equals(other.getOtpPolicyInitialCounter(), getOtpPolicyInitialCounter()) &&
               Objects.equals(other.getOtpPolicyLookAheadWindow(), getOtpPolicyLookAheadWindow()) &&
               Objects.equals(other.getOtpPolicyPeriod(), getOtpPolicyPeriod()) &&
               Objects.equals(other.getOtpPolicyType(), getOtpPolicyType());
    }
    
}
