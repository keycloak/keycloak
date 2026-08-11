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

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;
import org.keycloak.userprofile.validator.MultiValueValidator;
import org.keycloak.validate.AbstractSimpleValidator;
import org.keycloak.validate.BuiltinValidators;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidationResult;
import org.keycloak.validate.Validator;
import org.keycloak.validate.ValidatorConfig;
import org.keycloak.validate.validators.DoubleValidator;
import org.keycloak.validate.validators.EmailValidator;
import org.keycloak.validate.validators.IntegerValidator;
import org.keycloak.validate.validators.LengthValidator;
import org.keycloak.validate.validators.OptionsValidator;
import org.keycloak.validate.validators.PatternValidator;
import org.keycloak.validate.validators.UriValidator;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.validate.ValidatorConfig.configFromMap;

public class BuiltinValidatorsTest extends AbstractKeycloakTest {

    private static final ValidatorConfig valConfigIgnoreEmptyValues = ValidatorConfig.builder().config(AbstractSimpleValidator.IGNORE_EMPTY_VALUE, true).build();

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    @Test
    public void testLengthValidator() {

        Validator validator = BuiltinValidators.lengthValidator();

        // null and empty values handling
        Assertions.assertFalse(validator.validate(null, "name", configFromMap(ImmutableMap.of(LengthValidator.KEY_MIN, 1))).isValid());
        Assertions.assertFalse(validator.validate("", "name", configFromMap(ImmutableMap.of(LengthValidator.KEY_MIN, 1))).isValid());
        Assertions.assertFalse(validator.validate(" ", "name", configFromMap(ImmutableMap.of(LengthValidator.KEY_MIN, 1))).isValid());
        Assertions.assertTrue(validator.validate(" ", "name", configFromMap(ImmutableMap.of(LengthValidator.KEY_MAX, 10))).isValid());
        
        //KEYCLOAK-19006 reproducer
        Assertions.assertFalse(validator.validate("     ", "name", ValidatorConfig.builder().config(LengthValidator.KEY_MAX, 4).config(AbstractSimpleValidator.IGNORE_EMPTY_VALUE, true).config(LengthValidator.KEY_TRIM_DISABLED, true).build()).isValid());
        
        // min validation only
        Assertions.assertTrue(validator.validate("t", "name", ValidatorConfig.builder().config(LengthValidator.KEY_MIN, 1).build()).isValid());
        Assertions.assertFalse(validator.validate("tester", "name", ValidatorConfig.builder().config(LengthValidator.KEY_MIN, 7).build()).isValid());
        
        //min value validation with "empty value ignoration" configured
        Assertions.assertTrue(validator.validate(null, "name", ValidatorConfig.builder().config(LengthValidator.KEY_MIN, 1).config(AbstractSimpleValidator.IGNORE_EMPTY_VALUE, true).build()).isValid());
        Assertions.assertTrue(validator.validate("", "name", ValidatorConfig.builder().config(LengthValidator.KEY_MIN, 1).config(AbstractSimpleValidator.IGNORE_EMPTY_VALUE, true).build()).isValid());
        Assertions.assertFalse(validator.validate(" ", "name", ValidatorConfig.builder().config(LengthValidator.KEY_MIN, 1).config(AbstractSimpleValidator.IGNORE_EMPTY_VALUE, true).build()).isValid());
        Assertions.assertTrue(validator.validate("t", "name", ValidatorConfig.builder().config(LengthValidator.KEY_MIN, 1).config(AbstractSimpleValidator.IGNORE_EMPTY_VALUE, true).build()).isValid());
        Assertions.assertFalse(validator.validate("tester", "name", ValidatorConfig.builder().config(LengthValidator.KEY_MIN, 7).config(AbstractSimpleValidator.IGNORE_EMPTY_VALUE, true).build()).isValid());

        // max validation only
        Assertions.assertTrue(validator.validate("tester", "name", ValidatorConfig.builder().config(LengthValidator.KEY_MAX, 8).build()).isValid());
        Assertions.assertFalse(validator.validate("tester", "name", ValidatorConfig.builder().config(LengthValidator.KEY_MAX, 4).build()).isValid());
        
        //max value validation with "empty value ignoration" configured
        Assertions.assertTrue(validator.validate(null, "name", ValidatorConfig.builder().config(LengthValidator.KEY_MAX, 8).config(AbstractSimpleValidator.IGNORE_EMPTY_VALUE, true).build()).isValid());
        Assertions.assertTrue(validator.validate("tester", "name", ValidatorConfig.builder().config(LengthValidator.KEY_MAX, 8).config(AbstractSimpleValidator.IGNORE_EMPTY_VALUE, true).build()).isValid());
        Assertions.assertFalse(validator.validate("tester", "name", ValidatorConfig.builder().config(LengthValidator.KEY_MAX, 4).config(AbstractSimpleValidator.IGNORE_EMPTY_VALUE, true).build()).isValid());

        // both validations together
        ValidatorConfig config1 = configFromMap(ImmutableMap.of(LengthValidator.KEY_MIN, 3, LengthValidator.KEY_MAX, 4));
        Assertions.assertFalse(validator.validate("te", "name", config1).isValid());
        Assertions.assertTrue(validator.validate("tes", "name", config1).isValid());
        Assertions.assertTrue(validator.validate("test", "name", config1).isValid());
        Assertions.assertFalse(validator.validate("testr", "name", config1).isValid());

        // test value trimming performed by default
        Assertions.assertFalse(validator.validate("t ", "name", configFromMap(ImmutableMap.of(LengthValidator.KEY_MIN, 2))).isValid(), "trim not performed");
        Assertions.assertFalse(validator.validate(" t", "name", configFromMap(ImmutableMap.of(LengthValidator.KEY_MIN, 2))).isValid(), "trim not performed");
        Assertions.assertTrue(validator.validate("tr ", "name", configFromMap(ImmutableMap.of(LengthValidator.KEY_MAX, 2))).isValid(), "trim not performed");
        Assertions.assertTrue(validator.validate(" tr", "name", configFromMap(ImmutableMap.of(LengthValidator.KEY_MAX, 2))).isValid(), "trim not performed");
        
        // test value trimming disabled in config
        Assertions.assertTrue(validator.validate("tr ", "name", configFromMap(ImmutableMap.of(LengthValidator.KEY_MIN, 3, LengthValidator.KEY_TRIM_DISABLED, true))).isValid(), "trim disabled but performed");
        Assertions.assertFalse(validator.validate("trr ", "name", configFromMap(ImmutableMap.of(LengthValidator.KEY_MAX, 3, LengthValidator.KEY_TRIM_DISABLED, true))).isValid(), "trim disabled but performed");
        
        //test correct error message selection
        Assertions.assertEquals(LengthValidator.MESSAGE_INVALID_LENGTH_TOO_SHORT,validator.validate("", "name", ValidatorConfig.builder().config(LengthValidator.KEY_MIN, 1).build()).getErrors().iterator().next().getMessage());
        Assertions.assertEquals(LengthValidator.MESSAGE_INVALID_LENGTH,validator.validate("", "name", ValidatorConfig.builder().config(LengthValidator.KEY_MIN, 1).config(LengthValidator.KEY_MAX, 10).build()).getErrors().iterator().next().getMessage());
        Assertions.assertEquals(LengthValidator.MESSAGE_INVALID_LENGTH_TOO_LONG,validator.validate("aaa", "name", ValidatorConfig.builder().config(LengthValidator.KEY_MAX, 1).build()).getErrors().iterator().next().getMessage());
        Assertions.assertEquals(LengthValidator.MESSAGE_INVALID_LENGTH,validator.validate("aaa", "name", ValidatorConfig.builder().config(LengthValidator.KEY_MIN, 1).config(LengthValidator.KEY_MAX, 2).build()).getErrors().iterator().next().getMessage());
    }

