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

package org.keycloak.policy;

import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RegexPatterns extends BasePasswordPolicy {
    private static final String NAME = "regexPattern";
    private static final String INVALID_PASSWORD_REGEX_PATTERN = "invalidPasswordRegexPatternMessage";
    private String regexPattern;

    public RegexPatterns(String arg) {
        regexPattern = arg;
    }

    @Override
    public Error validate(KeycloakSession session, String username, String password) {
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(password);
        if (!matcher.matches()) {
            return new Error(INVALID_PASSWORD_REGEX_PATTERN, (Object) regexPattern);
        }
        return null;
    }

    @Override
    public Error validate(KeycloakSession session, UserModel user, String password) {
        return validate(session, user.getUsername(), password);
    }

    @Override
    public String description() {
        return null;
    }

    @Override
    public PasswordPolicyProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public String getId() {
        return NAME;
    }
}
