package org.keycloak.services.validation;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.storage.adapter.InMemoryUserAdapter;
import org.keycloak.validation.ConditionalValidation;
import org.keycloak.validation.NamedValidation;
import org.keycloak.validation.NestedValidationContext;
import org.keycloak.validation.Validation.ValidationCondition;
import org.keycloak.validation.ValidationActionKey;
import org.keycloak.validation.ValidationContext;
import org.keycloak.validation.ValidationContextKey;
import org.keycloak.validation.ValidationKey;
import org.keycloak.validation.ValidationProblem;
import org.keycloak.validation.ValidationRegistry;
import org.keycloak.validation.ValidationRegistry.MutableValidationRegistry;
import org.keycloak.validation.ValidationResult;
import org.keycloak.validation.ValidatorProvider;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.keycloak.validation.ValidationContextKey.CLIENT_CONTEXT_KEY;
import static org.keycloak.validation.ValidationContextKey.DEFAULT_CONTEXT_KEY;
import static org.keycloak.validation.ValidationContextKey.USER_CONTEXT_KEY;
import static org.keycloak.validation.ValidationContextKey.USER_PROFILE_CONTEXT_KEY;
import static org.keycloak.validation.ValidationContextKey.USER_REGISTRATION_CONTEXT_KEY;

public class DefaultValidatorProviderTest {

    ValidatorProvider validator;

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

        registerDefaultValidations();

        ValidationContext context = new ValidationContext(USER_PROFILE_CONTEXT_KEY);

        ValidationResult result;
        ValidationProblem problem;

        result = validator.validate(context, "test@localhost", ValidationKey.USER_EMAIL);
        assertTrue("A valid email should be valid", result.isValid());
        assertFalse("A valid email should cause no problems", result.hasProblems());

        result = validator.validate(context, "", ValidationKey.USER_EMAIL);
        assertFalse("An empty email should be invalid", result.isValid());
        assertTrue("An empty email should cause report problems", result.hasProblems());
        problem = result.getErrors(ValidationKey.USER_EMAIL).get(0);
        assertEquals("An empty email should result in a problem", Messages.MISSING_EMAIL, problem.getMessage());
        assertTrue("An empty email should result in a missing email error", problem.isError());

        result = validator.validate(context, null, ValidationKey.USER_EMAIL);
        assertFalse("A null email should be invalid", result.isValid());
        assertTrue("A null email should cause report problems", result.hasProblems());
        problem = result.getErrors(ValidationKey.USER_EMAIL).get(0);
        assertEquals("An null email should result in a missing email problem", Messages.MISSING_EMAIL, problem.getMessage());

