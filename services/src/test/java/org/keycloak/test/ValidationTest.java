/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    
    @Test
    public void testIsEmpty(){
        Assert.assertTrue(Validation.isEmpty(null));
        Assert.assertTrue(Validation.isEmpty(""));
        Assert.assertFalse(Validation.isEmpty(" "));
        Assert.assertFalse(Validation.isEmpty("     "));
        Assert.assertFalse(Validation.isEmpty("a"));
        Assert.assertFalse(Validation.isEmpty("    a "));
        Assert.assertFalse(Validation.isEmpty("asgadfgedfs"));
    }
    
    @Test
    public void testIsBlank(){
        Assert.assertTrue(Validation.isBlank(null));
        Assert.assertTrue(Validation.isBlank(""));
        Assert.assertTrue(Validation.isBlank(" "));
        Assert.assertTrue(Validation.isBlank("  \n   "));
        Assert.assertFalse(Validation.isBlank("a"));
        Assert.assertFalse(Validation.isBlank("    a "));
        Assert.assertFalse(Validation.isBlank("asgadfgedfs"));
    }
}
