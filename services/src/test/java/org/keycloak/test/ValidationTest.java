package org.keycloak.test;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.services.validation.Validation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ValidationTest {

    @Test
    public void testEmailValidation() {
        Assert.assertTrue(Validation.isEmailValid("abc@abc.cz"));
        Assert.assertTrue(Validation.isEmailValid("435@coco.foo.cz"));
        Assert.assertTrue(Validation.isEmailValid("A@something"));
        Assert.assertTrue(Validation.isEmailValid("A@some-thing.foo"));
        Assert.assertTrue(Validation.isEmailValid("1@A"));
        Assert.assertFalse(Validation.isEmailValid("A@some_thing.foo"));
        Assert.assertFalse(Validation.isEmailValid("@some_thing.foo"));
        Assert.assertFalse(Validation.isEmailValid("abc@"));
        Assert.assertFalse(Validation.isEmailValid("abc@."));
        Assert.assertFalse(Validation.isEmailValid("abc@.foo"));
        Assert.assertFalse(Validation.isEmailValid("abc@foo."));
        Assert.assertFalse(Validation.isEmailValid("abc@foo..bar"));
    }
}
