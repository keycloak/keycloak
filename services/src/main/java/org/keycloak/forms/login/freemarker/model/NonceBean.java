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

package org.keycloak.forms.login.freemarker.model;

import org.keycloak.common.util.SecretGenerator;

/**
 * @author <a href="mailto:michal@boska.me">Michal Boska</a>
 * @version $Revision: 1 $
 */
public class NonceBean {

    private static final int NONCE_LENGTH_BYTES = 32;

    private final String value;

    public NonceBean() {
        value = java.util.Base64.getEncoder().encodeToString(SecretGenerator.getInstance().randomBytes(NONCE_LENGTH_BYTES));
    }

    public NonceBean(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("nonce value must not be null/blank");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
