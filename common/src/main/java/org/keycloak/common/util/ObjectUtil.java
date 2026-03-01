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

package org.keycloak.common.util;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ObjectUtil {

    private ObjectUtil() {}

    /**
     *
     * @param str1
     * @param str2
     * @return true if both strings are null or equal
     */
    public static boolean isEqualOrBothNull(Object str1, Object str2) {
        if (str1 == null && str2 == null) {
            return true;
        }

        if ((str1 != null && str2 == null) || (str1 == null && str2 != null)) {
            return false;
        }

        return str1.equals(str2);
    }


    public static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }


    /**
     * Forked from apache-commons StringUtils
     *
     * <p>Checks if a CharSequence is whitespace, empty ("") or null.</p>
     *
     * <pre>
     * ObjectUtil.isBlank(null)      = true
     * ObjectUtil.isBlank("")        = true
     * ObjectUtil.isBlank(" ")       = true
     * ObjectUtil.isBlank("bob")     = false
     * ObjectUtil.isBlank("  bob  ") = false
     * </pre>
     *
     * @param cs
     * @return {@code true} if the CharSequence is null, empty or whitespace
     */
    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
