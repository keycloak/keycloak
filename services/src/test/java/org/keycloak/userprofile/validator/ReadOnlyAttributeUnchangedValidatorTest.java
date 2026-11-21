/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.userprofile.validator;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link ReadOnlyAttributeUnchangedValidator#isUnchanged(String, String)}.
 */
public class ReadOnlyAttributeUnchangedValidatorTest {

    private final ReadOnlyAttributeUnchangedValidator validator = new ReadOnlyAttributeUnchangedValidator();

    @Test
    public void bothNull() {
        Assert.assertTrue("both null should be unchanged", validator.isUnchanged(null, null));
    }

    @Test
    public void existingNullValueBlank() {
        Assert.assertTrue("existing null and blank value should be unchanged", validator.isUnchanged(null, ""));
    }

    @Test
    public void existingNullValueWhitespace() {
        Assert.assertTrue("existing null and whitespace value should be unchanged", validator.isUnchanged(null, "   "));
    }

    @Test
    public void existingNullValueNonBlank() {
        Assert.assertFalse("existing null and non-blank value should be changed", validator.isUnchanged(null, "newValue"));
    }

    @Test
    public void existingValueAndValueNull_omittedAttribute() {
        // This is the bug fix: attribute exists on user but was omitted from the update request
        Assert.assertTrue("existing value with null (omitted) value should be unchanged",
                validator.isUnchanged("existingValue", null));
    }

    @Test
    public void bothEqual() {
        Assert.assertTrue("equal values should be unchanged", validator.isUnchanged("same", "same"));
    }

    @Test
    public void bothDifferent() {
        Assert.assertFalse("different values should be changed", validator.isUnchanged("old", "new"));
    }

    @Test
    public void existingValueAndValueBlank() {
        Assert.assertFalse("existing value with blank value should be changed", validator.isUnchanged("existingValue", ""));
    }
}
