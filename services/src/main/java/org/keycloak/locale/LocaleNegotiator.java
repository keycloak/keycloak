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
package org.keycloak.locale;

import java.util.Locale;
import java.util.Set;

public class LocaleNegotiator {
    private Set<String> supportedLocales;

    public LocaleNegotiator(Set<String> supportedLocales) {
        this.supportedLocales = supportedLocales;
    }

    public LocaleSelection invoke(String... localeStrings) {
        for (String localeString : localeStrings) {
            if (localeString != null) {
                Locale result = null;
                Locale search = Locale.forLanguageTag(localeString);
                for (String languageTag : supportedLocales) {
                    Locale locale = Locale.forLanguageTag(languageTag);
                    if (locale.getLanguage().equals(search.getLanguage())) {
                        if (search.getCountry().equals("") ^ locale.getCountry().equals("") && result == null) {
                            result = locale;
                        }
                        if (locale.getCountry().equals(search.getCountry())) {
                            return new LocaleSelection(localeString, locale);
                        }
                    }
                }
                if (result != null) {
                    return new LocaleSelection(localeString, result);
                }
            }
        }
        return null;
    }

}
