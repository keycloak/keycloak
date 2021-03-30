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
import java.util.stream.Collectors;

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

    /**
     * Creates a new {@link ValidationResult} from the given errors.
     * <p>
     * The created {@link ValidationResult} is considered valid if the given {@code errors} are empty.
     *
     * @param errors
     */
    public ValidationResult(Set<ValidationError> errors) {
        this(errors == null || errors.isEmpty(), errors);
    }

    /**
     * Creates a new {@link ValidationResult} from the given errors and valid state.
     *
     * @param valid
     * @param errors
     */
    public ValidationResult(boolean valid, Set<ValidationError> errors) {
        this.valid = valid;
        this.errors = errors == null ? Collections.emptySet() : errors;
    }

    /**
     * Accepts a {@link Consumer<ValidationResult>} to visit the this current {@link ValidationResult}.
     *
     * @param consumer
     */
    @Override
    public void accept(Consumer<ValidationResult> consumer) {
        consumer.accept(this);
    }

    /**
     * Convenience method that accepts a {@link Consumer<ValidationResult>} if the result is not valid.
     *
     * @param consumer
     */
    public void ifNotValidAccept(Consumer<ValidationResult> consumer) {
        if (!isValid()) {
            accept(consumer);
        }
    }

    public void forEachError(Consumer<ValidationError> consumer) {
        for (ValidationError error : getErrors()) {
            consumer.accept(error);
        }
    }

    public boolean isValid() {
        return valid;
    }

    public Set<ValidationError> getErrors() {
        return errors;
    }

    public boolean hasErrorsForValidatorId(String id) {
        return getErrors().stream().anyMatch(e -> e.getValidatorId().equals(id));
    }

    public Set<ValidationError> getErrorsForValidatorId(String id) {
        return getErrors().stream().filter(e -> e.getValidatorId().equals(id)).collect(Collectors.toSet());
    }

    public boolean hasErrorsForInputHint(String inputHint) {
        return getErrors().stream().anyMatch(e -> e.getInputHint().equals(inputHint));
    }

    public Set<ValidationError> getErrorsForInputHint(String inputHint) {
        return getErrors().stream().filter(e -> e.getInputHint().equals(inputHint)).collect(Collectors.toSet());
    }
}