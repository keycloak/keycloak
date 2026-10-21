/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models;

import org.keycloak.common.util.SecretGenerator;

/**
 * The default implementation for generating/formatting user code of OAuth 2.0 Device Authorization Grant.
 * For generation, uppercase eight-letter format is used.
 * For display, uppercase four-letters dashes four-letters format is used.
 *
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 */
public class DefaultOAuth2DeviceUserCodeProvider implements OAuth2DeviceUserCodeProvider {

    private static final int LENGTH = 8;
    private static final String DELIMITER = "-";

    @Override
    public String generate() {
        // For case-insensitive, use uppercase
        return SecretGenerator.getInstance().randomString(LENGTH, SecretGenerator.UPPER);
    }

    @Override
    public String display(String userCode) {
        return new StringBuilder(userCode).insert(4, DELIMITER).toString();
    }

    @Override
    public String format(String userCode) {
        return String.join("", userCode.split(DELIMITER)).toUpperCase();
    }

    @Override
    public void close() {

    }
}
