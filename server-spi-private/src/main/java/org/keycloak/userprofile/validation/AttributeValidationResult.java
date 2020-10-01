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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:markus.till@bosch.io">Markus Till</a>
 */
public class AttributeValidationResult {

    private final String attributeKey;
    private final boolean changed;
    List<ValidationResult> validationResults;

    public List<ValidationResult> getValidationResults() {
        return validationResults;
    }

    public List<ValidationResult> getFailedValidations() {
        return validationResults == null ? null : validationResults.stream().filter(ValidationResult::isInvalid).collect(Collectors.toList());
    }


    public AttributeValidationResult(String attributeKey, boolean changed, List<ValidationResult> validationResults) {
        this.attributeKey = attributeKey;
        this.validationResults = validationResults;
        this.changed = changed;

    }

    public boolean isValid() {
        return validationResults.stream().allMatch(ValidationResult::isValid);
    }

    protected boolean isInvalid() {
        return !isValid();
    }

    public boolean hasChanged() {
        return changed;
    }

    public String getField() {
        return attributeKey;
    }

    public boolean hasFailureOfErrorType(String... errorKeys) {
        return this.validationResults != null
                && this.getFailedValidations().stream().anyMatch(o -> o.getErrorType() != null
                && Arrays.stream(errorKeys).anyMatch(a -> a.equals(o.getErrorType())));
    }

}
