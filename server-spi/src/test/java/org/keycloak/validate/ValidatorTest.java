package org.keycloak.validate;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.validate.builtin.LengthValidator;
import org.keycloak.validate.builtin.NotEmptyValidator;

import java.util.Collections;

public class ValidatorTest {

    @Test
    public void simpleValidation() {

        KeycloakSession session = null;
        ValidationContext context = new ValidationContext(session, ValidationUseCase.Common.USER_REGISTRATION);

        LengthValidator.INSTANCE.validate("a", "username", context);

        ValidationResult result = context.toResult();

        Assert.assertTrue(result.isValid());
    }

    @Test
    public void simpleValidationFluent() {

        KeycloakSession session = null;
        ValidationContext context = new ValidationContext(session, ValidationUseCase.Common.USER_REGISTRATION);

        ValidationResult result = LengthValidator.INSTANCE.validate("a", "username", context).toResult();

        Assert.assertTrue(result.isValid());
    }


    @Test
    public void simpleValidationError() {

        KeycloakSession session = null;
        ValidationContext context = new ValidationContext(session, ValidationUseCase.Common.USER_REGISTRATION);

        String input = "a";
        String inputHint = "username";

        LengthValidator.INSTANCE.validate(input, inputHint, context, Collections.singletonMap("min", "2"));

        ValidationResult result = context.toResult();

        Assert.assertFalse(result.isValid());
        Assert.assertEquals(1, result.getErrors().size());

        ValidationError[] errors = result.getErrors().toArray(new ValidationError[0]);
        ValidationError error = errors[0];

        Assert.assertNotNull(error);
        Assert.assertEquals(LengthValidator.ID, error.getValidatorId());
        Assert.assertEquals(inputHint, error.getInputHint());
        Assert.assertEquals(LengthValidator.ERROR_INVALID_LENGTH, error.getMessage());
        Assert.assertEquals(input, error.getMessageParameters()[0]);
    }


    @Test
    public void multipleValidations() {

        KeycloakSession session = null;
        ValidationContext context = new ValidationContext(session, ValidationUseCase.Common.USER_REGISTRATION);

        String input = "aaa";
        String inputHint = "username";

        LengthValidator.INSTANCE.validate(input, inputHint, context);
        NotEmptyValidator.INSTANCE.validate(input, inputHint, context);

        ValidationResult result = context.toResult();

        Assert.assertTrue(result.isValid());
    }

    @Test
    public void multipleValidationsError() {

        KeycloakSession session = null;
        ValidationContext context = new ValidationContext(session, ValidationUseCase.Common.USER_REGISTRATION);

        String input = "";
        String inputHint = "username";

        LengthValidator.INSTANCE.validate(input, inputHint, context, Collections.singletonMap("min", 1));
        NotEmptyValidator.INSTANCE.validate(input, inputHint, context);

//        Map<String, Map<String, Object>> configs = new HashMap<>();
//        configs.put(LengthValidator.ID, Collections.singletonMap("min", 1));

//        Stream.of(LengthValidator.INSTANCE, NotEmptyValidator.INSTANCE).forEach(v -> {
//            v.validate(input, inputHint, context, configs.get(v.getId()));
//        });

        ValidationResult result = context.toResult();

        Assert.assertFalse(result.isValid());
        Assert.assertEquals(2, result.getErrors().size());

        ValidationError[] errors = result.getErrors().toArray(new ValidationError[0]);

        ValidationError error1 = errors[1];

        Assert.assertNotNull(error1);
        Assert.assertEquals(NotEmptyValidator.ID, error1.getValidatorId());
        Assert.assertEquals(inputHint, error1.getInputHint());
        Assert.assertEquals(NotEmptyValidator.ERROR_EMPTY, error1.getMessage());
        Assert.assertEquals(input, error1.getMessageParameters()[0]);
    }
}