    @Test
    @ModelTest
    public void testLengthValidator_ConfigValidation(KeycloakSession session) {

        // invalid min and max config values
        ValidatorConfig config = new ValidatorConfig(ImmutableMap.of(LengthValidator.KEY_MIN, new Object(), LengthValidator.KEY_MAX, "invalid"));

        ValidationResult result = BuiltinValidators.validatorConfigValidator().validate(config, LengthValidator.ID, new ValidationContext(session)).toResult();

        Assertions.assertFalse(result.isValid());
        ValidationError[] errors = result.getErrors().toArray(new ValidationError[0]);

        ValidationError error0 = errors[0];
        Assertions.assertNotNull(error0);
        Assertions.assertEquals(LengthValidator.ID, error0.getValidatorId());
        Assertions.assertEquals(LengthValidator.KEY_MIN, error0.getInputHint());

        ValidationError error1 = errors[1];
        Assertions.assertNotNull(error1);
        Assertions.assertEquals(LengthValidator.ID, error1.getValidatorId());
        Assertions.assertEquals(LengthValidator.KEY_MAX, error1.getInputHint());

        // empty config
        result = BuiltinValidators.validatorConfigValidator().validate(null, LengthValidator.ID, new ValidationContext(session)).toResult();
        Assertions.assertEquals(2, result.getErrors().size());
        result = BuiltinValidators.validatorConfigValidator().validate(ValidatorConfig.EMPTY, LengthValidator.ID, new ValidationContext(session)).toResult();
        Assertions.assertEquals(2, result.getErrors().size());

        // correct config
        Assertions.assertTrue(BuiltinValidators.validatorConfigValidator().validate(new ValidatorConfig(ImmutableMap.of(LengthValidator.KEY_MIN, "10")), LengthValidator.ID, new ValidationContext(session)).toResult().isValid());
        Assertions.assertTrue(BuiltinValidators.validatorConfigValidator().validate(new ValidatorConfig(ImmutableMap.of(LengthValidator.KEY_MAX, "10")), LengthValidator.ID, new ValidationContext(session)).toResult().isValid());
        Assertions.assertTrue(BuiltinValidators.validatorConfigValidator().validate(new ValidatorConfig(ImmutableMap.of(LengthValidator.KEY_MIN, "10", LengthValidator.KEY_MAX, "10")), LengthValidator.ID, new ValidationContext(session)).toResult().isValid());

        // max is smaller than min
        Assertions.assertFalse(BuiltinValidators.validatorConfigValidator().validate(new ValidatorConfig(ImmutableMap.of(LengthValidator.KEY_MIN, "10", LengthValidator.KEY_MAX, "9")), LengthValidator.ID, new ValidationContext(session)).toResult().isValid());
    }

