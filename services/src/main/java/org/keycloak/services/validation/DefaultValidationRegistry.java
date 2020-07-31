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
import org.keycloak.validation.Validation;
import org.keycloak.validation.ValidationContext;
import org.keycloak.validation.ValidationContextKey;
import org.keycloak.validation.ValidationKey;
import org.keycloak.validation.ValidationRegistration;
import org.keycloak.validation.ValidationRegistry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Default {@link ValidationRegistry} implementation.
 */
public class DefaultValidationRegistry implements ValidationRegistry.MutableValidationRegistry {

    private static final Logger LOGGER = Logger.getLogger(DefaultValidationProvider.class);

    private final ConcurrentMap<ValidationKey, SortedSet<ValidationRegistration>> validatorRegistrations = new ConcurrentHashMap<>();

    protected Stream<ValidationRegistration> getValidationRegistrationsStream(ValidationKey key) {

        SortedSet<ValidationRegistration> registrations = validatorRegistrations.get(key);

        if (registrations == null || registrations.isEmpty()) {
            return Stream.empty();
        }

        return registrations.stream();
    }

    @Override
    public List<Validation> getValidations(ValidationKey key) {
        return getValidationRegistrationsStream(key)
                .map(ValidationRegistration::getValidation)
                .collect(Collectors.toList());
    }

    @Override
    public Map<ValidationKey, List<Validation>> getValidations(Set<ValidationKey> keys) {

        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<ValidationKey, List<Validation>> validationMap = new LinkedHashMap<>();
        for (ValidationKey key : keys) {
            List<Validation> validations = getValidations(key);
            validationMap.put(key, validations);
        }

        return validationMap;
    }

    @Override
    public Map<ValidationKey, List<Validation>> resolveValidations(ValidationContext context, Set<ValidationKey> keys, Object value) {

        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<ValidationKey, List<Validation>> validationMap = new LinkedHashMap<>();
        for (ValidationKey key : keys) {
            List<Validation> validations = resolveValidations(context, key, value);
            validationMap.put(key, validations);
        }

        return validationMap;
    }

    @Override
    public List<Validation> resolveValidations(ValidationContext context, ValidationKey key, Object value) {
        return resolveValidationsInternal(key, getValidationRegistrationsStream(key), context, value);
    }

    protected List<Validation> resolveValidationsInternal(ValidationKey key, Stream<ValidationRegistration> registrations, ValidationContext context, Object value) {
        return filterSupportedValidationsStream(key, registrations, context, value)
                .collect(Collectors.toList());
    }

    protected Stream<Validation> filterSupportedValidationsStream(ValidationKey key, Stream<ValidationRegistration> registrations, ValidationContext context, Object value) {
        return filterEligibleRegistrationsStream(registrations, context)
                .filter(v -> v.isSupported(key, value, context));
    }

    protected Stream<Validation> filterEligibleRegistrationsStream(Stream<ValidationRegistration> registrations, ValidationContext context) {
        return registrations.filter(vr -> vr.isEligibleForContextKey(context.getContextKey())).sorted()
                .map(ValidationRegistration::getValidation);
    }

    @Override
    public void register(Validation validation, ValidationKey key, double order, Set<ValidationContextKey> contextKeys) {

        ValidationRegistration registration = new ValidationRegistration(key, validation, order, contextKeys);

        SortedSet<ValidationRegistration> registrations = validatorRegistrations.computeIfAbsent(key, t -> new TreeSet<>());

        boolean wasNew = registrations.add(registration);
        if (!wasNew) {
            LOGGER.debugf("Validation %s for key %s replaced existing validation.", validation.getClass().getName(), key);

            // remove existing registration (with same order)
            registrations.remove(registration);

            // add new registration
            registrations.add(registration);
        }
    }
}
