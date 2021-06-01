package org.keycloak.validate;

import static org.keycloak.validate.ValidatorConfig.configFromMap;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.validate.validators.LengthValidator;
import org.keycloak.validate.validators.NotBlankValidator;
import org.keycloak.validate.validators.ValidatorConfigValidator;

public class ValidatorTest {

    KeycloakSession session = null;

    @Test
    public void simpleValidation() {

        Validator validator = Validators.notEmptyValidator();

        Assert.assertTrue(validator.validate("a").isValid());
        Assert.assertFalse(validator.validate("").isValid());
    }

    @Test
    public void simpleValidationWithContext() {

        Validator validator = Validators.lengthValidator();

        ValidationContext context = new ValidationContext(session);
        validator.validate("a", "username", context);
        ValidationResult result = context.toResult();

        Assert.assertTrue(result.isValid());
    }

    @Test
    public void simpleValidationFluent() {

        ValidationContext context = new ValidationContext(session);

        ValidationResult result = Validators.lengthValidator().validate("a", "username", context).toResult();

        Assert.assertTrue(result.isValid());
    }

    @Test
    public void simpleValidationLookup() {

        // later: session.validators().validator(LengthValidator.ID);
        Validator validator = Validators.validator(session, LengthValidator.ID);

        ValidationContext context = new ValidationContext(session);
        validator.validate("a", "username", context);
        ValidationResult result = context.toResult();

        Assert.assertTrue(result.isValid());
    }

    @Test
    public void simpleValidationError() {

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
        Assert.assertEquals(LengthValidator.MESSAGE_INVALID_LENGTH, error.getMessage());
        Assert.assertEquals(new Integer(2), error.getMessageParameters()[0]);

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
        Validators.notEmptyValidator().validate("a").toResult().ifNotValidAccept(r -> bool1.set(true));
        Assert.assertFalse(bool1.get());

        AtomicBoolean bool2 = new AtomicBoolean();
        Validators.notEmptyValidator().validate("").toResult().ifNotValidAccept(r -> bool2.set(true));
        Assert.assertTrue(bool2.get());
    }

    @Test
    public void forEachError() {

        List<String> errors = new ArrayList<>();
        MockAddress faultyAddress = new MockAddress("", "Saint-Maur-des-Fossés", null, "Germany");
        MockAddressValidator.INSTANCE.validate(faultyAddress, "address").toResult().forEachError(e -> {
            errors.add(e.getMessage());
        });

        Assert.assertEquals(Arrays.asList(NotBlankValidator.MESSAGE_BLANK, NotBlankValidator.MESSAGE_BLANK), errors);
    }

    @Test
    public void formatError() {

        Map<String, String> miniResourceBundle = new HashMap<>();
        miniResourceBundle.put("error-invalid-blank", "{0} is blank: <{1}>");
        miniResourceBundle.put("error-invalid-value", "{0} is invalid: <{1}>");

        List<String> errors = new ArrayList<>();
        MockAddress faultyAddress = new MockAddress("", "Saint-Maur-des-Fossés", null, "Germany");
        MockAddressValidator.INSTANCE.validate(faultyAddress, "address").toResult().forEachError(e -> {
            errors.add(e.formatMessage((message, args) -> MessageFormat.format(miniResourceBundle.getOrDefault(message, message), args)));
        });

        Assert.assertEquals(Arrays.asList("address.street is blank: <>", "address.zip is blank: <null>"), errors);
    }

    @Test
    public void multipleValidations() {

        ValidationContext context = new ValidationContext(session);

        String input = "aaa";
        String inputHint = "username";

        Validators.lengthValidator().validate(input, inputHint, context);
        Validators.notEmptyValidator().validate(input, inputHint, context);

        ValidationResult result = context.toResult();

        Assert.assertTrue(result.isValid());
    }

    @Test
    public void multipleValidationsError() {

        ValidationContext context = new ValidationContext(session);

        String input = " ";
        String inputHint = "username";

        Validators.lengthValidator().validate(input, inputHint, context, configFromMap(Collections.singletonMap(LengthValidator.KEY_MIN, 1)));
        Validators.notBlankValidator().validate(input, inputHint, context);

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
    public void validateValidatorConfigSimple() {

        SimpleValidator validator = LengthValidator.INSTANCE;

        Assert.assertFalse(validator.validateConfig(session, null).isValid());
        Assert.assertTrue(validator.validateConfig(session, configFromMap(Collections.singletonMap("min", 1))).isValid());
        Assert.assertTrue(validator.validateConfig(session, configFromMap(Collections.singletonMap("max", 100))).isValid());
        Assert.assertFalse(validator.validateConfig(session, configFromMap(Collections.singletonMap("min", null))).isValid());
        Assert.assertFalse(validator.validateConfig(session, configFromMap(Collections.singletonMap("min", "a"))).isValid());
        Assert.assertTrue(validator.validateConfig(session, configFromMap(Collections.singletonMap("min", "123"))).isValid());
    }

    @Test
    public void validateValidatorConfigMultipleOptions() {

        SimpleValidator validator = LengthValidator.INSTANCE;

        Map<String, Object> config = new HashMap<>();
        config.put("min", 1);
        config.put("max", 10);

        ValidatorConfig validatorConfig = configFromMap(config);

        Assert.assertTrue(validator.validateConfig(session, validatorConfig).isValid());
    }

    @Test
    public void validateValidatorConfigMultipleOptionsInvalidValues() {

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
    public void validateValidatorConfigViaValidatorFactory() {

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
    public void nestedValidation() {

        Assert.assertTrue(MockAddressValidator.INSTANCE.validate(
                new MockAddress("4848 Arcu St.", "Saint-Maur-des-Fossés", "02206", "Germany")
                , "address").isValid());

        ValidationResult result = MockAddressValidator.INSTANCE.validate(
                new MockAddress("", "Saint-Maur-des-Fossés", null, "Germany")
                , "address").toResult();
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
