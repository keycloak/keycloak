/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.BadRequestException;

import org.jboss.logging.Logger;

/**
 *
 * @author Stan Silvert
 * @author Lukas Hanusovsky lhanusov@redhat.com
 */
public class ReservedCharValidator {
    protected static final Logger logger = Logger.getLogger(ReservedCharValidator.class);

    // https://tools.ietf.org/html/rfc3986#section-2.2
    private static final Pattern RESERVED_CHARS_PATTERN = Pattern.compile("[:/?#@!$&()*+,;=\\[\\]\\\\]");

    // KEYCLOAK-14231 - Supported Locales: Three new characters were added on top of this RFC: "{", "}", "%"
    private static final Pattern RESERVED_CHARS_LOCALES_PATTERN = Pattern.compile("[:/?#@!$&()*+,;=\\[\\]\\\\{}%]");

    private ReservedCharValidator() {}

    public static void validate(String str, Pattern pattern) {
        validate(str, pattern, null);
    }

    public static void validate(String str, Pattern pattern, String message) throws ReservedCharException {
        if (str == null) return;

        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            if(message == null) {
                message = "Character '" + matcher.group() + "' not allowed.";
            }
            logger.warn(message);
            throw new ReservedCharException(message);
        }
    }

    public static void validateNoSpace(String str) {
        validate(str, Pattern.compile("\\s"), "Empty Space not allowed.");
        validate(str, RESERVED_CHARS_PATTERN);
    }

    public static void validate(String str) {
        validate(str, RESERVED_CHARS_PATTERN);
    }

    public static void validateLocales(Iterable<String> strIterable) {
        if (strIterable == null) return;

        for (String str: strIterable) {
            validate(str, RESERVED_CHARS_LOCALES_PATTERN);
        }
    }

    public static void validateSecurityHeaders(Map<String, String> headers) {
        if (headers == null) return;

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            validate(entry.getKey(), Pattern.compile("\\n"), "Newline not allowed.");
            validate(entry.getValue(), Pattern.compile("\\n"), "Newline not allowed.");
        }
    }

    public static class ReservedCharException extends BadRequestException {
        ReservedCharException(String msg) {
            super(msg);
        }
    }
}
