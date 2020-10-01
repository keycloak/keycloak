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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:markus.till@bosch.io">Markus Till</a>
 */
public class UserProfileValidationResult {


    List<AttributeValidationResult> attributeValidationResults;

    public UserProfileValidationResult(List<AttributeValidationResult> attributeValidationResults) {
        this.attributeValidationResults = attributeValidationResults;
    }

    public List<AttributeValidationResult> getValidationResults() {
        return attributeValidationResults;
    }

    public List<AttributeValidationResult> getErrors() {
        return attributeValidationResults.stream().filter(AttributeValidationResult::isInvalid).collect(Collectors.toCollection(ArrayList::new));
    }


    public boolean hasFailureOfErrorType(String... errorKeys) {
        return this.attributeValidationResults != null
                && this.attributeValidationResults.stream().anyMatch(attributeValidationResult -> attributeValidationResult.hasFailureOfErrorType(errorKeys));
    }

    public boolean hasAttributeChanged(String attribute) {
        return this.attributeValidationResults.stream().filter(o -> o.getField().equals(attribute)).collect(Collectors.toList()).get(0).hasChanged();
    }
}