    @Test
    public void testEmailValidator() {
        // this also validates StringFormatValidatorBase for simple values

        Validator validator = BuiltinValidators.emailValidator();

        Assertions.assertFalse(validator.validate(null, "email").isValid());
        Assertions.assertFalse(validator.validate("", "email").isValid());
        Assertions.assertFalse(validator.validate(" ", "email").isValid());
        
        // empty value ignoration configured
        Assertions.assertTrue(validator.validate(null, "emptyString", valConfigIgnoreEmptyValues).isValid());
        Assertions.assertTrue(validator.validate("", "emptyString", valConfigIgnoreEmptyValues).isValid());
        Assertions.assertFalse(validator.validate(" ", "blankString", valConfigIgnoreEmptyValues).isValid());
        
        Assertions.assertTrue(validator.validate("admin@example.org", "email").isValid());
        Assertions.assertTrue(validator.validate("admin+sds@example.org", "email").isValid());

        Assertions.assertFalse(validator.validate(" ", "email").isValid());
        Assertions.assertFalse(validator.validate("adminATexample.org", "email").isValid());

        Assertions.assertTrue(validator.validate("username@keycloak.org", "email", (ValidatorConfig) null).isValid());
        Assertions.assertTrue(validator.validate("abcd012345678901234567890123456789012345678901234567890123456789@keycloak.org", "email").isValid());
        Assertions.assertFalse(validator.validate("abcde012345678901234567890123456789012345678901234567890123456789@keycloak.org", "email").isValid());
        Assertions.assertTrue(validator.validate("abcdef0123456789@keycloak.org", "email",
                new ValidatorConfig(ImmutableMap.of(EmailValidator.MAX_LOCAL_PART_LENGTH_PROPERTY, "16"))).isValid());
        Assertions.assertFalse(validator.validate("abcdefg0123456789@keycloak.org", "email",
                new ValidatorConfig(ImmutableMap.of(EmailValidator.MAX_LOCAL_PART_LENGTH_PROPERTY, 16))).isValid());
        Assertions.assertTrue(validator.validate("ab012345678901234567890123456789@keycloak.org", "email",
                new ValidatorConfig(ImmutableMap.of(EmailValidator.MAX_LOCAL_PART_LENGTH_PROPERTY, "32"))).isValid());
        Assertions.assertFalse(validator.validate("abc012345678901234567890123456789@keycloak.org", "email",
                new ValidatorConfig(ImmutableMap.of(EmailValidator.MAX_LOCAL_PART_LENGTH_PROPERTY, 32))).isValid());
    }

    @Test
    public void testAbstractSimpleValidatorSupportForCollections() {

        Validator validator = BuiltinValidators.emailValidator();

        List<String> valuesCollection = new ArrayList<>();

        Assertions.assertTrue(validator.validate(valuesCollection, "email").isValid());

        valuesCollection.add("");
        Assertions.assertFalse(validator.validate(valuesCollection, "email").isValid());
        valuesCollection.add("admin@example.org");
        Assertions.assertTrue(validator.validate("admin@example.org", "email").isValid());

        // wrong value fails validation even it is not at first position
        valuesCollection.add(" ");
        Assertions.assertFalse(validator.validate(valuesCollection, "email").isValid());

        valuesCollection.remove(valuesCollection.size() - 1);
        valuesCollection.add("adminATexample.org");
        Assertions.assertFalse(validator.validate(valuesCollection, "email").isValid());

    }

    @Test
    public void testNotBlankValidator() {

        Validator validator = BuiltinValidators.notBlankValidator();

        // simple String value
        Assertions.assertTrue(validator.validate("tester", "username").isValid());
        Assertions.assertFalse(validator.validate("", "username").isValid());
        Assertions.assertFalse(validator.validate("   ", "username").isValid());
        Assertions.assertFalse(validator.validate(null, "username").isValid());

        // collection as input
        Assertions.assertTrue(validator.validate(Arrays.asList("a", "b"), "username").isValid());
        Assertions.assertFalse(validator.validate(new ArrayList<>(), "username").isValid());
        Assertions.assertFalse(validator.validate(Arrays.asList(""), "username").isValid());
        Assertions.assertFalse(validator.validate(Arrays.asList(" "), "username").isValid());
        Assertions.assertFalse(validator.validate(Arrays.asList("a", " "), "username").isValid());
        Assertions.assertFalse(validator.validate(Arrays.asList("a", new Object()), "username").isValid());

        // unsupported input type
        Assertions.assertFalse(validator.validate(new Object(), "username").isValid());
    }

    @Test
    public void testNotEmptyValidator() {

        Validator validator = BuiltinValidators.notEmptyValidator();

        Assertions.assertTrue(validator.validate("tester", "username").isValid());
        Assertions.assertTrue(validator.validate(" ", "username").isValid());
        Assertions.assertTrue(validator.validate(Arrays.asList(1, 2, 3), "numberList").isValid());
        Assertions.assertTrue(validator.validate(Collections.singleton("key"), "stringSet").isValid());
        Assertions.assertTrue(validator.validate(Collections.singletonMap("key", "value"), "stringMap").isValid());

        Assertions.assertFalse(validator.validate(null, "username").isValid());
        Assertions.assertFalse(validator.validate("", "username").isValid());
        Assertions.assertFalse(validator.validate(Collections.emptyList(), "emptyList").isValid());
        Assertions.assertFalse(validator.validate(Collections.emptySet(), "emptySet").isValid());
        Assertions.assertFalse(validator.validate(Collections.emptyMap(), "emptyMap").isValid());
    }

