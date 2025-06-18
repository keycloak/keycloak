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

import java.util.List;

/**
 * <a href="mailto:external.benjamin.weimer@bosch-si.com">Benjamin Weimer</a>,
 */
public class RegexUtils {
    public static boolean valueMatchesRegex(String regex, Object value) {
        if (value instanceof List) {
            List list = (List) value;
            for (Object val : list) {
                if (valueMatchesRegex(regex, val)) {
                    return true;
                }
            }
        } else {
            if (value != null) {
                String stringValue = value.toString();
                return stringValue != null && stringValue.matches(regex);
            }
        }
        return false;
    }
}
