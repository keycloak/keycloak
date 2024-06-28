/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.theme;

import java.util.Properties;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class TemplatingUtil {

    public static String resolveVariables(String text, Properties props) {
        return resolveVariables(text, props, "${", "}");
    }

    public static String resolveVariables(String text, Properties props, String startMarker, String endMarker) {

        int e = 0;
        int s = text.indexOf(startMarker);
        if (s == -1) {
            return text;
        } else {
            StringBuilder sb = new StringBuilder();

            do {
                if (e < s) {
                    sb.append(text.substring(e, s));
                }
                e = text.indexOf(endMarker, s + startMarker.length());
                if (e != -1) {
                    String key = text.substring(s + startMarker.length(), e);
                    sb.append(props.getProperty(key, key));
                    e += endMarker.length();
                    s = text.indexOf(startMarker, e);
                } else {
                    e = s;
                    break;
                }
            } while (s != -1);

            if (e < text.length()) {
                sb.append(text.substring(e));
            }
            return sb.toString();
        }
    }
}
