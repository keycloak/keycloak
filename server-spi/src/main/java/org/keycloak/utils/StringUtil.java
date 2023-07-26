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

import java.util.Collection;

public class StringUtil {

    public static boolean isBlank(String str) {
        return !(isNotBlank(str));
    }

    public static boolean  isNotBlank(String str) {
        return str != null && !"".equals(str.trim());
    }

    /**
     * Calling:
     * <pre>joinValuesWithLogicalCondition("or", Arrays.asList("foo", "bar", "baz", "caz" ))</pre>
     * will return "foo, bar, baz or caz"
     *
     * @param conditionText condition
     * @param values values to be joined with the condition at the end
     * @return see the example above
     */
    public static String joinValuesWithLogicalCondition(String conditionText, Collection<String> values) {
        StringBuilder options = new StringBuilder();
        int i = 1;
        for (String o : values) {
            if (i == values.size()) {
                options.append(" " + conditionText + " ");
            } else if (i > 1) {
                options.append(", ");
            }
            options.append(o);
            i++;
        }
        return options.toString();
    }

}