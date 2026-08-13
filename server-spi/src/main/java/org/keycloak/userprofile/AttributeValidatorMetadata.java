/*
 *
 *  * Copyright 2021  Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.userprofile;

import java.util.Map;
import java.util.Objects;

import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.Validator;
import org.keycloak.validate.ValidatorConfig;
import org.keycloak.validate.Validators;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 * @author Vlastimil Elias <velias@redhat.com>
 */
public final class AttributeValidatorMetadata {

    private final String validatorId;
    private final ValidatorConfig validatorConfig;

    public AttributeValidatorMetadata(String validatorId) {
        this.validatorId = validatorId;
        this.validatorConfig = ValidatorConfig.configFromMap(null);
    }

    public AttributeValidatorMetadata(String validatorId, ValidatorConfig validatorConfig) {
        this.validatorId = validatorId;
        this.validatorConfig = validatorConfig;
    }

    /**
     * Getters so we can collect validation configurations and provide them to GUI for dynamic client side validations.
     * 
     * @return the validatorId
     */
    public String getValidatorId() {
        return validatorId;
    }
    
    /**
     * Get validator configuration as map.
     * 
     * @return never null
     */
    public Map<String, Object> getValidatorConfig(){
        return validatorConfig.asMap();
    }
    
    /**
     * Run validation for given AttributeContext.
     * 
     * @param context to validate
     * @return context containing errors if any found
     */
    public ValidationContext validate(AttributeContext context) {

        Validator validator = Validators.validator(context.getSession(), validatorId);
        if (validator == null) {
            throw new RuntimeException("No validator with id " + validatorId + " found to validate UserProfile attribute " + context.getMetadata().getName() + " in realm " + context.getSession().getContext().getRealm().getName());
        }

        return validator.validate(context.getAttribute().getValue(), context.getMetadata().getName(), new UserProfileAttributeValidationContext(context), validatorConfig);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (! (o instanceof AttributeValidatorMetadata)) return false;
        AttributeValidatorMetadata other = (AttributeValidatorMetadata) o;
        return Objects.equals(getValidatorId(), other.getValidatorId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(validatorId);
    }
}
