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
package org.keycloak.policy;

import java.util.Locale;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * Password policy that rejects passwords containing the user's first name,
 * last name, or display name (case-insensitive substring match).
 *
 * <p>Name segments shorter than {@value #MIN_NAME_LENGTH} characters are
 * skipped to avoid false positives with very short names (e.g. "Ed").</p>
 */
public class NotContainsNamePasswordPolicyProvider implements PasswordPolicyProvider {

    private static final String ERROR_MESSAGE = "invalidPasswordNotContainsNameMessage";
    private static final String DISPLAY_NAME = "displayName";

    /**
     * Minimum length a name segment must have before it is checked.
     * Prevents short names like "Ed" or "Li" from causing false positives.
     */
    static final int MIN_NAME_LENGTH = 3;

    @Override
    public PolicyError validate(String username, String password) {
        // This two-arg variant is called in contexts where we only have the
        // username (e.g. admin REST calls). We cannot check names here because
        // we have no UserModel. Return null (pass) and rely on the three-arg
        // variant being called during normal registration / profile update flows.
        return null;
    }

    @Override
    public PolicyError validate(RealmModel realm, UserModel user, String password) {
        if (password == null) {
            return null;
        }

        String passwordLower = password.toLowerCase(Locale.ROOT);

        if (containsNamePart(passwordLower, user.getFirstName())) {
            return new PolicyError(ERROR_MESSAGE);
        }
        if (containsNamePart(passwordLower, user.getLastName())) {
            return new PolicyError(ERROR_MESSAGE);
        }

        // Also check the "displayName" user attribute if present
        String displayName = user.getFirstAttribute(DISPLAY_NAME);
        if (containsNamePart(passwordLower, displayName)) {
            return new PolicyError(ERROR_MESSAGE);
        }

        return null;
    }

    /**
     * Returns {@code true} if {@code passwordLower} contains {@code name}
     * (case-insensitive) as a substring, provided {@code name} meets the
     * minimum length requirement.
     *
     * @param passwordLower the password already converted to lower-case
     * @param name          the name attribute value (may be null)
     * @return true if the password contains the name
     */
    private boolean containsNamePart(String passwordLower, String name) {
        if (name == null || name.trim().length() < MIN_NAME_LENGTH) {
            return false;
        }
        return passwordLower.contains(name.trim().toLowerCase(Locale.ROOT));
    }

    @Override
    public Object parseConfig(String value) {
        return null;
    }

    @Override
    public void close() {
    }
}
