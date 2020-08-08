package org.keycloak.services.validation;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.storage.adapter.InMemoryUserAdapter;
import org.keycloak.validation.DelegatingValidation;
import org.keycloak.validation.NamedValidation;
import org.keycloak.validation.NestedValidationContext;
import org.keycloak.validation.Validation.ValidationSupported;
import org.keycloak.validation.ValidationContext;
import org.keycloak.validation.ValidationContextKey;
import org.keycloak.validation.ValidationKey;
import org.keycloak.validation.ValidationKey.CustomValidationKey;
import org.keycloak.validation.ValidationProblem;
import org.keycloak.validation.ValidationRegistry;
import org.keycloak.validation.ValidationRegistry.MutableValidationRegistry;
import org.keycloak.validation.ValidationResult;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.keycloak.validation.ValidationContextKey.User.USER_ALL_CONTEXT_KEYS;
import static org.keycloak.validation.ValidationContextKey.User.USER_DEFAULT_CONTEXT_KEY;
import static org.keycloak.validation.ValidationContextKey.User.USER_PROFILE_UPDATE_CONTEXT_KEY;
import static org.keycloak.validation.ValidationContextKey.User.USER_REGISTRATION_CONTEXT_KEY;

public class DefaultValidatorProviderTest {

    DefaultValidatorProvider validator;
    MutableValidationRegistry registry;

    KeycloakSession session;

    RealmModel realm;

    @Before
    public void setup() {

        // TODO configure realm mock

        registry = new DefaultValidationRegistry();
        validator = new DefaultValidatorProvider(session, registry);
    }

    @Test
    public void validateEmail() {

        new DefaultValidationProvider().register(registry);

        ValidationContext context = new ValidationContext(USER_PROFILE_UPDATE_CONTEXT_KEY);

        ValidationResult result;
        ValidationProblem problem;

        result = validator.validate(context, "test@localhost", ValidationKey.User.EMAIL);
        assertTrue("A valid email should be valid", result.isValid());
        assertFalse("A valid email should cause no problems", result.hasProblems());

        result = validator.validate(context, "", ValidationKey.User.EMAIL);
        assertFalse("An empty email should be invalid", result.isValid());
        assertTrue("An empty email should cause report problems", result.hasProblems());
        problem = result.getErrors(ValidationKey.User.EMAIL).get(0);
        assertEquals("An empty email should result in a problem", Messages.MISSING_EMAIL, problem.getMessage());
        assertTrue("An empty email should result in a missing email error", problem.isError());

        result = validator.validate(context, null, ValidationKey.User.EMAIL);
        assertFalse("A null email should be invalid", result.isValid());
        assertTrue("A null email should cause report problems", result.hasProblems());
        problem = result.getErrors(ValidationKey.User.EMAIL).get(0);
        assertEquals("An null email should result in a missing email problem", Messages.MISSING_EMAIL, problem.getMessage());

        result = validator.validate(context, "invalid", ValidationKey.User.EMAIL);
        assertFalse("A null email should be invalid", result.isValid());
        assertTrue("A null email should cause report problems", result.hasProblems());
        problem = result.getErrors(ValidationKey.User.EMAIL).get(0);
        assertEquals("An null email should result in a problem", Messages.INVALID_EMAIL, problem.getMessage());
        assertTrue("An null email should result in a invalid email error", problem.isError());
    }

    @Test
    public void validateMultipleFieldsInValidationContext() {

        new DefaultValidationProvider().register(registry);

        ValidationContext context = new ValidationContext(USER_PROFILE_UPDATE_CONTEXT_KEY);

        ValidationResult result;
        ValidationProblem problem;

        result = validator.validate(context, "test@localhost", ValidationKey.User.EMAIL);
        assertTrue("A valid email should be valid", result.isValid());
        assertFalse("A valid email should cause no problems", result.hasProblems());

        result = validator.validate(context, "Theo", ValidationKey.User.FIRSTNAME);
        assertTrue("A valid firstname should be valid", result.isValid());
        assertFalse("A valid firstname should cause no problems", result.hasProblems());

        result = validator.validate(context, null, ValidationKey.User.LASTNAME);
        assertFalse("An invalid lastname should be valid", result.isValid());
        assertTrue("An invalid lastname should cause no problems", result.hasProblems());
        problem = result.getErrors(ValidationKey.User.LASTNAME).get(0);
        assertEquals("An invalid lastname should result in a problem", Messages.MISSING_LAST_NAME, problem.getMessage());
        assertTrue("An invalid lastname should result in a missing lastname error", problem.isError());
    }

