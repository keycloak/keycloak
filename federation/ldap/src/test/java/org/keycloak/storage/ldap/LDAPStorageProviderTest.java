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

package org.keycloak.storage.ldap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.keycloak.representations.idm.AbstractUserRepresentation;
import org.keycloak.userprofile.AttributeChangeListener;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.Attributes;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.ValidationException;
import org.keycloak.validate.ValidationError;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @see LDAPStorageProvider#canBeFixedByUser(UserProfile, ValidationException)
 */
public class LDAPStorageProviderTest {

    @Test
    public void canBeFixedByUser_allErrorsOnEditableAttributes_returnsTrue() {
        UserProfile profile = profileWithReadOnlyAttributes();
        ValidationException e = validationExceptionFor(
                new ValidationError("person-name-prohibited-characters", "firstName", "error-person-name-invalid-character"));

        Assertions.assertTrue(LDAPStorageProvider.canBeFixedByUser(profile, e));
    }

    @Test
    public void canBeFixedByUser_errorOnReadOnlyAttribute_returnsFalse() {
        UserProfile profile = profileWithReadOnlyAttributes("username");
        ValidationException e = validationExceptionFor(
                new ValidationError("username-prohibited-characters", "username", "error-username-invalid-character"));

        Assertions.assertFalse(LDAPStorageProvider.canBeFixedByUser(profile, e));
    }

    @Test
    public void canBeFixedByUser_oneOfSeveralErrorsOnReadOnlyAttribute_returnsFalse() {
        UserProfile profile = profileWithReadOnlyAttributes("username");
        ValidationException e = validationExceptionFor(
                new ValidationError("person-name-prohibited-characters", "firstName", "error-person-name-invalid-character"),
                new ValidationError("username-prohibited-characters", "username", "error-username-invalid-character"));

        Assertions.assertFalse(LDAPStorageProvider.canBeFixedByUser(profile, e));
    }

    // A validator can report an error with no attribute of its own (e.g. a cross-attribute check). There is
    // nothing in the user's own profile form for them to edit to fix that, so it must never be treated as
    // fixable just because none of the errors happen to be tied to a read-only attribute.
    @Test
    public void canBeFixedByUser_errorWithNoAttribute_returnsFalse() {
        UserProfile profile = profileWithReadOnlyAttributes();
        ValidationException e = validationExceptionFor(
                new ValidationError("some-cross-attribute-validator", null, "error-global"));

        Assertions.assertFalse(LDAPStorageProvider.canBeFixedByUser(profile, e));
    }

    @Test
    public void canBeFixedByUser_attributeErrorAndGlobalError_returnsFalse() {
        UserProfile profile = profileWithReadOnlyAttributes();
        ValidationException e = validationExceptionFor(
                new ValidationError("person-name-prohibited-characters", "firstName", "error-person-name-invalid-character"),
                new ValidationError("some-cross-attribute-validator", null, "error-global"));

        Assertions.assertFalse(LDAPStorageProvider.canBeFixedByUser(profile, e));
    }

    @Test
    public void describeErrors_doesNotIncludeRawMessageParameters() {
        // Mirrors what EmailValidator actually does: pass the submitted (invalid) value as a message parameter.
        ValidationException e = validationExceptionFor(
                new ValidationError("email", "email", "error-invalid-email", "someone@sensitive-domain.example"));

        String description = LDAPStorageProvider.describeErrors(e);

        Assertions.assertFalse(description.contains("someone@sensitive-domain.example"),
                "The submitted value must not end up in the log message");
        Assertions.assertTrue(description.contains("email") && description.contains("error-invalid-email"),
                "The attribute name and message key should still be present for diagnostics");
    }

    @Test
    public void describeErrors_multipleErrors_areAllIncluded() {
        ValidationException e = validationExceptionFor(
                new ValidationError("person-name-prohibited-characters", "firstName", "error-person-name-invalid-character"),
                new ValidationError("username-prohibited-characters", "username", "error-username-invalid-character"));

        String description = LDAPStorageProvider.describeErrors(e);

        Assertions.assertTrue(description.contains("firstName") && description.contains("error-person-name-invalid-character"));
        Assertions.assertTrue(description.contains("username") && description.contains("error-username-invalid-character"));
    }

    private static ValidationException validationExceptionFor(ValidationError... errors) {
        ValidationException.ValidationExceptionBuilder builder = new ValidationException.ValidationExceptionBuilder();
        for (ValidationError error : errors) {
            builder.accept(error);
        }
        return builder.build();
    }

    private static UserProfile profileWithReadOnlyAttributes(String... readOnlyAttributes) {
        Set<String> readOnly = Set.of(readOnlyAttributes);
        Attributes attributes = new Attributes() {
            @Override
            public boolean isReadOnly(String name) {
                return readOnly.contains(name);
            }

            @Override
            public List<String> get(String name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean validate(String name, Consumer<ValidationError>... listeners) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean contains(String name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<String> nameSet() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Map<String, List<String>> getWritable() {
                throw new UnsupportedOperationException();
            }

            @Override
            public AttributeMetadata getMetadata(String name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isRequired(String name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Map<String, List<String>> getReadable() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Map<String, List<String>> toMap() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Map<String, List<String>> getUnmanagedAttributes() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isDefaultAttribute(String name) {
                throw new UnsupportedOperationException();
            }
        };

        return new UserProfile() {
            @Override
            public Attributes getAttributes() {
                return attributes;
            }

            @Override
            public void validate() {
                throw new UnsupportedOperationException();
            }

            @Override
            public org.keycloak.models.UserModel create() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void update(boolean removeAttributes, AttributeChangeListener... changeListener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <R extends AbstractUserRepresentation> R toRepresentation(boolean full) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
