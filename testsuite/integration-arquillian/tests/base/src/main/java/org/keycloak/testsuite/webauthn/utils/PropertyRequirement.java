/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.webauthn.utils;

import java.util.Arrays;

import org.keycloak.models.Constants;

public enum PropertyRequirement {
    NOT_SPECIFIED(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED),
    YES(Constants.WEBAUTHN_POLICY_OPTION_YES),
    NO(Constants.WEBAUTHN_POLICY_OPTION_NO);

    private final String value;

    PropertyRequirement(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PropertyRequirement fromValue(String value) {
        return Arrays.stream(PropertyRequirement.values())
                .filter(f -> f.getValue().equals(value))
                .findFirst()
                .orElse(NOT_SPECIFIED);
    }
}