    @Test
    public void validateWithCustomValidation() {

        new DefaultValidationProvider().register(registry);

        registry.register("custom_user_phone_validation",
                CustomValidations::validatePhone, CustomValidations.PHONE,
                ValidationRegistry.DEFAULT_ORDER, USER_PROFILE_UPDATE_CONTEXT_KEY);

        ValidationContext context = new ValidationContext(USER_PROFILE_UPDATE_CONTEXT_KEY);

        ValidationResult result;
        ValidationProblem problem;

        result = validator.validate(context, "+4912345678", CustomValidations.PHONE);
        assertTrue("A valid phone number should be valid", result.isValid());
        assertFalse("A valid phone number should cause no problems", result.hasProblems());

        result = validator.validate(context, "", CustomValidations.PHONE);
        assertFalse("A missing phone number should be invalid", result.isValid());
        assertTrue("A missing phone should cause problems", result.hasProblems());
        problem = result.getErrors(CustomValidations.PHONE).get(0);
        assertEquals("A missing phone should result in a problem", CustomValidations.MISSING_PHONE, problem.getMessage());
        assertTrue("A missing phone should result in a missing email error", problem.isError());
    }

    @Test
    public void validateWithCustomValidationForBuiltInValidation() {

        new DefaultValidationProvider().register(registry);

        registry.register("custom_user_email_validation",
                CustomValidations::validateEmailCustom, ValidationKey.User.EMAIL,
                ValidationRegistry.DEFAULT_ORDER + 1000.0, USER_REGISTRATION_CONTEXT_KEY);

        ValidationContext context = new ValidationContext(USER_REGISTRATION_CONTEXT_KEY);

        ValidationResult result;
        ValidationProblem problem;

        result = validator.validate(context, "test@allowed", ValidationKey.User.EMAIL);
        assertTrue("A valid email should be valid", result.isValid());
        assertFalse("A valid email should cause no problems", result.hasProblems());

        result = validator.validate(context, "test@notallowed", ValidationKey.User.EMAIL);
        assertFalse("A not allowed email should be invalid", result.isValid());
        assertTrue("A not allowed email should cause report problems", result.hasProblems());
        problem = result.getErrors(ValidationKey.User.EMAIL).get(0);
        assertEquals("A not allowed should result in a problem", CustomValidations.EMAIL_NOT_ALLOWED, problem.getMessage());
        assertTrue("A not allowed should result in a email not allowed error", problem.isError());
    }

    @Test
    public void validateWithReplacedDefaultValidation() {

        new DefaultValidationProvider().register(registry);

        // default order = 0.0 effectively replaces the existing validator
        registry.register("custom_user_email_validation",
                CustomValidations::validateEmailCustom, ValidationKey.User.EMAIL,
                ValidationRegistry.DEFAULT_ORDER, USER_REGISTRATION_CONTEXT_KEY);

        List<NamedValidation> validations = registry.getValidations(ValidationKey.User.EMAIL);
        assertEquals("Should have only one validation", 1, validations.size());
        assertSame("custom_user_email_validation", validations.get(0).getName());

        ValidationContext context = new ValidationContext(USER_REGISTRATION_CONTEXT_KEY);

        ValidationResult result;
        ValidationProblem problem;

        result = validator.validate(context, "test@allowed", ValidationKey.User.EMAIL);
        assertTrue("A valid email should be valid", result.isValid());
        assertFalse("A valid email should cause no problems", result.hasProblems());

        result = validator.validate(context, "test@notallowed", ValidationKey.User.EMAIL);
        assertFalse("A not allowed email should be invalid", result.isValid());
        assertTrue("A not allowed email should cause report problems", result.hasProblems());
        problem = result.getErrors(ValidationKey.User.EMAIL).get(0);
        assertEquals("A not allowed should result in a problem", CustomValidations.EMAIL_NOT_ALLOWED, problem.getMessage());
        assertTrue("A not allowed should result in a email not allowed error", problem.isError());
    }