    @Test
    public void testDoubleValidator() {

        Validator validator = BuiltinValidators.doubleValidator();

        // null value and empty String
        Assertions.assertFalse(validator.validate(null, "null").isValid());
        Assertions.assertFalse(validator.validate("", "emptyString").isValid());
        Assertions.assertFalse(validator.validate(" ", "blankString").isValid());
        
        // empty value ignoration configured
        Assertions.assertTrue(validator.validate(null, "emptyString", valConfigIgnoreEmptyValues).isValid());
        Assertions.assertTrue(validator.validate("", "emptyString", valConfigIgnoreEmptyValues).isValid());
        Assertions.assertTrue(validator.validate(" ", "blankString", valConfigIgnoreEmptyValues).isValid());

        // simple values
        Assertions.assertTrue(validator.validate(10, "age").isValid());
        Assertions.assertTrue(validator.validate("10", "age").isValid());
        Assertions.assertTrue(validator.validate("3.14", "pi").isValid());
        Assertions.assertTrue(validator.validate("   3.14   ", "piWithBlank").isValid());

        Assertions.assertFalse(validator.validate("a", "notAnumber").isValid());
        Assertions.assertFalse(validator.validate(true, "true").isValid());

        // collections
        Assertions.assertFalse(validator.validate(Arrays.asList(""), "age").isValid());
        Assertions.assertTrue(validator.validate(Arrays.asList(""), "age",valConfigIgnoreEmptyValues).isValid());
        Assertions.assertTrue(validator.validate(new ArrayList<>(), "age").isValid());
        Assertions.assertTrue(validator.validate(Arrays.asList(10), "age").isValid());
        Assertions.assertTrue(validator.validate(Arrays.asList(" 10 "), "age").isValid());
        Assertions.assertTrue(validator.validate(Arrays.asList("3.14"), "pi").isValid());
        Assertions.assertTrue(validator.validate(Arrays.asList("3.14", 10), "pi").isValid());

        Assertions.assertFalse(validator.validate(Arrays.asList("a"), "notAnumber").isValid());
        Assertions.assertFalse(validator.validate(Arrays.asList("3.14", "a"), "notANumberPresent").isValid());
        Assertions.assertFalse(validator.validate(Arrays.asList("3.14", new Object()), "notANumberPresent").isValid());
        
        // min only
        Assertions.assertTrue(validator.validate("10.1", "name", ValidatorConfig.builder().config(DoubleValidator.KEY_MIN, 1.4).build()).isValid());
        Assertions.assertFalse(validator.validate("10.1", "name", ValidatorConfig.builder().config(DoubleValidator.KEY_MIN, 100.5).build()).isValid());
        // min behavior around empty values
        Assertions.assertFalse(validator.validate(null, "name", ValidatorConfig.builder().config(DoubleValidator.KEY_MIN, 1.1).build()).isValid());
        Assertions.assertFalse(validator.validate("", "name", ValidatorConfig.builder().config(DoubleValidator.KEY_MIN, 1.1).build()).isValid());
        Assertions.assertFalse(validator.validate(" ", "name", ValidatorConfig.builder().config(DoubleValidator.KEY_MIN, 1.1).build()).isValid());
        Assertions.assertTrue(validator.validate(null, "name", ValidatorConfig.builder().config(DoubleValidator.KEY_MIN, 1.1).config(valConfigIgnoreEmptyValues).build()).isValid());
        Assertions.assertTrue(validator.validate("", "name", ValidatorConfig.builder().config(DoubleValidator.KEY_MIN, 1.1).config(valConfigIgnoreEmptyValues).build()).isValid());
        Assertions.assertTrue(validator.validate(" ", "name", ValidatorConfig.builder().config(DoubleValidator.KEY_MIN, 1.1).config(valConfigIgnoreEmptyValues).build()).isValid());
        
        // max only
        Assertions.assertFalse(validator.validate("10.5", "name", ValidatorConfig.builder().config(DoubleValidator.KEY_MAX, 1.1).build()).isValid());
        Assertions.assertTrue(validator.validate("10.5", "name", ValidatorConfig.builder().config(DoubleValidator.KEY_MAX, 100.1).build()).isValid());

        // min and max
        Assertions.assertFalse(validator.validate("10.09", "name", ValidatorConfig.builder().config(DoubleValidator.KEY_MIN, 10.1).config(DoubleValidator.KEY_MAX, 100).build()).isValid());
        Assertions.assertTrue(validator.validate("10.1", "name", ValidatorConfig.builder().config(DoubleValidator.KEY_MIN, 10.1).config(DoubleValidator.KEY_MAX, 100).build()).isValid());
        Assertions.assertTrue(validator.validate("100.1", "name", ValidatorConfig.builder().config(DoubleValidator.KEY_MIN, 10.1).config(DoubleValidator.KEY_MAX, 100.1).build()).isValid());
        Assertions.assertFalse(validator.validate("100.2", "name", ValidatorConfig.builder().config(DoubleValidator.KEY_MIN, 10.1).config(DoubleValidator.KEY_MAX, 100.1).build()).isValid());
        
        //test correct error message selection
        Assertions.assertEquals(DoubleValidator.MESSAGE_NUMBER_OUT_OF_RANGE_TOO_SMALL,validator.validate("10", "name", ValidatorConfig.builder().config(DoubleValidator.KEY_MIN, 100).build()).getErrors().iterator().next().getMessage());
        Assertions.assertEquals(DoubleValidator.MESSAGE_NUMBER_OUT_OF_RANGE,validator.validate("10", "name", ValidatorConfig.builder().config(DoubleValidator.KEY_MIN, 100).config(DoubleValidator.KEY_MAX, 1000).build()).getErrors().iterator().next().getMessage());
        Assertions.assertEquals(DoubleValidator.MESSAGE_NUMBER_OUT_OF_RANGE,validator.validate("10000", "name", ValidatorConfig.builder().config(DoubleValidator.KEY_MIN, 100).config(DoubleValidator.KEY_MAX, 1000).build()).getErrors().iterator().next().getMessage());
        Assertions.assertEquals(DoubleValidator.MESSAGE_NUMBER_OUT_OF_RANGE_TOO_BIG,validator.validate("10000", "name", ValidatorConfig.builder().config(DoubleValidator.KEY_MAX, 1000).build()).getErrors().iterator().next().getMessage());

    }

