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

package org.keycloak.theme;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import org.jboss.logging.Logger;

/**
 * Util class for localized date and time representation
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class DateTimeFormatterUtil {
    private static final Logger log = Logger.getLogger(DateTimeFormatterUtil.class);

    public static String getDateTimeFromMillis(long millis) {
        return getDateTimeFromMillis(millis, Locale.ENGLISH);
    }

    public static String getDateTimeFromMillis(long millis, String locale) {
        return getDateTimeFromMillis(millis, getLocaleFromString(locale));
    }

    public static String getDateTimeFromMillis(long millis, Locale locale) {
        return getDateTimeFromMillis(millis, getDefaultDateFormat(locale));
    }

    /**
     * Get string representation of localized date and time
     *
     * @param millis    number of milliseconds passed since January 1, 1970, 00:00:00 GMT
     * @param dateFormat format of date and time. See {@link DateFormat}
     * @return string representation
     */
    public static String getDateTimeFromMillis(long millis, DateFormat dateFormat) {
        if (dateFormat == null) return null;
        return dateFormat.format(new Date(millis));
    }

    public static Locale getLocaleFromString(String locale) {
        return getLocaleFromString(locale, Locale.ENGLISH);
    }

    /**
     * Parse {@link Locale} from string
     *
     * @param locale       required locale
     * @param defaultValue default value if the locale parameter is invalid
     * @return Locale
     */
    public static Locale getLocaleFromString(String locale, Locale defaultValue) {
        try {
            return Optional.ofNullable(locale)
                    .map(Locale::new)
                    .orElse(defaultValue);
        } catch (Exception e) {
            log.debugf("Invalid locale '%s'", locale);
        }
        return defaultValue;
    }

    public static DateFormat getDefaultDateFormat() {
        return getDefaultDateFormat(Locale.ENGLISH);
    }

    public static DateFormat getDefaultDateFormat(Locale locale) {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale);
    }
}