    @Test
    public void ignoreCustomValidationInDifferentValidationContext() {

        ValidationContextKey contextKey = USER_PROFILE_UPDATE_CONTEXT_KEY;
        registry.register("custom_user_phone_validation",
                CustomValidations::validatePhone, CustomValidations.PHONE,
                ValidationRegistry.DEFAULT_ORDER, contextKey);

        ValidationContextKey differentContextKey = USER_REGISTRATION_CONTEXT_KEY;
        ValidationContext context = new ValidationContext(differentContextKey);

        ValidationResult result = validator.validate(context, "", CustomValidations.PHONE);
        assertTrue("A missing phone number is valid without validation in the " + differentContextKey, result.isValid());
    }

    @Test
    public void validateWithCustomValidationInCustomValidationContext() {

        ValidationContextKey contextKey = CustomValidations.USER_CUSTOM_CONTEXT_KEY;
        registry.register("custom_user_phone_validation",
                CustomValidations::validatePhone, CustomValidations.PHONE,
                ValidationRegistry.DEFAULT_ORDER, contextKey);

        ValidationContext context = new ValidationContext(contextKey);

        ValidationResult result = validator.validate(context, "", CustomValidations.PHONE);
        assertFalse("A missing phone number should be invalid", result.isValid());
        assertTrue("A missing phone should cause problems", result.hasProblems());
    }

    @Test
    public void validateWithCustomValidationsInBulkMode() {

        registry.register("custom_user_attribute1_validation",
                CustomValidations::validateCustomAttribute1, CustomValidations.CUSTOM_ATTRIBUTE,
                ValidationRegistry.DEFAULT_ORDER, CustomValidations.USER_CUSTOM_CONTEXT_KEY);

        registry.register("custom_user_attribute2_validation",
                CustomValidations::validateCustomAttribute2, CustomValidations.CUSTOM_ATTRIBUTE,
                ValidationRegistry.DEFAULT_ORDER + 1000.0, CustomValidations.USER_CUSTOM_CONTEXT_KEY);

        ValidationContext context = new ValidationContext(CustomValidations.USER_CUSTOM_CONTEXT_KEY);

        ValidationResult result = validator.validate(context, "value3", CustomValidations.CUSTOM_ATTRIBUTE);
        assertFalse("An invalid custom attribute should be invalid", result.isValid());
        assertTrue("An invalid custom attribute should cause problems", result.hasProblems());
        assertEquals(2, result.getProblems().size());
        assertEquals(2, result.getErrors().size());
        assertEquals(0, result.getErrors(ValidationKey.User.USERNAME).size());
        List<ValidationProblem> errors = result.getErrors(CustomValidations.CUSTOM_ATTRIBUTE);
        assertEquals(2, errors.size());
        assertEquals(CustomValidations.CUSTOM_ATTRIBUTE, errors.get(0).getKey());
        assertEquals(CustomValidations.INVALID_ATTRIBUTE1, errors.get(0).getMessage());
        assertEquals(CustomValidations.INVALID_ATTRIBUTE2, errors.get(1).getMessage());
    }

    @Test
    public void validateWithCustomValidationsWithoutBulkMode() {

        registry.register("custom_user_attribute1_validation",
                CustomValidations::validateCustomAttribute1, CustomValidations.CUSTOM_ATTRIBUTE,
                ValidationRegistry.DEFAULT_ORDER, CustomValidations.USER_CUSTOM_CONTEXT_KEY);

        registry.register("custom_user_attribute2_validation",
                CustomValidations::validateCustomAttribute2, CustomValidations.CUSTOM_ATTRIBUTE,
                ValidationRegistry.DEFAULT_ORDER + 1000.0, CustomValidations.USER_CUSTOM_CONTEXT_KEY);

        ValidationContext context = new ValidationContext(CustomValidations.USER_CUSTOM_CONTEXT_KEY).withBulkMode(false);

        ValidationResult result = validator.validate(context, "value3", CustomValidations.CUSTOM_ATTRIBUTE);
        assertFalse("An invalid custom attribute should be invalid", result.isValid());
        assertTrue("An invalid custom attribute should cause problems", result.hasProblems());
        assertEquals(1, result.getProblems().size());
        assertEquals(1, result.getErrors().size());
        assertEquals(0, result.getErrors(ValidationKey.User.USERNAME).size());
        List<ValidationProblem> errors = result.getErrors(CustomValidations.CUSTOM_ATTRIBUTE);
        assertEquals(1, errors.size());
        assertEquals(CustomValidations.CUSTOM_ATTRIBUTE, errors.get(0).getKey());
        assertEquals(CustomValidations.INVALID_ATTRIBUTE1, errors.get(0).getMessage());
    }

