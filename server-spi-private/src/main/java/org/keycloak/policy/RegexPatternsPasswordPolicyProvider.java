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

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RegexPatternsPasswordPolicyProvider implements PasswordPolicyProvider {

    private static final String ERROR_MESSAGE = "invalidPasswordRegexPatternMessage";

    private KeycloakContext context;

    public RegexPatternsPasswordPolicyProvider(KeycloakContext context) {
        this.context = context;
    }

    @Override
    public PolicyError validate(String username, String password) {
        Pattern pattern = context.getRealm().getPasswordPolicy().getPolicyConfig(RegexPatternsPasswordPolicyProviderFactory.ID);
        Matcher matcher = pattern.matcher(password);
        if (!matcher.matches()) {
            return new PolicyError(ERROR_MESSAGE, pattern.pattern());
        }
        return null;
    }

    @Override
    public PolicyError validate(RealmModel realm, UserModel user, String password) {
        return validate(user.getUsername(), password);
    }

    @Override
    public Object parseConfig(String value) {
        if (value == null) {
            throw new PasswordPolicyConfigException("Config required");
        }
        try {
            return Pattern.compile(value);
        } catch (PatternSyntaxException e) {
            throw new PasswordPolicyConfigException("Not a valid regular expression");
        }
    }

    @Override
    public void close() {
    }

}
