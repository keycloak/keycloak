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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import jakarta.ws.rs.core.Response;

import org.keycloak.validate.ValidationError;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class ValidationException extends RuntimeException {

    private final Map<String, List<Error>> errors;

    public ValidationException(ValidationError error) {
        errors = Map.of(error.getMessage(), List.of(new Error(error)));
    }

    private ValidationException(Map<String, List<Error>> errors) {
        this.errors = errors;
    }

    public List<Error> getErrors() {
        return errors.values().stream().reduce(new ArrayList<>(), (l, r) -> {
            l.addAll(r);
            return l;
        }, (l, r) -> l);
    }

    public boolean hasError(String... types) {
        if (types.length == 0) {
            return !errors.isEmpty();
        }

        for (String type : types) {
            if (errors.containsKey(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if there are validation errors related to the attribute with the given {@code name}.
     *
     * @param name
     * @return
     */
    public boolean isAttributeOnError(String... name) {
        if (name.length == 0) {
            return !errors.isEmpty();
        }

        List<String> names = Arrays.asList(name);

        return errors.values().stream().flatMap(Collection::stream).anyMatch(error -> names.contains(error.getAttribute()));
    }

    @Override
    public String toString() {
        return "ValidationException [errors=" + errors + "]";
    }

    @Override
    public String getMessage() {
        return toString();
    }

    /**
     * Creating a light-weight consumer of validation errors to avoid creating an empty exception which has an expensive stack trace without having the need for it.
     */
    public static class ValidationExceptionBuilder implements Consumer<ValidationError> {

        private final Map<String, List<Error>> errors = new HashMap<>();

        @Override
        public void accept(ValidationError error) {
            addError(error);
        }

        void addError(ValidationError error) {
            List<Error> errors = this.errors.computeIfAbsent(error.getMessage(), (k) -> new ArrayList<>());
            errors.add(new Error(error));
        }

        public boolean hasError() {
            return !errors.isEmpty();
        }

        public ValidationException build() {
            return new ValidationException(errors);
        }
    }

    public Response.Status getStatusCode() {
        for (Map.Entry<String, List<Error>> entry : errors.entrySet()) {
            for (Error error : entry.getValue()) {
                if (!Response.Status.BAD_REQUEST.equals(error.getStatusCode())) {
                    return error.getStatusCode();
                }
            }
        }
        return Response.Status.BAD_REQUEST;
    }

    public static class Error implements Serializable {

        private final ValidationError error;

        public Error(ValidationError error) {
            this.error = error;
        }

        public String getAttribute() {
            return error.getInputHint();
        }

        public String getMessage() {
            return error.getMessage();
        }

        public Object[] getMessageParameters() {
            return error.getInputHintWithMessageParameters();
        }

        @Override
        public String toString() {
            return "Error [error=" + error + "]";
        }

        public String getFormattedMessage(BiFunction<String, Object[], String>  messageFormatter) {
            return messageFormatter.apply(getMessage(), getMessageParameters());
        }

        public Response.Status getStatusCode() {
            return error.getStatusCode();
        }
    }

}
