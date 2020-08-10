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

/**
 * Denotes a problem that occurred during a {@link Validation}.
 */
public class ValidationProblem {

    public enum Severity {
        /**
         * Warning: the validation was performed but created a warning. The value might still be considered valid.
         */
        WARNING,

        /**
         * Error: the validation failed
         */
        ERROR
    }

    /**
     * Holds the validation key.
     */
    private final ValidationKey key;

    /**
     * Holds the i18n validation message.
     */
    private final String message;

    /**
     * Holds the severity of the validation problem.
     */
    private final Severity severity;

    /**
     * Holds an exception that occurred during a Validation.
     */
    private final Exception exception;

    public ValidationProblem(ValidationKey key, String message, Severity severity, Exception exception) {
        this.key = key;
        this.message = message;
        this.severity = severity;
        this.exception = exception;
    }

    public ValidationKey getKey() {
        return key;
    }

    public String getMessage() {
        return message;
    }

    public Severity getSeverity() {
        return severity;
    }

    public boolean isError() {
        return severity == Severity.ERROR;
    }

    public boolean isWarning() {
        return severity == Severity.WARNING;
    }

    public Exception getException() {
        return exception;
    }

    public static ValidationProblem warning(ValidationKey key, String message) {
        return new ValidationProblem(key, message, Severity.WARNING, null);
    }

    public static ValidationProblem error(ValidationKey key, String message) {
        return error(key, message, null);
    }

    public static ValidationProblem error(ValidationKey key, String message, Exception ex) {
        return new ValidationProblem(key, message, Severity.ERROR, ex);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "key=" + key +
                ", message='" + message + '\'' +
                ", severity=" + severity +
                ", exception=" + exception +
                '}';
    }
}