    @Test
    public void validateCompoundObjectWithNestedPropertiesAndDefaultValidations() {

        new DefaultValidationProvider().register(registry);

        registry.register("custom_user_registration_validation",
                CustomValidations::validateUserModel, ValidationKey.User.USER,
                ValidationRegistry.DEFAULT_ORDER + 1000.0, USER_REGISTRATION_CONTEXT_KEY);

        ValidationContext context = new ValidationContext(USER_REGISTRATION_CONTEXT_KEY);

        UserModel user = new InMemoryUserAdapter(session, realm, "1");

        user.setFirstName("Petra");
        user.setFirstName("Probe");
        user.setEmail("");

        ValidationResult result = validator.validate(context, user, ValidationKey.User.USER);

        assertFalse("An invalid custom attribute should be invalid", result.isValid());
        assertTrue("An invalid custom attribute should cause problems", result.hasProblems());
        assertEquals(3, result.getProblems().size());
        assertEquals(3, result.getErrors().size());
        assertEquals(0, result.getErrors(ValidationKey.User.USERNAME).size());
        List<ValidationProblem> errors = result.getErrors();
        assertEquals(CustomValidations.INVALID_USER_FIRSTNAME, errors.get(0).getMessage());
        assertEquals(CustomValidations.INVALID_USER_LASTNAME, errors.get(1).getMessage());
        assertEquals(Messages.MISSING_EMAIL, errors.get(2).getMessage());
    }

    @Test
    public void validateWithExceptionShouldFail() {

        String exceptionDuringValidation = "exception_during_validation";

        registry.register("custom_user_phone_validation",
                (key, value, context) -> {
                    throw new RuntimeException(exceptionDuringValidation);
                }, CustomValidations.PHONE, ValidationRegistry.DEFAULT_ORDER, CustomValidations.USER_CUSTOM_CONTEXT_KEY);

        ValidationContext context = new ValidationContext(CustomValidations.USER_CUSTOM_CONTEXT_KEY);

        ValidationResult result = validator.validate(context, "+491234567", CustomValidations.PHONE);
        assertFalse("An exception during validation should fail the validation", result.isValid());
        assertTrue("An exception during validation should cause problems", result.hasProblems());

        ValidationProblem problem = result.getErrors(CustomValidations.PHONE).get(0);
        assertEquals(org.keycloak.validation.Validation.VALIDATION_ERROR, problem.getMessage());
        assertEquals(exceptionDuringValidation, problem.getException().getMessage());
    }


    @Test
    public void validateWithConditionalValidation() {

        org.keycloak.validation.Validation userAgeValidation = (key, value, context) -> {
            int input = value instanceof Integer ? (Integer) value : -1;
            boolean valid = input >= 18;
            if (!valid) {
                context.addError(key, "invalid_age");
            }
            return valid;
        };

        ValidationSupported userAgeValidationCondition = (key, value, context) ->
                context.getAttributeAsBoolean("over18");

        registry.register("custom_user_age_validation",
                new DelegatingValidation(userAgeValidation, userAgeValidationCondition), CustomValidations.CUSTOM_ATTRIBUTE,
                ValidationRegistry.DEFAULT_ORDER, USER_REGISTRATION_CONTEXT_KEY);

        ValidationContext context;
        ValidationResult result;

        // validation should run
        context = new ValidationContext(USER_REGISTRATION_CONTEXT_KEY, Collections.singletonMap("over18", true));
        result = validator.validate(context, 15, CustomValidations.CUSTOM_ATTRIBUTE);
        assertFalse("Conditional validation should fail the validation", result.isValid());
        assertTrue("Conditional validation should cause problems", result.hasProblems());

        // validation should NOT run
        context = new ValidationContext(USER_REGISTRATION_CONTEXT_KEY);
        result = validator.validate(context, 15, CustomValidations.CUSTOM_ATTRIBUTE);
        assertTrue("Conditional validation that is not triggered should pass the validation", result.isValid());
        assertFalse("Conditional validation that is not triggered should not cause problems", result.hasProblems());
    }