    @Test
    @ModelTest
    public void testDoubleValidator_ConfigValidation(KeycloakSession session) {

        // invalid min and max config values
        ValidatorConfig config = new ValidatorConfig(ImmutableMap.of(DoubleValidator.KEY_MIN, new Object(), DoubleValidator.KEY_MAX, "invalid"));

        ValidationResult result = BuiltinValidators.validatorConfigValidator().validate(config, DoubleValidator.ID, new ValidationContext(session)).toResult();

        Assertions.assertFalse(result.isValid());
        ValidationError[] errors = result.getErrors().toArray(new ValidationError[0]);

        ValidationError error0 = errors[0];
        Assertions.assertNotNull(error0);
        Assertions.assertEquals(DoubleValidator.ID, error0.getValidatorId());
        Assertions.assertEquals(DoubleValidator.KEY_MIN, error0.getInputHint());

        ValidationError error1 = errors[1];
        Assertions.assertNotNull(error1);
        Assertions.assertEquals(DoubleValidator.ID, error1.getValidatorId());
        Assertions.assertEquals(DoubleValidator.KEY_MAX, error1.getInputHint());

        // empty config
        result = BuiltinValidators.validatorConfigValidator().validate(null, DoubleValidator.ID, new ValidationContext(session)).toResult();
        Assertions.assertEquals(0, result.getErrors().size());
        result = BuiltinValidators.validatorConfigValidator().validate(ValidatorConfig.EMPTY, DoubleValidator.ID, new ValidationContext(session)).toResult();
        Assertions.assertEquals(0, result.getErrors().size());

        // correct config
        Assertions.assertTrue(BuiltinValidators.validatorConfigValidator().validate(new ValidatorConfig(ImmutableMap.of(DoubleValidator.KEY_MIN, "10.1")), DoubleValidator.ID, new ValidationContext(session)).toResult().isValid());
        Assertions.assertTrue(BuiltinValidators.validatorConfigValidator().validate(new ValidatorConfig(ImmutableMap.of(DoubleValidator.KEY_MAX, "10.1")), DoubleValidator.ID, new ValidationContext(session)).toResult().isValid());
        Assertions.assertTrue(BuiltinValidators.validatorConfigValidator().validate(new ValidatorConfig(ImmutableMap.of(DoubleValidator.KEY_MIN, "10.1", DoubleValidator.KEY_MAX, "11")), DoubleValidator.ID, new ValidationContext(session)).toResult().isValid());

        // max is smaller than min
        Assertions.assertFalse(BuiltinValidators.validatorConfigValidator().validate(new ValidatorConfig(ImmutableMap.of(DoubleValidator.KEY_MIN, "10.1", DoubleValidator.KEY_MAX, "10.1")), DoubleValidator.ID, new ValidationContext(session)).toResult().isValid());
    }

    @Test
    public void testIntegerValidator() {
        Validator validator = BuiltinValidators.integerValidator();

        // null value and empty String
        Assertions.assertFalse(validator.validate(null, "null").isValid());
        Assertions.assertFalse(validator.validate("", "emptyString").isValid());

        // empty value ignoration configured
        Assertions.assertTrue(validator.validate(null, "emptyString", valConfigIgnoreEmptyValues).isValid());
        Assertions.assertTrue(validator.validate("", "emptyString", valConfigIgnoreEmptyValues).isValid());
        Assertions.assertTrue(validator.validate(" ", "blankString", valConfigIgnoreEmptyValues).isValid());

        // simple values
        Assertions.assertTrue(validator.validate(10, "age").isValid());
        Assertions.assertTrue(validator.validate("10", "age").isValid());

        Assertions.assertFalse(validator.validate("3.14", "pi").isValid());
        Assertions.assertFalse(validator.validate("   3.14   ", "piWithBlank").isValid());
        Assertions.assertFalse(validator.validate("a", "notAnumber").isValid());
        Assertions.assertFalse(validator.validate(true, "true").isValid());

        // collections
        Assertions.assertTrue(validator.validate(new ArrayList<>(), "age").isValid());
        Assertions.assertFalse(validator.validate(Arrays.asList(""), "age").isValid());
        Assertions.assertTrue(validator.validate(Arrays.asList(""), "age",valConfigIgnoreEmptyValues).isValid());
        Assertions.assertTrue(validator.validate(Arrays.asList(10), "age").isValid());
        Assertions.assertFalse(validator.validate(Arrays.asList(" 10 "), "age").isValid());

        Assertions.assertFalse(validator.validate(Arrays.asList("3.14"), "pi").isValid());
        Assertions.assertFalse(validator.validate(Arrays.asList("3.14", 10), "pi").isValid());
        Assertions.assertFalse(validator.validate(Arrays.asList("a"), "notAnumber").isValid());
        Assertions.assertFalse(validator.validate(Arrays.asList("10", "a"), "notANumberPresent").isValid());
        Assertions.assertFalse(validator.validate(Arrays.asList("10", new Object()), "notANumberPresent").isValid());

        // min only
        Assertions.assertTrue(validator.validate("10", "name", ValidatorConfig.builder().config(IntegerValidator.KEY_MIN, 1).build()).isValid());
        Assertions.assertFalse(validator.validate("10", "name", ValidatorConfig.builder().config(IntegerValidator.KEY_MIN, 100).build()).isValid());
        // min behavior around empty values
        Assertions.assertFalse(validator.validate(null, "name", ValidatorConfig.builder().config(IntegerValidator.KEY_MIN, 1).build()).isValid());
        Assertions.assertFalse(validator.validate("", "name", ValidatorConfig.builder().config(IntegerValidator.KEY_MIN, 1).build()).isValid());
        Assertions.assertFalse(validator.validate(" ", "name", ValidatorConfig.builder().config(IntegerValidator.KEY_MIN, 1).build()).isValid());
        Assertions.assertTrue(validator.validate(null, "name", ValidatorConfig.builder().config(IntegerValidator.KEY_MIN, 1).config(valConfigIgnoreEmptyValues).build()).isValid());
        Assertions.assertTrue(validator.validate("", "name", ValidatorConfig.builder().config(IntegerValidator.KEY_MIN, 1).config(valConfigIgnoreEmptyValues).build()).isValid());
        Assertions.assertTrue(validator.validate(" ", "name", ValidatorConfig.builder().config(IntegerValidator.KEY_MIN, 1).config(valConfigIgnoreEmptyValues).build()).isValid());
        
        // max only
        Assertions.assertFalse(validator.validate("10", "name", ValidatorConfig.builder().config(IntegerValidator.KEY_MAX, 1).build()).isValid());
        Assertions.assertTrue(validator.validate("10", "name", ValidatorConfig.builder().config(IntegerValidator.KEY_MAX, 100).build()).isValid());

        // min and max
        Assertions.assertFalse(validator.validate("9", "name", ValidatorConfig.builder().config(IntegerValidator.KEY_MIN, 10).config(IntegerValidator.KEY_MAX, 100).build()).isValid());
        Assertions.assertTrue(validator.validate("10", "name", ValidatorConfig.builder().config(IntegerValidator.KEY_MIN, 10).config(IntegerValidator.KEY_MAX, 100).build()).isValid());
        Assertions.assertTrue(validator.validate("100", "name", ValidatorConfig.builder().config(IntegerValidator.KEY_MIN, 10).config(IntegerValidator.KEY_MAX, 100).build()).isValid());
        Assertions.assertFalse(validator.validate("101", "name", ValidatorConfig.builder().config(IntegerValidator.KEY_MIN, 10).config(IntegerValidator.KEY_MAX, 100).build()).isValid());

        Assertions.assertTrue(validator.validate(Long.MIN_VALUE, "name").isValid());
        Assertions.assertTrue(validator.validate(Long.MAX_VALUE, "name").isValid());
        
        //test correct error message selection
        Assertions.assertEquals(IntegerValidator.MESSAGE_NUMBER_OUT_OF_RANGE_TOO_SMALL,validator.validate("10", "name", ValidatorConfig.builder().config(IntegerValidator.KEY_MIN, 100).build()).getErrors().iterator().next().getMessage());
        Assertions.assertEquals(IntegerValidator.MESSAGE_NUMBER_OUT_OF_RANGE,validator.validate("10", "name", ValidatorConfig.builder().config(IntegerValidator.KEY_MIN, 100).config(IntegerValidator.KEY_MAX, 1000).build()).getErrors().iterator().next().getMessage());
        Assertions.assertEquals(IntegerValidator.MESSAGE_NUMBER_OUT_OF_RANGE,validator.validate("10000", "name", ValidatorConfig.builder().config(IntegerValidator.KEY_MIN, 100).config(IntegerValidator.KEY_MAX, 1000).build()).getErrors().iterator().next().getMessage());
        Assertions.assertEquals(IntegerValidator.MESSAGE_NUMBER_OUT_OF_RANGE_TOO_BIG,validator.validate("10000", "name", ValidatorConfig.builder().config(IntegerValidator.KEY_MAX, 1000).build()).getErrors().iterator().next().getMessage());
    }

