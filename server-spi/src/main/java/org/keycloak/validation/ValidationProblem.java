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
 * Denotes a problem that occurred during validatoin.
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

    public ValidationProblem(ValidationKey key, String message, Severity severity) {
        this.key = key;
        this.message = message;
        this.severity = severity;
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

    public static ValidationProblem warning(ValidationKey key, String message) {
        return new ValidationProblem(key, message, Severity.WARNING);
    }

    public static ValidationProblem error(ValidationKey key, String message) {
        return new ValidationProblem(key, message, Severity.ERROR);
    }

    @Override
    public String toString() {
        return "ValidationProblem{" +
                "key='" + key + '\'' +
                ", message='" + message + '\'' +
                ", severity=" + severity +
                '}';
    }
}
