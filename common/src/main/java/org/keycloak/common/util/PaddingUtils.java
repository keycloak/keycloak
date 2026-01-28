/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.common.util;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PaddingUtils {

    private static final char PADDING_CHAR_NONE = '\u0000';

    /**
     * Applies padding to given string up to specified number of characters. If given string is shorter or same as maxPaddingLength, it will just return the original string.
     * Otherwise it would be padded with "\0" character to have at least "maxPaddingLength" characters
     *
     * @param rawString raw string
     * @param maxPaddingLength max padding length
     * @return padded output
     */
    public static String padding(String rawString, int maxPaddingLength) {
        if (rawString.length() < maxPaddingLength) {
            int nPad = maxPaddingLength - rawString.length();
            StringBuilder result = new StringBuilder(rawString);
            for (int i = 0 ; i < nPad; i++) result.append(PADDING_CHAR_NONE);
            return result.toString();
        } else
            return rawString;
    }
}