    @Test
    @ModelTest
    public void testIntegerValidator_ConfigValidation(KeycloakSession session) {

        // invalid min and max config values
        ValidatorConfig config = new ValidatorConfig(ImmutableMap.of(IntegerValidator.KEY_MIN, new Object(), IntegerValidator.KEY_MAX, "invalid"));

        ValidationResult result = BuiltinValidators.validatorConfigValidator().validate(config, IntegerValidator.ID, new ValidationContext(session)).toResult();

        Assertions.assertFalse(result.isValid());
        ValidationError[] errors = result.getErrors().toArray(new ValidationError[0]);

        ValidationError error0 = errors[0];
        Assertions.assertNotNull(error0);
        Assertions.assertEquals(IntegerValidator.ID, error0.getValidatorId());
        Assertions.assertEquals(IntegerValidator.KEY_MIN, error0.getInputHint());

        ValidationError error1 = errors[1];
        Assertions.assertNotNull(error1);
        Assertions.assertEquals(IntegerValidator.ID, error1.getValidatorId());
        Assertions.assertEquals(IntegerValidator.KEY_MAX, error1.getInputHint());

        // empty config
        result = BuiltinValidators.validatorConfigValidator().validate(null, IntegerValidator.ID, new ValidationContext(session)).toResult();
        Assertions.assertEquals(0, result.getErrors().size());
        result = BuiltinValidators.validatorConfigValidator().validate(ValidatorConfig.EMPTY, IntegerValidator.ID, new ValidationContext(session)).toResult();
        Assertions.assertEquals(0, result.getErrors().size());

        // correct config
        Assertions.assertTrue(BuiltinValidators.validatorConfigValidator().validate(new ValidatorConfig(ImmutableMap.of(IntegerValidator.KEY_MIN, "10")), IntegerValidator.ID, new ValidationContext(session)).toResult().isValid());
        Assertions.assertTrue(BuiltinValidators.validatorConfigValidator().validate(new ValidatorConfig(ImmutableMap.of(IntegerValidator.KEY_MAX, "10")), IntegerValidator.ID, new ValidationContext(session)).toResult().isValid());
        Assertions.assertTrue(BuiltinValidators.validatorConfigValidator().validate(new ValidatorConfig(ImmutableMap.of(IntegerValidator.KEY_MIN, "10", IntegerValidator.KEY_MAX, "11")), IntegerValidator.ID, new ValidationContext(session)).toResult().isValid());

        // max is smaller than min
        Assertions.assertFalse(BuiltinValidators.validatorConfigValidator().validate(new ValidatorConfig(ImmutableMap.of(IntegerValidator.KEY_MIN, "10", IntegerValidator.KEY_MAX, "10")), IntegerValidator.ID, new ValidationContext(session)).toResult().isValid());
    }

