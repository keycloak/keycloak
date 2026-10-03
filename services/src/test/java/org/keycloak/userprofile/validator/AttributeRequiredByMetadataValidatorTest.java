/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.userprofile.AttributeContext;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.UserProfileAttributeValidationContext;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileMetadata;
import org.keycloak.userprofile.UserProfileUtil;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidatorConfig;

import org.junit.Assert;
import org.junit.Test;

/**
 * @see AttributeRequiredByMetadataValidator
 */
public class AttributeRequiredByMetadataValidatorTest {

    private static final AttributeRequiredByMetadataValidator VALIDATOR = new AttributeRequiredByMetadataValidator();

    @Test
    public void requiredAndEditable_missingValue_producesError() {
        // Baseline: an editable required attribute with no value always fails, regardless of the read-only bypass
        // mechanism - it is never consulted here since the attribute isn't read-only in the first place.
        Assert.assertFalse(validate(true, false, true).isValid());
    }

    @Test
    public void requiredAndReadOnly_bypassAllowedByDefault_skipsValidation() {
        // Legacy behavior, unchanged by the new bypass mechanism: a read-only required attribute with no value is
        // still forgiven when nothing has explicitly disabled the bypass (the default for every attribute that
        // doesn't opt into stricter validation).
        Assert.assertTrue(validate(true, true, true).isValid());
    }

    @Test
    public void requiredAndReadOnly_bypassDisabled_producesError() {
        // Once a decorator disables the bypass for this attribute (e.g. because its value is established from an
        // external, untrustworthy source), a missing required value must actually fail - not be silently skipped
        // just because the attribute happens to be read-only.
        Assert.assertFalse(validate(true, true, false).isValid());
    }

    @Test
    public void notRequired_readOnlyWithBypassDisabled_stillSkipsValidation() {
        // The bypass override only matters once an attribute is already required; it must never turn an
        // attribute that isn't required into one that is.
        Assert.assertTrue(validate(false, true, false).isValid());
    }

    private static ValidationContext validate(boolean required, boolean readOnly, boolean readOnlyBypassAllowed) {
        // USER_API is an admin context, satisfying the write condition createAttributeMetadata() bakes in by
        // default (ONLY_ADMIN_CONDITION) - so the attribute starts out writable, and "readOnly" here is controlled
        // purely by the extra condition added below, not by an unrelated context/role mismatch.
        UserProfileContext context = UserProfileContext.USER_API;
        UserProfileMetadata profileMetadata = new UserProfileMetadata(context);
        AttributeMetadata attributeMetadata = UserProfileUtil.createAttributeMetadata("department", profileMetadata, 0, "test");

        attributeMetadata.setRequired(required ? AttributeMetadata.ALWAYS_TRUE : AttributeMetadata.ALWAYS_FALSE);
        if (readOnly) {
            attributeMetadata.addWriteCondition(AttributeMetadata.ALWAYS_FALSE);
        }
        if (!readOnlyBypassAllowed) {
            attributeMetadata.addReadOnlyBypassCondition(AttributeMetadata.ALWAYS_FALSE);
        }

        Map.Entry<String, List<String>> attribute = new SimpleImmutableEntry<>("department", Collections.emptyList());
        AttributeContext attributeContext = new AttributeContext(context, null, attribute, null, attributeMetadata, null);
        ValidationContext validationContext = new UserProfileAttributeValidationContext(attributeContext);

        return VALIDATOR.validate(Collections.emptyList(), "department", validationContext, ValidatorConfig.EMPTY);
    }
}
