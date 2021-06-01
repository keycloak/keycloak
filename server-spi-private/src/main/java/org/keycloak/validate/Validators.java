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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.validate.validators.LocalDateValidator;
import org.keycloak.validate.validators.EmailValidator;
import org.keycloak.validate.validators.IntegerValidator;
import org.keycloak.validate.validators.LengthValidator;
import org.keycloak.validate.validators.NotBlankValidator;
import org.keycloak.validate.validators.NotEmptyValidator;
import org.keycloak.validate.validators.DoubleValidator;
import org.keycloak.validate.validators.PatternValidator;
import org.keycloak.validate.validators.UriValidator;
import org.keycloak.validate.validators.ValidatorConfigValidator;

/**
 * Facade for Validation functions with support for {@link Validator} implementation lookup by id.
 */
public class Validators {

    /**
     * Holds a mapping of internal {@link SimpleValidator} to allow look-up via provider id.
     */
    private static final Map<String, SimpleValidator> INTERNAL_VALIDATORS;

    static {
        List<SimpleValidator> list = Arrays.asList(
                LengthValidator.INSTANCE,
                NotEmptyValidator.INSTANCE,
                UriValidator.INSTANCE,
                EmailValidator.INSTANCE,
                NotBlankValidator.INSTANCE,
                PatternValidator.INSTANCE,
                DoubleValidator.INSTANCE,
                IntegerValidator.INSTANCE,
                ValidatorConfigValidator.INSTANCE
        );

        INTERNAL_VALIDATORS = list.stream().collect(Collectors.toMap(SimpleValidator::getId, v -> v));
    }

    /**
     * Holds the {@link KeycloakSession}.
     */
    private final KeycloakSession session;

    /**
     * Creates a new {@link Validators} instance with the given {@link KeycloakSession}.
     *
     * @param session
     */
    public Validators(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Look-up for a built-in or registered {@link Validator} with the given provider {@code id}.
     *
     * @param id
     * @return
     * @see #validator(KeycloakSession, String)
     */
    public Validator validator(String id) {
        return validator(session, id);
    }

    /**
     * Look-up for a built-in or registered {@link ValidatorFactory} with the given provider {@code id}.
     *
     * @param id
     * @return
     * @see #validatorFactory(KeycloakSession, String)
     */
    public ValidatorFactory validatorFactory(String id) {
        return validatorFactory(session, id);
    }

    /**
     * Validates the {@link ValidatorConfig} of {@link Validator} referenced by the given provider {@code id}.
     *
     * @param id
     * @param config
     * @return
     * @see #validateConfig(KeycloakSession, String, ValidatorConfig)
     */
    public ValidationResult validateConfig(String id, ValidatorConfig config) {
        return validateConfig(session, id, config);
    }

    /* static import friendly accessor methods for built-in validators */

    public static Validator getInternalValidatorById(String id) {
        return INTERNAL_VALIDATORS.get(id);
    }

    public static ValidatorFactory getInternalValidatorFactoryById(String id) {
        return INTERNAL_VALIDATORS.get(id);
    }

    public static Map<String, Validator> getInternalValidators() {
        return Collections.unmodifiableMap(INTERNAL_VALIDATORS);
    }

    public static NotBlankValidator notBlankValidator() {
        return NotBlankValidator.INSTANCE;
    }

    public static NotEmptyValidator notEmptyValidator() {
        return NotEmptyValidator.INSTANCE;
    }

    public static LengthValidator lengthValidator() {
        return LengthValidator.INSTANCE;
    }

    public static UriValidator uriValidator() {
        return UriValidator.INSTANCE;
    }

    public static EmailValidator emailValidator() {
        return EmailValidator.INSTANCE;
    }

    public static PatternValidator patternValidator() {
        return PatternValidator.INSTANCE;
    }

    public static DoubleValidator doubleValidator() {
        return DoubleValidator.INSTANCE;
    }

    public static IntegerValidator integerValidator() {
        return IntegerValidator.INSTANCE;
    }

    public static LocalDateValidator dateValidator() {
        return LocalDateValidator.INSTANCE;
    }

    public static ValidatorConfigValidator validatorConfigValidator() {
        return ValidatorConfigValidator.INSTANCE;
    }

    /**
     * Look-up up for a built-in or registered {@link Validator} with the given validatorId.
     *
     * @param session the {@link KeycloakSession}
     * @param id      the id of the validator
     * @return the {@link Validator} or {@literal null}
     */
    public static Validator validator(KeycloakSession session, String id) {

        // Fast-path for internal Validators
        Validator validator = getInternalValidatorById(id);
        if (validator != null) {
            return validator;
        }

        if (session == null) {
            return null;
        }

        // Lookup validator in registry
        return session.getProvider(Validator.class, id);
    }

    /**
     * Look-up for a built-in or registered {@link ValidatorFactory} with the given validatorId.
     * <p>
     * This is intended for users who want to dynamically create new {@link Validator} instances, validate
     * {@link ValidatorConfig} configurations or create default configurations for a {@link Validator}.
     *
     * @param session the {@link KeycloakSession}
     * @param id      the id of the validator
     * @return the {@link Validator} or {@literal null}
     */
    public static ValidatorFactory validatorFactory(KeycloakSession session, String id) {

        // Fast-path for internal Validators
        ValidatorFactory factory = getInternalValidatorFactoryById(id);
        if (factory != null) {
            return factory;
        }

        if (session == null) {
            return null;
        }

        // Lookup factory in registry
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        return (ValidatorFactory) sessionFactory.getProviderFactory(Validator.class, id);
    }

    /**
     * Validates the {@link ValidatorConfig} of {@link Validator} referenced by the given provider {@code id}.
     *
     * @param session
     * @param id of the validator
     * @param config to be validated
     * @return
     */
    public static ValidationResult validateConfig(KeycloakSession session, String id, ValidatorConfig config) {

        ValidatorFactory validatorFactory = validatorFactory(session, id);
        if (validatorFactory != null) {
            return validatorFactory.validateConfig(session, config);
        }

        // We could not find a ValidationFactory to validate that config, so we assume the config is valid.
        return ValidationResult.OK;
    }
}