    @Test
    public void testPatternValidator() {

        Validator validator = BuiltinValidators.patternValidator();

        // Pattern object in the configuration
        ValidatorConfig config = configFromMap(Collections.singletonMap(PatternValidator.CFG_PATTERN, Pattern.compile("^start-.*-end$")));
        Assertions.assertTrue(validator.validate("start-1234-end", "value", config).isValid());
        Assertions.assertFalse(validator.validate("start___end", "value", config).isValid());

        // String in the configuration
        config = configFromMap(Collections.singletonMap(PatternValidator.CFG_PATTERN, "^start-.*-end$"));
        Assertions.assertTrue(validator.validate("start-1234-end", "value", config).isValid());
        Assertions.assertFalse(validator.validate("start___end", "value", config).isValid());
        
        //custom error message
        config = ValidatorConfig.builder().config(PatternValidator.CFG_PATTERN, "^start-.*-end$").config(PatternValidator.CFG_ERROR_MESSAGE, "customError").build();
        Assertions.assertEquals("customError", validator.validate("start___end", "value", config).getErrors().iterator().next().getMessage());

        // null and empty values handling
        Assertions.assertFalse(validator.validate(null, "value", config).isValid());
        Assertions.assertFalse(validator.validate("", "value", config).isValid());
        Assertions.assertFalse(validator.validate(" ", "value", config).isValid());
        
        // empty value ignoration configured
        Assertions.assertTrue(validator.validate(null, "value", valConfigIgnoreEmptyValues).isValid());
        Assertions.assertTrue(validator.validate("", "value", valConfigIgnoreEmptyValues).isValid());
        Assertions.assertFalse(validator.validate(" ", "value", ValidatorConfig.builder().config(PatternValidator.CFG_PATTERN, "^[^\\s]$").config(valConfigIgnoreEmptyValues).build()).isValid());

    }

    @Test
    public void testUriValidator() throws Exception {

        Validator validator = BuiltinValidators.uriValidator();

        Assertions.assertTrue(validator.validate(null, "baseUrl").isValid());
        Assertions.assertTrue(validator.validate("", "baseUrl").isValid());
        Assertions.assertTrue(validator.validate("http://localhost:3000/", "baseUrl").isValid());
        Assertions.assertTrue(validator.validate("https://localhost:3000/", "baseUrl").isValid());
        Assertions.assertTrue(validator.validate("https://localhost:3000/#someFragment", "baseUrl").isValid());
        Assertions.assertTrue(validator.validate(new URL("https://localhost:3000/#someFragment"), "baseUrl").isValid());

        // Collections
        Assertions.assertTrue(validator.validate(Arrays.asList("https://localhost:3000/#someFragment", "https://localhost:3000"), "baseUrl").isValid());
        Assertions.assertTrue(validator.validate(Arrays.asList("https://localhost:3000/#someFragment"), "baseUrl").isValid());
        Assertions.assertTrue(validator.validate(Arrays.asList(new URL("https://localhost:3000/#someFragment")), "baseUrl").isValid());
        Assertions.assertTrue(validator.validate(Arrays.asList(""), "baseUrl").isValid());

        Assertions.assertFalse(validator.validate(" ", "baseUrl").isValid());
        Assertions.assertFalse(validator.validate("file:///somefile.txt", "baseUrl").isValid());
        Assertions.assertFalse(validator.validate("invalidUrl++@23", "invalidUri").isValid());
        Assertions.assertFalse(validator.validate(Arrays.asList("https://localhost:3000/#someFragment", "file:///somefile.txt"), "baseUrl").isValid());

        ValidatorConfig config = configFromMap(ImmutableMap.of(UriValidator.KEY_ALLOW_FRAGMENT, false));
        Assertions.assertFalse(validator.validate("https://localhost:3000/#someFragment", "baseUrl", config).isValid());

        // it is also possible to call dedicated validation methods on a built-in validator
        Assertions.assertTrue(BuiltinValidators.uriValidator().validateUri(new URI("https://customurl"), Collections.singleton("https"), true, true));

        Assertions.assertFalse(BuiltinValidators.uriValidator().validateUri(new URI("http://customurl"), Collections.singleton("https"), true, true));
    }
    
    @Test
    public void testOptionsValidator(){
        Validator validator = BuiltinValidators.optionsValidator();
        
        // options not configured - always invalid
        Assertions.assertFalse(validator.validate(null, "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, null).build()).isValid());
        Assertions.assertFalse(validator.validate("", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, null).build()).isValid());
        Assertions.assertFalse(validator.validate(" ", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, null).build()).isValid());
        Assertions.assertFalse(validator.validate("s", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, null).build()).isValid());
        
