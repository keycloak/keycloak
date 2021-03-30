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

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.validate.builtin.EmailValidator;
import org.keycloak.validate.builtin.LengthValidator;
import org.keycloak.validate.builtin.NotBlankValidator;
import org.keycloak.validate.builtin.NotEmptyValidator;
import org.keycloak.validate.builtin.NumberValidator;
import org.keycloak.validate.builtin.PatternValidator;
import org.keycloak.validate.builtin.UriValidator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Facade for Validation functions with support for {@link Validator} implementation lookup by id.
 */
public class Validators {

    // TODO Validator lookup via Validators should be provided via KeycloakSession API later

    /**
     * Holds a mapping of internal {@link CompactValidator} to allow look-up via provider id.
     */
    private static final Map<String, CompactValidator> INTERNAL_VALIDATORS;

    static {
        List<CompactValidator> list = Arrays.asList(
                LengthValidator.INSTANCE,
                NotEmptyValidator.INSTANCE,
                UriValidator.INSTANCE,
                EmailValidator.INSTANCE,
                NotBlankValidator.INSTANCE,
                PatternValidator.INSTANCE,
                NumberValidator.INSTANCE
        );

        Map<String, CompactValidator> validators = new HashMap<>();

        list.forEach(v -> validators.put(v.getId(), v));

        INTERNAL_VALIDATORS = validators;
    }

    private final KeycloakSession session;

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


//   public interface ClientModelValidator {
//      boolean hasValidUrls();
//   }

//    public ClientModelValidator validator(ClientModel client) {
//        return new ClientModelValidator() {
//            @Override
//            public boolean hasValidUrls() {
//                ValidationContext context = new ValidationContext();
//                for (String uri : client.getRedirectUris()) {
//                    uri().validate(uri, "redirectUri", context);
//                }
//                return context.isValid();
//            }
//        };
//    }

    public static Validator getInternalValidatorById(String id) {
        return INTERNAL_VALIDATORS.get(id);
    }

    public static ValidatorFactory getInternalValidatorFactoryById(String id) {
        return INTERNAL_VALIDATORS.get(id);
    }

    public static Map<String, Validator> getInternalValidators() {
        return Collections.unmodifiableMap(INTERNAL_VALIDATORS);
    }

    public static NotBlankValidator notBlank() {
        return NotBlankValidator.INSTANCE;
    }

    public static NotEmptyValidator notEmpty() {
        return NotEmptyValidator.INSTANCE;
    }

    public static LengthValidator length() {
        return LengthValidator.INSTANCE;
    }

    public static UriValidator uri() {
        return UriValidator.INSTANCE;
    }

    public static EmailValidator email() {
        return EmailValidator.INSTANCE;
    }

    public static PatternValidator pattern() {
        return PatternValidator.INSTANCE;
    }

    public static NumberValidator number() {
        return NumberValidator.INSTANCE;
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
     * @param id
     * @param config
     * @return
     */
    public static ValidationResult validateConfig(KeycloakSession session, String id, ValidatorConfig config) {

        ValidatorFactory validatorFactory = validatorFactory(session, id);
        if (validatorFactory != null) {
            return validatorFactory.validateConfig(config);
        }

        return ValidationResult.INVALID;
    }
}
