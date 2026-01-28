/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.validation;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;
import org.keycloak.validate.BuiltinValidators;
import org.keycloak.validate.SimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidationResult;
import org.keycloak.validate.Validator;
import org.keycloak.validate.ValidatorConfig;
import org.keycloak.validate.Validators;
import org.keycloak.validate.validators.EmailValidator;
import org.keycloak.validate.validators.LengthValidator;
import org.keycloak.validate.validators.NotBlankValidator;
import org.keycloak.validate.validators.ValidatorConfigValidator;

import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.validate.ValidatorConfig.configFromMap;

public class ValidatorsTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    @Test
    public void simpleValidation() {

        Validator validator = BuiltinValidators.notEmptyValidator();

        Assert.assertTrue(validator.validate("a").isValid());
        Assert.assertFalse(validator.validate("").isValid());
    }

    @Test
    @ModelTest
    public void simpleValidationWithContext(KeycloakSession session) {

        Validator validator = BuiltinValidators.lengthValidator();

        ValidationContext context = new ValidationContext(session);
        validator.validate("a", "username", context);
        ValidationResult result = context.toResult();

        Assert.assertTrue(result.isValid());
    }

    @Test
    @ModelTest
    public void simpleValidationFluent(KeycloakSession session) {

        ValidationContext context = new ValidationContext(session);

        ValidationResult result = BuiltinValidators.lengthValidator().validate("a", "username", context).toResult();

        Assert.assertTrue(result.isValid());
    }

    @Test
    @ModelTest
    public void simpleValidationLookup(KeycloakSession session) {

        // later: session.validators().validator(LengthValidator.ID);
        Validator validator = Validators.validator(session, LengthValidator.ID);

        ValidationContext context = new ValidationContext(session);
        validator.validate("a", "username", context);
        ValidationResult result = context.toResult();

        Assert.assertTrue(result.isValid());
    }

    @Test
    @ModelTest
    public void simpleValidationError(KeycloakSession session) {

        Validator validator = LengthValidator.INSTANCE;

        String input = "a";
        String inputHint = "username";

        ValidationContext context = new ValidationContext(session);
        validator.validate(input, inputHint, context, configFromMap(Collections.singletonMap("min", "2")));
        ValidationResult result = context.toResult();

        Assert.assertFalse(result.isValid());
        Assert.assertEquals(1, result.getErrors().size());

        ValidationError[] errors = result.getErrors().toArray(new ValidationError[0]);
        ValidationError error = errors[0];
        Assert.assertNotNull(error);
        Assert.assertEquals(LengthValidator.ID, error.getValidatorId());
        Assert.assertEquals(inputHint, error.getInputHint());
        Assert.assertEquals(LengthValidator.MESSAGE_INVALID_LENGTH_TOO_SHORT, error.getMessage());
        Assert.assertEquals(Integer.valueOf(2), error.getMessageParameters()[0]);

        Assert.assertTrue(result.hasErrorsForValidatorId(LengthValidator.ID));
        Assert.assertFalse(result.hasErrorsForValidatorId("unknown"));

        Assert.assertEquals(result.getErrors(), result.getErrorsForValidatorId(LengthValidator.ID));
        Assert.assertEquals(result.getErrors(), result.getErrorsForInputHint(inputHint));

        Assert.assertTrue(result.hasErrorsForInputHint(inputHint));
        Assert.assertFalse(result.hasErrorsForInputHint("email"));
    }

    @Test
    public void acceptOnError() {

        AtomicBoolean bool1 = new AtomicBoolean();
        BuiltinValidators.notEmptyValidator().validate("a").toResult().ifNotValidAccept(r -> bool1.set(true));
        Assert.assertFalse(bool1.get());

        AtomicBoolean bool2 = new AtomicBoolean();
        BuiltinValidators.notEmptyValidator().validate("").toResult().ifNotValidAccept(r -> bool2.set(true));
        Assert.assertTrue(bool2.get());
    }

    @Test
    @ModelTest
    public void forEachError(KeycloakSession session) {

        List<String> errors = new ArrayList<>();
        MockAddress faultyAddress = new MockAddress("", "Saint-Maur-des-Fossés", null, "Germany");
        MockAddressValidator.INSTANCE.validate(faultyAddress, "address", new ValidationContext(session)).toResult().forEachError(e -> {
            errors.add(e.getMessage());
        });

        Assert.assertEquals(Arrays.asList(NotBlankValidator.MESSAGE_BLANK, NotBlankValidator.MESSAGE_BLANK), errors);
    }

    @Test
    @ModelTest
    public void formatError(KeycloakSession session) {

        Map<String, String> miniResourceBundle = new HashMap<>();
        miniResourceBundle.put("error-invalid-blank", "{0} is blank: <{1}>");
        miniResourceBundle.put("error-invalid-value", "{0} is invalid: <{1}>");

        List<String> errors = new ArrayList<>();
        MockAddress faultyAddress = new MockAddress("", "Saint-Maur-des-Fossés", null, "Germany");
        MockAddressValidator.INSTANCE.validate(faultyAddress, "address", new ValidationContext(session)).toResult().forEachError(e -> {
            errors.add(e.formatMessage((message, args) -> MessageFormat.format(miniResourceBundle.getOrDefault(message, message), args)));
        });

        Assert.assertEquals(Arrays.asList("address.street is blank: <>", "address.zip is blank: <null>"), errors);
    }

    @Test
    @ModelTest
    public void multipleValidations(KeycloakSession session) {

        ValidationContext context = new ValidationContext(session);

        String input = "aaa";
        String inputHint = "username";

        BuiltinValidators.lengthValidator().validate(input, inputHint, context);
        BuiltinValidators.notEmptyValidator().validate(input, inputHint, context);

        ValidationResult result = context.toResult();

        Assert.assertTrue(result.isValid());
    }

    @Test
    @ModelTest
    public void multipleValidationsError(KeycloakSession session) {

        ValidationContext context = new ValidationContext(session);

        String input = " ";
        String inputHint = "username";

        BuiltinValidators.lengthValidator().validate(input, inputHint, context, configFromMap(Collections.singletonMap(LengthValidator.KEY_MIN, 1)));
        BuiltinValidators.notBlankValidator().validate(input, inputHint, context);

        ValidationResult result = context.toResult();

        Assert.assertFalse(result.isValid());
        Assert.assertEquals(2, result.getErrors().size());

        ValidationError[] errors = result.getErrors().toArray(new ValidationError[0]);

        ValidationError error1 = errors[1];

        Assert.assertNotNull(error1);
        Assert.assertEquals(NotBlankValidator.ID, error1.getValidatorId());
        Assert.assertEquals(inputHint, error1.getInputHint());
        Assert.assertEquals(NotBlankValidator.MESSAGE_BLANK, error1.getMessage());
        Assert.assertEquals(input, error1.getMessageParameters()[0]);
    }

    @Test
    @ModelTest
    public void validateValidatorConfigSimple(KeycloakSession session) {

        SimpleValidator validator = LengthValidator.INSTANCE;

        Assert.assertFalse(validator.validateConfig(session, null).isValid());
        Assert.assertTrue(validator.validateConfig(session, configFromMap(Collections.singletonMap("min", 1))).isValid());
        Assert.assertTrue(validator.validateConfig(session, configFromMap(Collections.singletonMap("max", 100))).isValid());
        Assert.assertFalse(validator.validateConfig(session, configFromMap(Collections.singletonMap("min", null))).isValid());
        Assert.assertFalse(validator.validateConfig(session, configFromMap(Collections.singletonMap("min", "a"))).isValid());
        Assert.assertTrue(validator.validateConfig(session, configFromMap(Collections.singletonMap("min", "123"))).isValid());
    }

    @Test
    @ModelTest
    public void validateEmailValidator(KeycloakSession session) {
        SimpleValidator validator = BuiltinValidators.emailValidator();

        Assert.assertTrue(validator.validateConfig(session, null).isValid());
        Assert.assertTrue(validator.validateConfig(session, ValidatorConfig.EMPTY).isValid());
        Assert.assertTrue(validator.validateConfig(session, configFromMap(Collections.singletonMap(
                EmailValidator.MAX_LOCAL_PART_LENGTH_PROPERTY, 128))).isValid());
        Assert.assertTrue(validator.validateConfig(session, configFromMap(Collections.singletonMap(
                EmailValidator.MAX_LOCAL_PART_LENGTH_PROPERTY, "128"))).isValid());
        Assert.assertFalse(validator.validateConfig(session, configFromMap(Collections.singletonMap(
                EmailValidator.MAX_LOCAL_PART_LENGTH_PROPERTY, null))).isValid());
        Assert.assertFalse(validator.validateConfig(session, configFromMap(Collections.singletonMap(
                EmailValidator.MAX_LOCAL_PART_LENGTH_PROPERTY, "a"))).isValid());
        Assert.assertFalse(validator.validateConfig(session, configFromMap(Collections.singletonMap(
                EmailValidator.MAX_LOCAL_PART_LENGTH_PROPERTY, ""))).isValid());
    }

    @Test
    @ModelTest
    public void validateValidatorConfigMultipleOptions(KeycloakSession session) {

        SimpleValidator validator = LengthValidator.INSTANCE;

        Map<String, Object> config = new HashMap<>();
        config.put("min", 1);
        config.put("max", 10);

        ValidatorConfig validatorConfig = configFromMap(config);

        Assert.assertTrue(validator.validateConfig(session, validatorConfig).isValid());
    }

    @Test
    @ModelTest
    public void validateValidatorConfigMultipleOptionsInvalidValues(KeycloakSession session) {

        SimpleValidator validator = LengthValidator.INSTANCE;

        Map<String, Object> config = new HashMap<>();
        config.put("min", "a");
        config.put("max", new ArrayList<>());

        ValidationResult result = validator.validateConfig(session, configFromMap(config));

        Assert.assertFalse(result.isValid());
        Assert.assertEquals(2, result.getErrors().size());

        ValidationError[] errors = result.getErrors().toArray(new ValidationError[0]);
        ValidationError error1 = errors[1];

        Assert.assertNotNull(error1);
        Assert.assertEquals(LengthValidator.ID, error1.getValidatorId());
        Assert.assertEquals("max", error1.getInputHint());
        Assert.assertEquals(ValidatorConfigValidator.MESSAGE_CONFIG_INVALID_NUMBER_VALUE, error1.getMessage());
        Assert.assertEquals(new ArrayList<>(), error1.getMessageParameters()[0]);
    }

    @Test
    @ModelTest
    public void validateValidatorConfigViaValidatorFactory(KeycloakSession session) {

        Map<String, Object> config = new HashMap<>();
        config.put("min", "a");
        config.put("max", new ArrayList<>());

        ValidatorConfig validatorConfig = configFromMap(config);

        ValidationResult result = Validators.validateConfig(session, LengthValidator.ID, validatorConfig);
        Assert.assertEquals(2, result.getErrors().size());

        ValidationError[] errors = result.getErrors().toArray(new ValidationError[0]);
        ValidationError error1 = errors[1];

        Assert.assertNotNull(error1);
        Assert.assertEquals(LengthValidator.ID, error1.getValidatorId());
        Assert.assertEquals("max", error1.getInputHint());
        Assert.assertEquals(ValidatorConfigValidator.MESSAGE_CONFIG_INVALID_NUMBER_VALUE, error1.getMessage());
        Assert.assertEquals(new ArrayList<>(), error1.getMessageParameters()[0]);
    }

    @Test
    @ModelTest
    public void nestedValidation(KeycloakSession session) {

        Assert.assertTrue(MockAddressValidator.INSTANCE.validate(
                new MockAddress("4848 Arcu St.", "Saint-Maur-des-Fossés", "02206", "Germany")
                , "address", new ValidationContext(session)).isValid());

        ValidationResult result = MockAddressValidator.INSTANCE.validate(
                new MockAddress("", "Saint-Maur-des-Fossés", null, "Germany")
                , "address", new ValidationContext(session)).toResult();
        Assert.assertFalse(result.isValid());
        Assert.assertEquals(2, result.getErrors().size());

        ValidationError[] errors = result.getErrors().toArray(new ValidationError[0]);

        ValidationError error0 = errors[0];

        Assert.assertNotNull(error0);
        Assert.assertEquals(NotBlankValidator.ID, error0.getValidatorId());
        Assert.assertEquals("address.street", error0.getInputHint());
        Assert.assertEquals(NotBlankValidator.MESSAGE_BLANK, error0.getMessage());
        Assert.assertEquals("", error0.getMessageParameters()[0]);

        ValidationError error1 = errors[1];

        Assert.assertNotNull(error1);
        Assert.assertEquals(NotBlankValidator.ID, error1.getValidatorId());
        Assert.assertEquals("address.zip", error1.getInputHint());
        Assert.assertEquals(NotBlankValidator.MESSAGE_BLANK, error1.getMessage());

    }

    static class MockAddress {

        private final String street;
        private final String city;
        private final String zip;
        private final String country;

        public MockAddress(String street, String city, String zip, String country) {
            this.street = street;
            this.city = city;
            this.zip = zip;
            this.country = country;
        }
    }

    static class MockAddressValidator implements SimpleValidator {

        public static MockAddressValidator INSTANCE = new MockAddressValidator();

        public static final String ID = "address";

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {

            if (!(input instanceof MockAddress)) {
                context.addError(new ValidationError(ID, inputHint, ValidationError.MESSAGE_INVALID_VALUE, input));
                return context;
            }

            MockAddress address = (MockAddress) input;
            // Access validator statically
            NotBlankValidator.INSTANCE.validate(address.street, inputHint + ".street", context);
            NotBlankValidator.INSTANCE.validate(address.city, inputHint + ".city", context);
            NotBlankValidator.INSTANCE.validate(address.country, inputHint + ".country", context);

            // Access validator via lookup (could be built-in or user-provided Validator)
            context.validator(NotBlankValidator.ID).validate(address.zip, inputHint + ".zip", context);

            return context;
        }
    }
}
