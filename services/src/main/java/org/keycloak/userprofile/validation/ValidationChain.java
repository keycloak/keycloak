/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.userprofile.validation;

import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author <a href="mailto:markus.till@bosch.io">Markus Till</a>
 */
public class ValidationChain {
    List<AttributeValidator> attributeValidators;

    public ValidationChain(List<AttributeValidator> attributeValidators) {
        this.attributeValidators = attributeValidators;
    }

    public List<AttributeValidationResult> validate(UserProfileContext updateContext, UserProfile updatedProfile) {
        List<AttributeValidationResult> overallResults = new ArrayList<>();
        for (AttributeValidator attribute : attributeValidators) {
            List<ValidationResult> validationResults = new ArrayList<>();

            String attributeKey = attribute.attributeKey;
            List<String> attributeValues = updatedProfile.getAttributes().getAttribute(attributeKey);

            List<String> existingAttrValues = updateContext.getCurrentProfile() == null ? null : updateContext.getCurrentProfile().getAttributes().getAttribute(attributeKey);
            boolean attributeChanged = !Objects.equals(attributeValues, existingAttrValues);
            for (Validator validator : attribute.validators) {
                validationResults.add(new ValidationResult(validator.function.apply(attributeValues, updateContext), validator.errorType));
            }

            overallResults.add(new AttributeValidationResult(attributeKey, attributeChanged, validationResults));
        }

        return overallResults;
    }

}
