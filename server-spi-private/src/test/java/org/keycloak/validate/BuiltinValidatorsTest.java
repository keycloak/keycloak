package org.keycloak.validate;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.validate.builtin.UriValidator;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import static org.keycloak.validate.ValidatorConfig.configFromMap;

public class BuiltinValidatorsTest {

    @Test
    public void validateLength() {

        Validator validator = Validators.length();

        ValidatorConfig config1 = configFromMap(ImmutableMap.of("min", 1));
        Assert.assertTrue(validator.validate("tester", "name", config1).isValid());

        ValidatorConfig config2 = configFromMap(ImmutableMap.of("min", 7));
        Assert.assertFalse(validator.validate("tester", "name", config2).isValid());
    }

    @Test
    public void validateEmail() {

        Validator validator = Validators.email();

        Assert.assertTrue(validator.validate("admin@example.org", "email")
                .isValid());
        Assert.assertFalse(validator.validate("adminATexample.org", "email")
                .isValid());
    }

    @Test
    public void validateNotBlank() {

        Validator validator = Validators.notBlank();

        Assert.assertTrue(validator.validate("tester", "username").isValid());
        Assert.assertFalse(validator.validate("", "username").isValid());
        Assert.assertFalse(validator.validate("   ", "username").isValid());
        Assert.assertFalse(validator.validate(null, "username").isValid());
    }

    @Test
    public void validateNotEmpty() {

        Validator validator = Validators.notEmpty();

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

        Validator validator = Validators.number();

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

        Validator validator = Validators.pattern();

        ValidatorConfig config = configFromMap(Collections.singletonMap("pattern", "^start-.*-end$"));

        Assert.assertTrue(validator.validate("start-1234-end", "value", config).isValid());
        Assert.assertFalse(validator.validate("start___end", "value", config).isValid());
    }

    @Test
    public void validateUri() throws Exception {

        Validator validator = Validators.uri();

        Assert.assertTrue(validator.validate("http://localhost:3000/", "baseUrl").isValid());
        Assert.assertTrue(validator.validate("https://localhost:3000/", "baseUrl").isValid());
        Assert.assertTrue(validator.validate("https://localhost:3000/#someFragment", "baseUrl").isValid());

        Assert.assertFalse(validator.validate("file:///somefile.txt", "baseUrl").isValid());
        Assert.assertFalse(validator.validate("invalidUrl++@23", "invalidUri").isValid());

        ValidatorConfig config = configFromMap(ImmutableMap.of(UriValidator.KEY_ALLOW_FRAGMENT, false));
        Assert.assertFalse(validator.validate("https://localhost:3000/#someFragment", "baseUrl", config).isValid());

        // it is also possible to call dedicated validation methods on a built-in validator
        Assert.assertTrue(Validators.uri().
                validateUri(new URI("https://customurl"), Collections.singleton("https"), true, true));

        Assert.assertFalse(Validators.uri().
                validateUri(new URI("http://customurl"), Collections.singleton("https"), true, true));
    }
}
