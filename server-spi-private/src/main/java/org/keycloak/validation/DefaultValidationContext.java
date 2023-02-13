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

import org.keycloak.models.KeycloakSession;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class DefaultValidationContext<T> implements ValidationContext<T> {

    private final Event event;
    private final KeycloakSession session;
    private final T objectToValidate;
    private final Set<ValidationError> errors;

    public DefaultValidationContext(Event event, KeycloakSession session, T objectToValidate) {
        this.event = event;
        this.session = session;
        this.objectToValidate = objectToValidate;
        this.errors = new HashSet<>();
    }

    @Override
    public Event getEvent() {
        return event;
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public T getObjectToValidate() {
        return objectToValidate;
    }

    @Override
    public ValidationContext<T> addError(String message) {
        return addError(null, message, null);
    }

    @Override
    public ValidationContext<T> addError(String fieldId, String message) {
        return addError(fieldId, message, null);
    }

    @Override
    public ValidationContext<T> addError(String fieldId, String message, String localizedMessageKey, Object... localizedMessageParams) {
        errors.add(new ValidationError(fieldId, message, localizedMessageKey, localizedMessageParams));
        return this;
    }

    @Override
    public ValidationResult toResult() {
        return new ValidationResult(new HashSet<>(errors));
    }
}
