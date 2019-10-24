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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:jakub.onderka@gmail.com">Jakub Onderrka</a>
 */
public class NotPersonalInformationPasswordPolicyProvider implements PasswordPolicyProvider {

    private static final String ERROR_MESSAGE = "invalidPasswordNotPersonalInformationMessage";

    private KeycloakContext context;

    public NotPersonalInformationPasswordPolicyProvider(KeycloakContext context) {
        this.context = context;
    }

    @Override
    public PolicyError validate(String username, String password) {
        return null;
    }

    @Override
    public PolicyError validate(RealmModel realm, UserModel user, String password) {
        List<String> parts = new Vector<>();
        parts.add(user.getFirstName());
        parts.add(user.getLastName());
        parts.add(user.getUsername());
        parts.add(user.getEmail());
        parts.addAll(Arrays.asList(user.getEmail().split("[\\.@+-]")));

        String lowerCasePassword = password.toLowerCase();
        Optional<String> match = parts.stream()
                .filter(part -> part.length() >= 3)
                .map(String::toLowerCase)
                .filter(lowerCasePassword::contains)
                .findAny();

        return match.isPresent() ? new PolicyError(ERROR_MESSAGE) : null;
    }

    @Override
    public Object parseConfig(String value) {
        return null;
    }

    @Override
    public void close() {
    }

}
