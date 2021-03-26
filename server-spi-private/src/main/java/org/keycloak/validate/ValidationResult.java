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
package org.keycloak.validate;

import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Denotes the result of a validation.
 */
public class ValidationResult implements Consumer<Consumer<ValidationResult>> {

    /**
     * An empty ValidationResult that's valid by default.
     */
    public static final ValidationResult OK = new ValidationResult(true, Collections.emptySet());

    /**
     * Holds the validation status.
     */
    private final boolean valid;

    /**
     * Holds the {@link ValidationError ValidationError's} that occurred during validation.
     */
    private final Set<ValidationError> errors;

    public ValidationResult(boolean valid, Set<ValidationError> errors) {
        this.valid = valid;
        this.errors = errors == null ? Collections.emptySet() : errors;
    }

    @Override
    public void accept(Consumer<ValidationResult> consumer) {
        consumer.accept(this);
    }

    public boolean isValid() {
        return valid;
    }

    public Set<ValidationError> getErrors() {
        return errors;
    }
}