package org.keycloak.validate;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.validate.builtin.BuiltinValidators;
import org.keycloak.validate.builtin.UriValidator;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import static org.keycloak.validate.ValidatorConfig.configFromMap;

public class BuiltinValidatorsTest {

    @Test
    public void validateLength() {

        Validator validator = BuiltinValidators.length();

        {
            ValidatorConfig config = configFromMap(ImmutableMap.of("min", 1));
            ValidationResult result = validator.validate("tester", "name", config).toResult();
            Assert.assertTrue(result.isValid());
        }

        {
            ValidatorConfig config = configFromMap(ImmutableMap.of("min", 7));
            ValidationResult result = validator.validate("tester", "name", config).toResult();
            Assert.assertFalse(result.isValid());
        }
    }

    @Test
    public void validateEmail() {

        Validator validator = BuiltinValidators.email();

        {
            ValidationResult result = validator.validate("admin@example.org", "email")
                    .toResult();
            Assert.assertTrue(result.isValid());
        }

        {
            ValidationResult result = validator.validate("adminATexample.org", "email")
                    .toResult();
            Assert.assertFalse(result.isValid());
        }
    }

    @Test
    public void validateNotBlank() {

        Validator validator = BuiltinValidators.notBlank();

        {
            ValidationResult result = validator.validate("tester", "username")
                    .toResult();
            Assert.assertTrue(result.isValid());
        }

        Assert.assertFalse(validator.validate("", "username").isValid());
        Assert.assertFalse(validator.validate("   ", "username").isValid());
        Assert.assertFalse(validator.validate(null, "username").isValid());
    }

    @Test
    public void validateNotEmpty() {

        Validator validator = BuiltinValidators.notEmpty();

        Assert.assertTrue(validator.validate("tester", "username").isValid());
        Assert.assertTrue(validator.validate(Arrays.asList(1, 2, 3), "numberList").isValid());
        Assert.assertTrue(validator.validate(Collections.singleton("key"), "stringSet").isValid());
        Assert.assertTrue(validator.validate(Collections.singletonMap("key", "value"), "stringMap").isValid());

        Assert.assertFalse(validator.validate(null, "username").isValid());
        Assert.assertFalse(validator.validate(Collections.emptyList(), "emptyList").isValid());
        Assert.assertFalse(validator.validate(Collections.emptySet(), "emptySet").isValid());
        Assert.assertFalse(validator.validate(Collections.emptyMap(), "emptyMap").isValid());
    }

    @Test
    public void validateNumber() {

        Validator validator = BuiltinValidators.number();

        Assert.assertTrue(validator.validate(10, "age").isValid());
        Assert.assertTrue(validator.validate("10", "age").isValid());
        Assert.assertTrue(validator.validate("3.14", "pi").isValid());
        Assert.assertTrue(validator.validate("   3.14   ", "piWithBlank").isValid());

        Assert.assertFalse(validator.validate("", "emptyString").isValid());
        Assert.assertFalse(validator.validate(true, "true").isValid());
        Assert.assertFalse(validator.validate(null, "null").isValid());
    }

    @Test
    public void validatePattern() {

        Validator validator = BuiltinValidators.pattern();

        ValidatorConfig config = configFromMap(Collections.singletonMap("pattern", "^start-.*-end$"));

        Assert.assertTrue(validator.validate("start-1234-end", "value", config).isValid());
        Assert.assertFalse(validator.validate("start___end", "value", config).isValid());
    }

    @Test
    public void validateUri() throws Exception {

        Validator validator = BuiltinValidators.uri();

        Assert.assertTrue(validator.validate("http://localhost:3000/", "baseUrl").isValid());
        Assert.assertTrue(validator.validate("https://localhost:3000/", "baseUrl").isValid());
        Assert.assertTrue(validator.validate("https://localhost:3000/#someFragment", "baseUrl").isValid());

        Assert.assertFalse(validator.validate("file:///somefile.txt", "baseUrl").isValid());
        Assert.assertFalse(validator.validate("invalidUrl++@23", "invalidUri").isValid());

        ValidatorConfig config = configFromMap(ImmutableMap.of(UriValidator.KEY_ALLOW_FRAGMENT, false));
        Assert.assertFalse(validator.validate("https://localhost:3000/#someFragment", "baseUrl", config).isValid());

        // it is also possible to call dedicated validation methods on a built-in validator
        Assert.assertTrue(BuiltinValidators.uri().
                validateUri(new URI("https://customurl"), Collections.singleton("https"), true, true));

        Assert.assertFalse(BuiltinValidators.uri().
                validateUri(new URI("http://customurl"), Collections.singleton("https"), true, true));
    }
}
