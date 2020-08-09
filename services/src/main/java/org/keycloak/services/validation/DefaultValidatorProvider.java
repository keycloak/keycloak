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
package org.keycloak.services.validation;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.validation.NamedValidation;
import org.keycloak.validation.NestedValidationContext;
import org.keycloak.validation.Validation;
import org.keycloak.validation.ValidationContext;
import org.keycloak.validation.ValidationKey;
import org.keycloak.validation.ValidationProblem;
import org.keycloak.validation.ValidationRegistry;
import org.keycloak.validation.ValidationResult;
import org.keycloak.validation.ValidatorProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultValidatorProvider implements ValidatorProvider {

    private static final Logger LOGGER = Logger.getLogger(DefaultValidatorProvider.class);

    private final KeycloakSession session;

    private final ValidationRegistry validationRegistry;

    public DefaultValidatorProvider(KeycloakSession session, ValidationRegistry validationRegistry) {
        this.session = session;
        this.validationRegistry = validationRegistry;
    }

    @Override
    public ValidationResult validate(ValidationContext context, Object value, Set<ValidationKey> keys) {

        Map<ValidationKey, List<NamedValidation>> validators = validationRegistry.resolveValidations(context, keys, value);

        if (validators == null || validators.isEmpty()) {
            return null;
        }

        NestedValidationContext nestedContext = new NestedValidationContext(context, session, validationRegistry);
        boolean valid = validateInternal(nestedContext, value, validators);
        List<ValidationProblem> problems = nestedContext.getProblems();

        if (valid && problems.isEmpty()) {
            return ValidationResult.OK;
        }

        return new ValidationResult(valid, problems);
    }

    protected boolean validateInternal(NestedValidationContext context, Object value, Map<ValidationKey, List<NamedValidation>> validators) {

        boolean valid = true;

        for (Map.Entry<ValidationKey, List<NamedValidation>> entry : validators.entrySet()) {
            for (NamedValidation validation : entry.getValue()) {

                ValidationKey key = entry.getKey();

                try {
                    valid &= validation.validate(key, value, context);
                } catch (Exception ex) {
                    LOGGER.warnf("Exception during validation %s for key %s", validation.getName(), key.getName(), ex);
                    context.addProblem(ValidationProblem.error(key, Validation.VALIDATION_ERROR, ex));
                    return false;
                }

                if (!valid && !context.isBulkMode()) {
                    return false;
                }
            }
        }

        return valid;
    }
}
