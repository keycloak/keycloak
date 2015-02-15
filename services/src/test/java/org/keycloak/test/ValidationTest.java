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
        Assert.assertTrue(Validation.isEmailValid("A@some-thing.foo"));
        Assert.assertTrue(Validation.isEmailValid("A.b@some-thing.foo"));
        Assert.assertTrue(Validation.isEmailValid("A.b123@some-thing.foo"));
        Assert.assertTrue(Validation.isEmailValid("A.123@some-thing.foo"));
        Assert.assertFalse(Validation.isEmailValid("A@something"));
        Assert.assertFalse(Validation.isEmailValid("1@A"));
        Assert.assertFalse(Validation.isEmailValid("A@some_thing.foo"));
        Assert.assertFalse(Validation.isEmailValid("@some_thing.foo"));
        Assert.assertFalse(Validation.isEmailValid("abc@"));
        Assert.assertFalse(Validation.isEmailValid("abc@."));
        Assert.assertFalse(Validation.isEmailValid("abc@.foo"));
        Assert.assertFalse(Validation.isEmailValid("abc@foo."));
        Assert.assertFalse(Validation.isEmailValid("abc@foo..bar"));
        Assert.assertFalse(Validation.isEmailValid(".@foo.bar"));
        Assert.assertFalse(Validation.isEmailValid(",@foo.bar"));
        Assert.assertFalse(Validation.isEmailValid("!@foo.bar"));
        Assert.assertFalse(Validation.isEmailValid("ok!@foo.bar"));
        Assert.assertFalse(Validation.isEmailValid("ok!a@foo.bar"));
        Assert.assertFalse(Validation.isEmailValid("someone@foo.bar.2s"));
        Assert.assertFalse(Validation.isEmailValid(".someone@foo.bar.us"));
        Assert.assertFalse(Validation.isEmailValid("someone,a@foo.bar.us"));
        Assert.assertFalse(Validation.isEmailValid("someone..a@foo.bar"));
    }
}
