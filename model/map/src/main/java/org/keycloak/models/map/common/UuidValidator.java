/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.common;

import java.util.regex.Pattern;

/**
 * Utility class for validating and converting UUIDs.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class UuidValidator {

    protected static final Pattern UUID_REGEX_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$");

    private UuidValidator() {}

    /**
     * Validates that the specified {@code id} is a {@code UUID}.
     *
     * @param id the {@code id} to be validated.
     * @return {@code true} if the {@code id} is a {@code UUID}; {@code false} otherwise.
     */
    public static boolean isValid(final String id) {
        return (id == null) ? false : UUID_REGEX_PATTERN.matcher(id).matches();
    }

    /**
     * Validates that the specified {@code id} is a {@code UUID}. If it is, the {@code id} itself is returned. Otherwise,
     * it is discarded and a new {@code UUID} is created and returned.
     *
     * @param id the {@code id} to be validated.
     * @return the {@code id} itself if it is a valid {@code UUID}, or a new generated {@code UUID}.
     */
    public static String validateAndConvert(final String id) {
        return isValid(id) ? id : StringKeyConverter.UUIDKey.INSTANCE.yieldNewUniqueKey().toString();
    }
}
