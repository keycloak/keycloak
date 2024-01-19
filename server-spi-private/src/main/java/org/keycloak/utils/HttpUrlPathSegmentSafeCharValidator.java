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

import jakarta.ws.rs.BadRequestException;
import org.jboss.logging.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Philip Sanetra code@psanetra.de
 */
public class HttpUrlPathSegmentSafeCharValidator {
    protected static final Logger logger = Logger.getLogger(HttpUrlPathSegmentSafeCharValidator.class);

    // https://tools.ietf.org/html/rfc1738#section-5
    //   httpurl        = "http://" hostport [ "/" hpath [ "?" search ]]
    //   hpath          = hsegment *[ "/" hsegment ]
    //   hsegment       = *[ uchar | ";" | ":" | "@" | "&" | "=" ]
    //   uchar          = unreserved | escape
    //   unreserved     = alpha | digit | safe | extra
    //   safe           = "$" | "-" | "_" | "." | "+"
    //   extra          = "!" | "*" | "'" | "(" | ")" | ","
    // We allow unreserved, but also exclude "extra", "$" and "+" to also avoid characters, which are disallowed by previous ReservedCharsValidator implementation
    private static final Pattern UNSAFE_CHARS_PATTERN = Pattern.compile("[^a-zA-Z0-9-_.]");

    private HttpUrlPathSegmentSafeCharValidator() {}

    private static void validate(String str, Pattern pattern) throws UrlSafeCharException {
        if (str == null) return;

        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            String message = "Character '" + matcher.group() + "' not allowed.";
            logger.warn(message);
            throw new UrlSafeCharException(message);
        }
    }

    public static void validate(String str) {
        validate(str, UNSAFE_CHARS_PATTERN);
    }

    public static void validate(Iterable<String> strIterable) {
        if (strIterable == null) return;

        for (String str: strIterable) {
            validate(str);
        }
    }

    public static class UrlSafeCharException extends BadRequestException {
        UrlSafeCharException(String msg) {
            super(msg);
        }
    }
}
