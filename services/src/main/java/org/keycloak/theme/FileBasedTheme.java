/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

public abstract class FileBasedTheme implements Theme {
    abstract protected void loadBundle(String baseBundlename, Locale locale, Properties m) throws IOException;

    @Override
    public Properties getMessages(String baseBundlename, Locale locale) throws IOException {
        if(locale == null){
            return null;
        }
        Properties m = new Properties();

        loadBundle(baseBundlename, locale, m);

        // Chinese locales mapping
        if (locale.getLanguage().equals("zh") && !locale.getCountry().isEmpty()) {
            Locale l = switch (locale.getCountry()) {
                case "TW" -> Locale.forLanguageTag("zh-Hant");
                case "CN" -> Locale.forLanguageTag("zh-Hans");
                default -> null;
            };
            if (l != null) {
                loadBundle(baseBundlename, l, m);
            }
        }

        return m;
    }

    // Logic as implemented by JDK's ResourceBundle
    public String toBundleName(String baseName, Locale locale) {
        if (locale == Locale.ROOT) {
            return baseName;
        }

        String language = locale.getLanguage();
        String script = locale.getScript();
        String country = locale.getCountry();
        String variant = locale.getVariant();

        if (language.isEmpty() && country.isEmpty() && variant.isEmpty()) {
            return baseName;
        }

        StringBuilder sb = new StringBuilder(baseName);
        sb.append('_');
        if (!script.isEmpty()) {
            if (!variant.isEmpty()) {
                sb.append(language).append('_').append(script).append('_').append(country).append('_').append(variant);
            } else if (!country.isEmpty()) {
                sb.append(language).append('_').append(script).append('_').append(country);
            } else {
                sb.append(language).append('_').append(script);
            }
        } else {
            if (!variant.isEmpty()) {
                sb.append(language).append('_').append(country).append('_').append(variant);
            } else if (!country.isEmpty()) {
                sb.append(language).append('_').append(country);
            } else {
                sb.append(language);
            }
        }
        return sb.toString();

    }
}
