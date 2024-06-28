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
 * @author pedroigor
 */
public class HtmlUtils {

    /**
     * <p>Escapes the value for a HTML element attribute.</p>
     *
     * @param value
     * @return
     */
    public static String escapeAttribute(String value) {
        StringBuilder escaped = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char chr = value.charAt(i);

            if (chr == '<') {
                escaped.append("&lt;");
            } else if (chr == '>') {
                escaped.append("&gt;");
            } else if (chr == '"') {
                escaped.append("&quot;");
            } else if (chr == '\'') {
                escaped.append("&apos;");
            } else if (chr == '&') {
                escaped.append("&amp;");
            } else {
                escaped.append(chr);
            }
        }

        return escaped.toString();
    }

}