    @Test
    public void defaultValidationShouldRunInEveryContext() {

        registry.register("custom_user_attribute_validation",
                (key, value, context) -> context.evaluateAndReportErrorIfFalse(() -> value != null, key, CustomValidations.INVALID_ATTRIBUTE),
                CustomValidations.CUSTOM_ATTRIBUTE, ValidationRegistry.DEFAULT_ORDER, USER_DEFAULT_CONTEXT_KEY);

        for (ValidationContextKey contextKey : USER_ALL_CONTEXT_KEYS) {
            ValidationContext context = new ValidationContext(contextKey);
            ValidationResult result = validator.validate(context, null, CustomValidations.CUSTOM_ATTRIBUTE);
            assertFalse("Conditional validation should fail the validation", result.isValid());
            assertTrue("Conditional validation should cause problems", result.hasProblems());
            ValidationProblem problem = result.getErrors(CustomValidations.CUSTOM_ATTRIBUTE).get(0);
            assertEquals(CustomValidations.INVALID_ATTRIBUTE, problem.getMessage());
        }
    }

    interface CustomValidations {

        String MISSING_PHONE = "missing_phone";

        String EMAIL_NOT_ALLOWED = "invalid_email_not_allowed";

        String INVALID_ATTRIBUTE = "invalid_attribute";

        String INVALID_ATTRIBUTE1 = "invalid_attribute1";

        String INVALID_ATTRIBUTE2 = "invalid_attribute2";

        ValidationContextKey USER_CUSTOM_CONTEXT_KEY = ValidationContextKey.newCustomValidationContextKey("user.custom", USER_DEFAULT_CONTEXT_KEY);

        CustomValidationKey PHONE = ValidationKey.newCustomKey("user.phone", true);

        CustomValidationKey CUSTOM_ATTRIBUTE = ValidationKey.newCustomKey("user.attributes.customAttribute", true);
        String INVALID_USER_FIRSTNAME = "invalid_user_firstname";
        String INVALID_USER_LASTNAME = "invalid_user_lastname";

        static boolean validatePhone(ValidationKey key, Object value, NestedValidationContext context) {

            String input = value instanceof String ? (String) value : null;

            if (Validation.isBlank(input)) {
                context.addError(key, MISSING_PHONE);
                return false;
            }

            return true;
        }

        static boolean validateEmailCustom(ValidationKey key, Object value, NestedValidationContext context) {

            String input = value instanceof String ? (String) value : null;

            if (input == null || !input.endsWith("@allowed")) {
                context.addError(key, EMAIL_NOT_ALLOWED);
                return false;
            }

            return true;
        }


        static boolean validateCustomAttribute1(ValidationKey key, Object value, NestedValidationContext context) {
            if (!"value1".equals(value)) {
                context.addError(key, INVALID_ATTRIBUTE1);
                return false;
            }
            return true;
        }

        static boolean validateCustomAttribute2(ValidationKey key, Object value, NestedValidationContext context) {
            if (!"value2".equals(value)) {
                context.addError(key, INVALID_ATTRIBUTE2);
                return false;
            }
            return true;
        }

        static boolean validateUserModel(ValidationKey key, Object value, NestedValidationContext context) {

            UserModel user = value instanceof UserModel ? (UserModel) value : null;
            if (user == null) {
                context.addError(key, Messages.INVALID_USER);
                return false;
            }

            boolean valid = true;
            if (!"Theo".equals(user.getFirstName())) {
                context.addError(ValidationKey.User.FIRSTNAME, INVALID_USER_FIRSTNAME);
                // go on with additional checks
                // return false;
                valid = false;
            }

            if (!"Tester".equals(user.getLastName())) {
                context.addError(ValidationKey.User.EMAIL, INVALID_USER_LASTNAME);
                // go on with additional checks
                // return false;
                valid = false;
            }

            // use a built-in validation in a nested validation
            if (!context.validateNested(ValidationKey.User.EMAIL, user.getEmail())) {
                valid = false;
            }

            return valid;
        }
    }


}