        // options not configured but empty and blanks ignored, others invalid
        Assertions.assertTrue(validator.validate(null, "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, null).config(valConfigIgnoreEmptyValues).build()).isValid());
        Assertions.assertTrue(validator.validate("", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, null).config(valConfigIgnoreEmptyValues).build()).isValid());
        Assertions.assertFalse(validator.validate(" ", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, null).config(valConfigIgnoreEmptyValues).build()).isValid());
        Assertions.assertFalse(validator.validate("s", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, null).config(valConfigIgnoreEmptyValues).build()).isValid());
        
        List<String> options = Arrays.asList("opt1", "opt2");
        
        // options configured
        Assertions.assertFalse(validator.validate(null, "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, options).build()).isValid());
        Assertions.assertFalse(validator.validate("", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, options).build()).isValid());
        Assertions.assertFalse(validator.validate(" ", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, options).build()).isValid());
        Assertions.assertFalse(validator.validate("Opt1", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, options).build()).isValid(), "must be case sensitive");
        Assertions.assertTrue(validator.validate("opt1", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, options).build()).isValid());
        Assertions.assertTrue(validator.validate("opt2", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, options).build()).isValid());
        Assertions.assertFalse(validator.validate("opt2 ", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, options).build()).isValid(), "trim not expected");
        Assertions.assertFalse(validator.validate(" opt2", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, options).build()).isValid(), "trim not expected");
        
        // options configured - empty and blanks ignored
        Assertions.assertTrue(validator.validate(null, "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, options).config(valConfigIgnoreEmptyValues).build()).isValid());
        Assertions.assertTrue(validator.validate("", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, options).config(valConfigIgnoreEmptyValues).build()).isValid());
        Assertions.assertFalse(validator.validate(" ", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, options).config(valConfigIgnoreEmptyValues).build()).isValid());
        Assertions.assertFalse(validator.validate("Opt1", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, options).config(valConfigIgnoreEmptyValues).build()).isValid(), "must be case sensitive");
        Assertions.assertTrue(validator.validate("opt1", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, options).config(valConfigIgnoreEmptyValues).build()).isValid());
        Assertions.assertTrue(validator.validate("opt2", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, options).config(valConfigIgnoreEmptyValues).build()).isValid());
        Assertions.assertFalse(validator.validate(" opt2", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, options).config(valConfigIgnoreEmptyValues).build()).isValid(), "trim not expected");
        Assertions.assertFalse(validator.validate("opt2 ", "test", ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, options).config(valConfigIgnoreEmptyValues).build()).isValid(), "trim not expected");
    }

    @Test
    @ModelTest
    public void testOptionsValidator_Config_Validation(KeycloakSession session) {
        
        ValidationResult result = BuiltinValidators.validatorConfigValidator().validate(ValidatorConfig.builder().build(), OptionsValidator.ID, new ValidationContext(session)).toResult();
        Assertions.assertFalse(result.isValid());

        // invalid type of the config value
        result = BuiltinValidators.validatorConfigValidator().validate(ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, "a").build(), OptionsValidator.ID, new ValidationContext(session)).toResult();
        Assertions.assertFalse(result.isValid());
        
        result = BuiltinValidators.validatorConfigValidator().validate(ValidatorConfig.builder().config(OptionsValidator.KEY_OPTIONS, Arrays.asList("opt1")).build(), OptionsValidator.ID, new ValidationContext(session)).toResult();
        Assertions.assertTrue(result.isValid());

    }

    @Test
    @ModelTest
    public void testMultivaluedValidatorConfiguration(KeycloakSession session) {
        // invalid min and max config values
        ValidatorConfig config = new ValidatorConfig(ImmutableMap.of(MultiValueValidator.KEY_MIN, new Object(), MultiValueValidator.KEY_MAX, "invalid"));
        ValidationResult result = BuiltinValidators.validatorConfigValidator().validate(config, MultiValueValidator.ID, new ValidationContext(session)).toResult();

        Assertions.assertFalse(result.isValid());
        ValidationError[] errors = result.getErrors().toArray(new ValidationError[0]);

        ValidationError error0 = errors[0];
        Assertions.assertNotNull(error0);
        Assertions.assertEquals(MultiValueValidator.ID, error0.getValidatorId());
        Assertions.assertEquals(MultiValueValidator.KEY_MAX, error0.getInputHint());

        ValidationError error1 = errors[1];
        Assertions.assertNotNull(error1);
        Assertions.assertEquals(MultiValueValidator.ID, error1.getValidatorId());
        Assertions.assertEquals(MultiValueValidator.KEY_MIN, error1.getInputHint());

        // empty config
        result = BuiltinValidators.validatorConfigValidator().validate(null, MultiValueValidator.ID, new ValidationContext(session)).toResult();
        Assertions.assertEquals(1, result.getErrors().size());
        result = BuiltinValidators.validatorConfigValidator().validate(ValidatorConfig.EMPTY, MultiValueValidator.ID, new ValidationContext(session)).toResult();
        Assertions.assertEquals(1, result.getErrors().size());

        // correct config
        Assertions.assertTrue(BuiltinValidators.validatorConfigValidator().validate(new ValidatorConfig(ImmutableMap.of(MultiValueValidator.KEY_MAX, "10")), MultiValueValidator.ID, new ValidationContext(session)).toResult().isValid());
        Assertions.assertTrue(BuiltinValidators.validatorConfigValidator().validate(new ValidatorConfig(ImmutableMap.of(MultiValueValidator.KEY_MIN, "10", MultiValueValidator.KEY_MAX, "10")), MultiValueValidator.ID, new ValidationContext(session)).toResult().isValid());
        Assertions.assertTrue(BuiltinValidators.validatorConfigValidator().validate(new ValidatorConfig(ImmutableMap.of(MultiValueValidator.KEY_MIN, "10", MultiValueValidator.KEY_MAX, "11")), MultiValueValidator.ID, new ValidationContext(session)).toResult().isValid());

        // max is smaller than min
        Assertions.assertFalse(BuiltinValidators.validatorConfigValidator().validate(new ValidatorConfig(ImmutableMap.of(MultiValueValidator.KEY_MIN, "10", MultiValueValidator.KEY_MAX, "9")), MultiValueValidator.ID, new ValidationContext(session)).toResult().isValid());

        // max not set
        Assertions.assertFalse(BuiltinValidators.validatorConfigValidator().validate(new ValidatorConfig(ImmutableMap.of(MultiValueValidator.KEY_MIN, "10")), MultiValueValidator.ID, new ValidationContext(session)).toResult().isValid());
    }
}
