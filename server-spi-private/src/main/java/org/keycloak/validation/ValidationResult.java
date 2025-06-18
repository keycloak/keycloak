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

package org.keycloak.validation;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ValidationResult {
    private final boolean valid;
    private final Set<ValidationError> errors;

    public ValidationResult(Set<ValidationError> errors) {
        this.valid = errors.size() == 0;
        this.errors = Collections.unmodifiableSet(errors);
    }

    public boolean isValid() {
        return valid;
    }

    public Set<ValidationError> getErrors() {
        return errors;
    }

    public String getAllErrorsAsString() {
        return getAllErrorsAsString(ValidationError::getMessage);
    }

    public String getAllLocalizedErrorsAsString(Properties messagesBundle) {
        return getAllErrorsAsString(x -> x.getLocalizedMessage(messagesBundle));
    }

    protected String getAllErrorsAsString(Function<ValidationError, String> function) {
        return errors.stream().map(function).collect(Collectors.joining("; "));
    }

    public boolean fieldHasError(String fieldId) {
        if (fieldId == null) {
            return false;
        }
        for (ValidationError error : errors) {
            if (fieldId.equals(error.getFieldId())) {
                return true;
            }
        }
        return false;
    }
}
