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

package org.keycloak.admin.client.token;

import java.util.Objects;

import org.keycloak.OAuth2Constants;

public class ClientAssertion {

    private final String type;
    private final String value;

    public static ClientAssertion jwt(String jwt) {
        return new ClientAssertion(OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT, jwt);
    }

    public ClientAssertion(String type, String value) {
        this.type = Objects.requireNonNull(type, "type");
        this.value = Objects.requireNonNull(value, "value");
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
