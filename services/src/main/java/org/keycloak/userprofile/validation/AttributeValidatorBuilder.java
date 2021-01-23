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

import org.keycloak.userprofile.UserProfileContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * @author <a href="mailto:markus.till@bosch.io">Markus Till</a>
 */
public class AttributeValidatorBuilder {
    ValidationChainBuilder validationChainBuilder;
    String attributeKey;
    List<Validator> validations = new ArrayList<>();

    public AttributeValidatorBuilder(ValidationChainBuilder validationChainBuilder) {
        this.validationChainBuilder = validationChainBuilder;
    }

    /**
     * This method is for validating first value of the specified attribute. It is sufficient for all the single-valued attributes
     *
     * @param messageKey Key of the error message to be displayed when validation fails
     * @param validationFunction Function, which does the actual validation logic. The "String" argument is the new value of the particular attribute.
     * @return this
     */
    public AttributeValidatorBuilder addSingleAttributeValueValidationFunction(String messageKey, BiFunction<String, UserProfileContext, Boolean> validationFunction) {
        BiFunction<List<String>, UserProfileContext, Boolean> wrappedValidationFunction = (attrValues, context) -> {
            String singleValue = attrValues == null ? null : attrValues.get(0);
            return validationFunction.apply(singleValue, context);
        };
        this.validations.add(new Validator(messageKey, wrappedValidationFunction));
        return this;
    }

    public AttributeValidatorBuilder addValidationFunction(String messageKey, BiFunction<List<String>, UserProfileContext, Boolean> validationFunction) {
        this.validations.add(new Validator(messageKey, validationFunction));
        return this;
    }

    public AttributeValidatorBuilder forAttribute(String attributeKey) {
        this.attributeKey = attributeKey;
        return this;
    }

    public ValidationChainBuilder build() {
        this.validationChainBuilder.addValidatorConfig(new AttributeValidator(attributeKey, this.validations));
        return this.validationChainBuilder;
    }

}
