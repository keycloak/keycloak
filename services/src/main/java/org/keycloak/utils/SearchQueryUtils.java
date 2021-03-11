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

package org.keycloak.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class SearchQueryUtils {
    public static final Pattern queryPattern = Pattern.compile("\\s*(?:(?<name>[^\"][^: ]+)|\"(?<nameEsc>(?:\\\\.|[^\\\\\"])+)\"):(?:(?<value>[^\"][^ ]*)|\"(?<valueEsc>(?:\\\\.|[^\\\\\"])+)\")\\s*");
    public static final Pattern escapedCharsPattern = Pattern.compile("\\\\(.)");

    public static Map<String, String> getFields(final String query) {
        Matcher matcher = queryPattern.matcher(query);
        Map<String, String> ret = new HashMap<>();
        while (matcher.find()) {
            String name = matcher.group("name");
            if (name == null) {
                name = unescape(matcher.group("nameEsc"));
            }

            String value = matcher.group("value");
            if (value == null) {
                value = unescape(matcher.group("valueEsc"));
            }

            ret.put(name, value);
        }
        return ret;
    }

    public static String unescape(final String escaped) {
        return escapedCharsPattern.matcher(escaped).replaceAll("$1");
    }
}
