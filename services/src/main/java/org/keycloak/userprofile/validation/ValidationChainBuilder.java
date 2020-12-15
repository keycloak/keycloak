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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:markus.till@bosch.io">Markus Till</a>
 */
public class ValidationChainBuilder {

    Map<String, AttributeValidator> attributeConfigs = new HashMap<>();

    public static ValidationChainBuilder builder() {
        return new ValidationChainBuilder();
    }

    public AttributeValidatorBuilder addAttributeValidator() {
        return new AttributeValidatorBuilder(this);
    }

    public ValidationChain build() {
        return new ValidationChain(this.attributeConfigs.values().stream().collect(Collectors.toList()));
    }

    public void addValidatorConfig(AttributeValidator validator) {
        if (attributeConfigs.containsKey(validator.attributeKey)) {
            attributeConfigs.get(validator.attributeKey).validators.addAll(validator.validators);
        } else {
            attributeConfigs.put(validator.attributeKey, validator);
        }
    }
}