        result = validator.validate(context, "invalid", ValidationKey.USER_EMAIL);
        assertFalse("A null email should be invalid", result.isValid());
        assertTrue("A null email should cause report problems", result.hasProblems());
        problem = result.getErrors(ValidationKey.USER_EMAIL).get(0);
        assertEquals("An null email should result in a problem", Messages.INVALID_EMAIL, problem.getMessage());
        assertTrue("An null email should result in a invalid email error", problem.isError());
    }

    @Test
    public void validateMultipleFieldsInValidationContext() {

        registerDefaultValidations();

        ValidationContext context = new ValidationContext(USER_PROFILE_CONTEXT_KEY);

        ValidationResult result;
        ValidationProblem problem;

        result = validator.validate(context, "test@localhost", ValidationKey.USER_EMAIL);
        assertTrue("A valid email should be valid", result.isValid());
        assertFalse("A valid email should cause no problems", result.hasProblems());

        result = validator.validate(context, "Theo", ValidationKey.USER_FIRSTNAME);
        assertTrue("A valid firstname should be valid", result.isValid());
        assertFalse("A valid firstname should cause no problems", result.hasProblems());

        result = validator.validate(context, null, ValidationKey.USER_LASTNAME);
        assertFalse("An invalid lastname should be valid", result.isValid());
        assertTrue("An invalid lastname should cause no problems", result.hasProblems());
        problem = result.getErrors(ValidationKey.USER_LASTNAME).get(0);
        assertEquals("An invalid lastname should result in a problem", Messages.MISSING_LAST_NAME, problem.getMessage());
        assertTrue("An invalid lastname should result in a missing lastname error", problem.isError());
    }

    @Test
    public void validateWithCustomValidation() {

        registerDefaultValidations();

        registry.addValidation("custom_user_phone_validation",
                CustomValidations.USER_PHONE, CustomValidations::validatePhone,
                USER_PROFILE_CONTEXT_KEY);

        ValidationContext context = new ValidationContext(USER_PROFILE_CONTEXT_KEY);

        ValidationResult result;
        ValidationProblem problem;

        result = validator.validate(context, "+4912345678", CustomValidations.USER_PHONE);
        assertTrue("A valid phone number should be valid", result.isValid());
        assertFalse("A valid phone number should cause no problems", result.hasProblems());

        result = validator.validate(context, "", CustomValidations.USER_PHONE);
        assertFalse("A missing phone number should be invalid", result.isValid());
        assertTrue("A missing phone should cause problems", result.hasProblems());
        problem = result.getErrors(CustomValidations.USER_PHONE).get(0);
        assertEquals("A missing phone should result in a problem", CustomValidations.MISSING_PHONE, problem.getMessage());
        assertTrue("A missing phone should result in a missing email error", problem.isError());
    }

    @Test
    public void validateWithCustomValidationForBuiltInValidation() {

        registerDefaultValidations();

        registry.addValidation("custom_user_email_validation",
                ValidationKey.USER_EMAIL, CustomValidations::validateEmailCustom,
                USER_REGISTRATION_CONTEXT_KEY);

        ValidationContext context = new ValidationContext(USER_REGISTRATION_CONTEXT_KEY);

        ValidationResult result;
        ValidationProblem problem;

        result = validator.validate(context, "test@allowed", ValidationKey.USER_EMAIL);
        assertTrue("A valid email should be valid", result.isValid());
        assertFalse("A valid email should cause no problems", result.hasProblems());

        result = validator.validate(context, "test@notallowed", ValidationKey.USER_EMAIL);
        assertFalse("A not allowed email should be invalid", result.isValid());
        assertTrue("A not allowed email should cause report problems", result.hasProblems());
        problem = result.getErrors(ValidationKey.USER_EMAIL).get(0);
        assertEquals("A not allowed should result in a problem", CustomValidations.EMAIL_NOT_ALLOWED, problem.getMessage());
        assertTrue("A not allowed should result in a email not allowed error", problem.isError());
    }

    @Test
    public void validateWithReplacedDefaultValidation() {

        registerDefaultValidations();

        // replaces the existing validation for ValidationKey.USER_EMAIL
        registry.insertValidation("custom_user_email_validation",
                ValidationKey.USER_EMAIL, CustomValidations::validateEmailCustom,
                ValidationRegistry.DEFAULT_ORDER, USER_REGISTRATION_CONTEXT_KEY);

        List<NamedValidation> validations = registry.getValidations(ValidationKey.USER_EMAIL);
        assertEquals("Should have only one validation", 1, validations.size());
        assertSame("custom_user_email_validation", validations.get(0).getName());

        ValidationContext context = new ValidationContext(USER_REGISTRATION_CONTEXT_KEY);

        ValidationResult result;
        ValidationProblem problem;

        result = validator.validate(context, "test@allowed", ValidationKey.USER_EMAIL);
        assertTrue("A valid email should be valid", result.isValid());
        assertFalse("A valid email should cause no problems", result.hasProblems());

        result = validator.validate(context, "test@notallowed", ValidationKey.USER_EMAIL);
        assertFalse("A not allowed email should be invalid", result.isValid());
        assertTrue("A not allowed email should cause report problems", result.hasProblems());
        problem = result.getErrors(ValidationKey.USER_EMAIL).get(0);
        assertEquals("A not allowed should result in a problem", CustomValidations.EMAIL_NOT_ALLOWED, problem.getMessage());
        assertTrue("A not allowed should result in a email not allowed error", problem.isError());
    }

    @Test
    public void ignoreCustomValidationInDifferentValidationContext() {

        registry.addValidation("custom_user_phone_validation",
                CustomValidations.USER_PHONE, CustomValidations::validatePhone,
                USER_PROFILE_CONTEXT_KEY);

        ValidationContextKey differentContextKey = USER_REGISTRATION_CONTEXT_KEY;
        ValidationContext context = new ValidationContext(differentContextKey);

        ValidationResult result = validator.validate(context, "", CustomValidations.USER_PHONE);
        assertTrue("A missing phone number is valid without validation in the " + differentContextKey, result.isValid());
    }

    @Test
    public void customValidationInDifferentValidationContextsForSameValidationKey() {

        // allows value1 only
        registry.addValidation("custom_attribute_context1_validation",
                CustomValidations.USER_ATTRIBUTES_CUSTOM, CustomValidations::validateCustomAttributeContext1,
                CustomValidations.USER_CUSTOM_CONTEXT1_KEY);

        // allows value2 only
        registry.addValidation("custom_attribute_context2_validation",
                CustomValidations.USER_ATTRIBUTES_CUSTOM, CustomValidations::validateCustomAttributeContext2,
                CustomValidations.USER_CUSTOM_CONTEXT2_KEY);

        ValidationContext context;
        ValidationResult result;

        context = new ValidationContext(CustomValidations.USER_CUSTOM_CONTEXT1_KEY);
        result = validator.validate(context, "value1", CustomValidations.USER_ATTRIBUTES_CUSTOM);
        assertTrue(result.isValid());
        result = validator.validate(context, "value2", CustomValidations.USER_ATTRIBUTES_CUSTOM);
        assertFalse(result.isValid());

        context = new ValidationContext(CustomValidations.USER_CUSTOM_CONTEXT2_KEY);
        result = validator.validate(context, "value1", CustomValidations.USER_ATTRIBUTES_CUSTOM);
        assertFalse(result.isValid());
        result = validator.validate(context, "value2", CustomValidations.USER_ATTRIBUTES_CUSTOM);
        assertTrue(result.isValid());
    }

    @Test
    public void validateWithCustomValidationInCustomValidationContext() {

        registry.addValidation("custom_user_phone_validation",
                CustomValidations.USER_PHONE, CustomValidations::validatePhone,
                CustomValidations.USER_CUSTOM_CONTEXT_KEY);

        ValidationContext context = new ValidationContext(CustomValidations.USER_CUSTOM_CONTEXT_KEY);

        ValidationResult result = validator.validate(context, "", CustomValidations.USER_PHONE);
        assertFalse("A missing phone number should be invalid", result.isValid());
        assertTrue("A missing phone should cause problems", result.hasProblems());
    }

    @Test
    public void validateWithCustomValidationsFailFastOff() {

        registry.addValidation("custom_user_attribute1_validation",
                CustomValidations.USER_ATTRIBUTES_CUSTOM, CustomValidations::validateCustomAttribute1,
                CustomValidations.USER_CUSTOM_CONTEXT_KEY);

        registry.addValidation("custom_user_attribute2_validation",
                CustomValidations.USER_ATTRIBUTES_CUSTOM, CustomValidations::validateCustomAttribute2,
                CustomValidations.USER_CUSTOM_CONTEXT_KEY);

        ValidationContext context = new ValidationContext(CustomValidations.USER_CUSTOM_CONTEXT_KEY);

        ValidationResult result = validator.validate(context, "value3", CustomValidations.USER_ATTRIBUTES_CUSTOM);
        assertFalse("An invalid custom attribute should be invalid", result.isValid());
        assertTrue("An invalid custom attribute should cause problems", result.hasProblems());
        assertEquals(2, result.getProblems().size());
        assertEquals(2, result.getErrors().size());
        assertEquals(0, result.getErrors(ValidationKey.USER_USERNAME).size());
        List<ValidationProblem> errors = result.getErrors(CustomValidations.USER_ATTRIBUTES_CUSTOM);
        assertEquals(2, errors.size());
        assertEquals(CustomValidations.USER_ATTRIBUTES_CUSTOM, errors.get(0).getKey());
        assertEquals(CustomValidations.INVALID_ATTRIBUTE1, errors.get(0).getMessage());
        assertEquals(CustomValidations.INVALID_ATTRIBUTE2, errors.get(1).getMessage());
    }

    @Test
    public void validateWithCustomValidationsWithFailFast() {

        registry.addValidation("custom_user_attribute1_validation",
                CustomValidations.USER_ATTRIBUTES_CUSTOM, CustomValidations::validateCustomAttribute1,
                CustomValidations.USER_CUSTOM_CONTEXT_KEY);

        registry.addValidation("custom_user_attribute2_validation",
                CustomValidations.USER_ATTRIBUTES_CUSTOM, CustomValidations::validateCustomAttribute2,
                CustomValidations.USER_CUSTOM_CONTEXT_KEY);

        ValidationContext context = new ValidationContext(CustomValidations.USER_CUSTOM_CONTEXT_KEY).withFailFast(true);

        ValidationResult result = validator.validate(context, "value3", CustomValidations.USER_ATTRIBUTES_CUSTOM);
        assertFalse("An invalid custom attribute should be invalid", result.isValid());
        assertTrue("An invalid custom attribute should cause problems", result.hasProblems());
        assertEquals(1, result.getProblems().size());
        assertEquals(1, result.getErrors().size());
        assertEquals(0, result.getErrors(ValidationKey.USER_USERNAME).size());
        List<ValidationProblem> errors = result.getErrors(CustomValidations.USER_ATTRIBUTES_CUSTOM);
        assertEquals(1, errors.size());
        assertEquals(CustomValidations.USER_ATTRIBUTES_CUSTOM, errors.get(0).getKey());
        assertEquals(CustomValidations.INVALID_ATTRIBUTE1, errors.get(0).getMessage());
    }

    @Test
    public void validateCompoundObject() {

        registerDefaultValidations();

        UserModel user = new InMemoryUserAdapter(session, realm, "1");
        user.setFirstName("Theo");
        user.setLastName("Tester");
        user.setEmail("tester@allowed");

        ValidationContext context = new ValidationContext(USER_REGISTRATION_CONTEXT_KEY);
        // Note that we don't specify the ValidationKey here, since it is inferred by the given value (UserModel)
        ValidationResult result = validator.validate(context, user);
        assertTrue("A valid user should be valid", result.isValid());
    }

    @Test
    public void validateCompoundObjectWithNestedPropertiesAndDefaultValidations() {

        registerDefaultValidations();

        ValidationContext context = new ValidationContext(USER_REGISTRATION_CONTEXT_KEY);

        UserModel user = new InMemoryUserAdapter(session, realm, "1");

        user.setFirstName("");
        user.setLastName("");
        user.setEmail("");

        ValidationResult result = validator.validate(context, user, ValidationKey.USER);

        assertFalse("An invalid custom attribute should be invalid", result.isValid());
        assertTrue("An invalid custom attribute should cause problems", result.hasProblems());
        assertEquals(3, result.getProblems().size());
        assertEquals(3, result.getErrors().size());
        assertEquals(0, result.getErrors(ValidationKey.USER_USERNAME).size());
        List<ValidationProblem> errors = result.getErrors();
        assertEquals(Messages.MISSING_EMAIL, errors.get(0).getMessage());
        assertEquals(Messages.MISSING_FIRST_NAME, errors.get(1).getMessage());
        assertEquals(Messages.MISSING_LAST_NAME, errors.get(2).getMessage());
    }

    @Test
    public void validateCompoundObjectWithAdditionalCustomValidation() {

        registerDefaultValidations();

        registry.addValidation("custom_user_registration_validation",
                ValidationKey.USER, CustomValidations::validateCustomUserModel,
                USER_REGISTRATION_CONTEXT_KEY);

        ValidationContext context = new ValidationContext(USER_REGISTRATION_CONTEXT_KEY);

        UserModel user = new InMemoryUserAdapter(session, realm, "1");

        user.setFirstName(""); // missing
        user.setLastName(""); // missing
        user.setEmail("tester@notallowed"); // not allowed -> see CustomValidations::validateCustomUserModel

        ValidationResult result = validator.validate(context, user, ValidationKey.USER);

        assertFalse("An invalid custom attribute should be invalid", result.isValid());
        assertTrue("An invalid custom attribute should cause problems", result.hasProblems());
        assertEquals(3, result.getProblems().size());
        assertEquals(3, result.getErrors().size());
        assertEquals(0, result.getErrors(ValidationKey.USER_USERNAME).size());
        List<ValidationProblem> errors = result.getErrors();
        assertEquals(Messages.MISSING_FIRST_NAME, errors.get(0).getMessage());
        assertEquals(Messages.MISSING_LAST_NAME, errors.get(1).getMessage());
        assertEquals(CustomValidations.EMAIL_NOT_ALLOWED, errors.get(2).getMessage());
    }


    @Test
    public void validateWithExceptionShouldFail() {

        String exceptionDuringValidation = "exception_during_validation";

        registry.addValidation("custom_user_phone_validation",
                CustomValidations.USER_PHONE, (key, value, context) -> {
            throw new RuntimeException(exceptionDuringValidation);
        }, CustomValidations.USER_CUSTOM_CONTEXT_KEY);

        ValidationContext context = new ValidationContext(CustomValidations.USER_CUSTOM_CONTEXT_KEY);

        ValidationResult result = validator.validate(context, "+491234567", CustomValidations.USER_PHONE);
        assertFalse("An exception during validation should fail the validation", result.isValid());
        assertTrue("An exception during validation should cause problems", result.hasProblems());

        ValidationProblem problem = result.getErrors(CustomValidations.USER_PHONE).get(0);
        assertEquals(org.keycloak.validation.Validation.VALIDATION_ERROR, problem.getMessage());
        assertEquals(exceptionDuringValidation, problem.getException().getMessage());
    }


    @Test
    public void validateWithConditionalValidation() {

        org.keycloak.validation.Validation validation = (key, value, context) -> {
            int input = value instanceof Integer ? (Integer) value : -1;
            boolean valid = input >= 18;
            if (!valid) {
                context.addError(key, "invalid_age");
            }
            return valid;
        };

        ValidationCondition condition = (key, value, context) ->
                context.getAttributeAsBoolean("over18");

        registry.addValidation("custom_user_age_validation",
                CustomValidations.USER_ATTRIBUTES_CUSTOM, new ConditionalValidation(validation, condition),
                USER_REGISTRATION_CONTEXT_KEY);

        ValidationContext context;
        ValidationResult result;

        // validation should run
        context = new ValidationContext(USER_REGISTRATION_CONTEXT_KEY, Collections.singletonMap("over18", true));
        result = validator.validate(context, 15, CustomValidations.USER_ATTRIBUTES_CUSTOM);
        assertFalse("Conditional validation should fail the validation", result.isValid());
        assertTrue("Conditional validation should cause problems", result.hasProblems());

        // validation should NOT run
        context = new ValidationContext(USER_REGISTRATION_CONTEXT_KEY);
        result = validator.validate(context, 15, CustomValidations.USER_ATTRIBUTES_CUSTOM);
        assertTrue("Conditional validation that is not triggered should pass the validation", result.isValid());
        assertFalse("Conditional validation that is not triggered should not cause problems", result.hasProblems());
    }

    @Test
    public void defaultValidationShouldRunInEveryContext() {

        registry.addValidation("custom_user_attribute_validation",
                CustomValidations.USER_ATTRIBUTES_CUSTOM,
                (key, value, context) -> context.evaluateAndReportErrorIfFalse(() -> value != null, key, CustomValidations.INVALID_ATTRIBUTE),
                DEFAULT_CONTEXT_KEY);

        for (ValidationContextKey contextKey : ValidationContextKey.ALL_CONTEXT_KEYS) {
            ValidationContext context = new ValidationContext(contextKey);
            ValidationResult result = validator.validate(context, null, CustomValidations.USER_ATTRIBUTES_CUSTOM);
            assertFalse("Conditional validation should fail the validation", result.isValid());
            assertTrue("Conditional validation should cause problems", result.hasProblems());
            ValidationProblem problem = result.getErrors(CustomValidations.USER_ATTRIBUTES_CUSTOM).get(0);
            assertEquals(CustomValidations.INVALID_ATTRIBUTE, problem.getMessage());
        }
    }

    @Test
    public void validationWithWarningShouldStillBeValid() {

        registry.addValidation("custom_client_redirectUri",
                CustomValidations.CLIENT_REDIRECT_URI, (key, value, context) -> {

            String input = String.valueOf(value);
            URI uri;
            try {
                uri = URI.create(input);
            } catch (IllegalArgumentException iae) {
                context.addError(key, "redirect_uri_missing", iae);
                return false;
            }

            if (!"https".equals(uri.getScheme())) {
                context.addWarning(key, CustomValidations.WARNING_SECURITY_HTTPS_RECOMMENDED);
            }

            return true;
        }, CLIENT_CONTEXT_KEY);

        ValidationContext context = new ValidationContext(CLIENT_CONTEXT_KEY);
        ValidationResult result = validator.validate(context, "http://app/oidc_callback", CustomValidations.CLIENT_REDIRECT_URI);

        assertTrue("Validation with warning should still pass", result.isValid());
        assertTrue("Validation with warning should cause problems", result.hasProblems());
        assertTrue(result.getErrors().isEmpty());
        assertFalse(result.getWarnings().isEmpty());

        ValidationProblem problem = result.getWarnings(CustomValidations.CLIENT_REDIRECT_URI).get(0);
        assertEquals(CustomValidations.WARNING_SECURITY_HTTPS_RECOMMENDED, problem.getMessage());
        assertEquals(ValidationProblem.Severity.WARNING, problem.getSeverity());
    }

    @Test
    public void validateWithConfigurableValidation() {

        ValidationKey customRegex = ValidationKey.getOrCreate("custom_regex");

        registry.addValidation("custom_regex_validation", customRegex,
                // a dummy validation which can pull configuration from the given ValidationContext
                (ValidationKey key, Object value, NestedValidationContext context) -> {

                    String input = value instanceof String ? (String) value : null;

                    // TODO make extraction of validation configuration from ValidationContext more convenient
                    //  e.g.:
                    // T value = context.getConfig(key, "attribute", DEFAULT_T)
                    // Pattern regex = context.getConfig(key,"regex", DEFAULT)
                    Pattern regex = (Pattern) context.getAttribute(key.getName() + ".regex");

                    if (input == null ||
                            !regex.matcher(input).matches()) {

                        String message = (String)context.getAttribute(key.getName() + ".error_message");
                        context.addError(key, message);
                        return false;
                    }

                    return true;
                },
                CustomValidations.USER_CUSTOM_CONTEXT_KEY);

        // this config object can be created from a realm scoped validation configuration
        // configuration aware validations can be parameterized with values from a validationConfig map
        Map<String, Object> validationConfig = new HashMap<>();
        validationConfig.put("custom_regex.regex", Pattern.compile("^bubu.*$"));
        validationConfig.put("custom_regex.error_message", "custom_error");

        ValidationContext context = new ValidationContext(CustomValidations.USER_CUSTOM_CONTEXT_KEY)
                .withAttributes(validationConfig);

        ValidationResult result;
        result = validator.validate(context, "bubu123", customRegex);
        assertTrue("A matching string should be valid", result.isValid());
        assertFalse("A matching string should not cause problems", result.hasProblems());

        result = validator.validate(context, "gugu123", customRegex);
        assertFalse("A non matching string should be invalid", result.isValid());
        assertTrue("A non matching string should cause problems", result.hasProblems());
        assertEquals("custom_error", result.getProblems().get(0).getMessage());
    }

    @Test
    public void validateValueOnlyDuringUpdateAction() {

        registerDefaultValidations();

        registry.addValidation("custom_user_attribute_validation",
                CustomValidations.USER_ATTRIBUTES_CUSTOM, (key, value, context) -> {

            if (!ValidationActionKey.UPDATE.equals(context.getActionKey())) {
                return true;
            }

            boolean result = value != null;
            if (!result) {
                context.addError(key, CustomValidations.INVALID_ATTRIBUTE);
            }
            return result;
        }, DEFAULT_CONTEXT_KEY);

        ValidationContext context;
        ValidationResult result;

        // validation should run and fail with action update
        context = new ValidationContext(USER_CONTEXT_KEY).withActionKey(ValidationActionKey.UPDATE);
        result = validator.validate(context, null, CustomValidations.USER_ATTRIBUTES_CUSTOM);
        assertFalse("Conditional validation should fail the validation", result.isValid());
        assertTrue("Conditional validation should cause problems", result.hasProblems());
        ValidationProblem problem = result.getErrors(CustomValidations.USER_ATTRIBUTES_CUSTOM).get(0);
        assertEquals(CustomValidations.INVALID_ATTRIBUTE, problem.getMessage());

        // validation should run and pass without action update
        context = new ValidationContext(USER_CONTEXT_KEY);
        result = validator.validate(context, null, CustomValidations.USER_ATTRIBUTES_CUSTOM);
        assertTrue("Should not check value for default ContextActionKey", result.isValid());
    }

    protected void registerDefaultValidations() {
        new DefaultValidationProvider().register(registry);
    }

    interface CustomValidations {

        String MISSING_PHONE = "missing_phone";

        String EMAIL_NOT_ALLOWED = "invalid_email_not_allowed";

        String INVALID_ATTRIBUTE = "invalid_attribute";

        String INVALID_ATTRIBUTE1 = "invalid_attribute1";

        String INVALID_ATTRIBUTE2 = "invalid_attribute2";

        String WARNING_SECURITY_HTTPS_RECOMMENDED = "warning_security_https_recommended";

        ValidationContextKey USER_CUSTOM_CONTEXT_KEY = ValidationContextKey.newCustomValidationContextKey("user.custom", USER_CONTEXT_KEY);

        ValidationContextKey USER_CUSTOM_CONTEXT1_KEY = ValidationContextKey.newCustomValidationContextKey("user.custom1", USER_CUSTOM_CONTEXT_KEY);

        ValidationContextKey USER_CUSTOM_CONTEXT2_KEY = ValidationContextKey.newCustomValidationContextKey("user.custom2", USER_CUSTOM_CONTEXT_KEY);

        ValidationKey USER_PHONE = ValidationKey.getOrCreate("user.phone");

        ValidationKey USER_ATTRIBUTES_CUSTOM = ValidationKey.getOrCreate("user.attributes.customAttribute");

        ValidationKey CLIENT_REDIRECT_URI = ValidationKey.getOrCreate("client.redirectUri");

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

        static boolean validateCustomAttributeContext1(ValidationKey key, Object value, NestedValidationContext context) {
            if (!"value1".equals(value)) {
                context.addError(key, INVALID_ATTRIBUTE1);
                return false;
            }
            return true;
        }

        static boolean validateCustomAttributeContext2(ValidationKey key, Object value, NestedValidationContext context) {
            if (!"value2".equals(value)) {
                context.addError(key, INVALID_ATTRIBUTE2);
                return false;
            }
            return true;
        }

        static boolean validateCustomUserModel(ValidationKey key, Object value, NestedValidationContext context) {

            UserModel user = value instanceof UserModel ? (UserModel) value : null;
            if (user == null) {
                context.addError(key, Messages.INVALID_USER);
                return false;
            }

            return context.evaluateAndReportErrorIfFalse(() -> user.getEmail().endsWith("@allowed"), ValidationKey.USER_EMAIL, EMAIL_NOT_ALLOWED);
        }
    }
}
