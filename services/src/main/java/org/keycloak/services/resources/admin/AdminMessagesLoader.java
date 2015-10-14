/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.services.resources.admin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.jboss.logging.Logger;
import org.keycloak.freemarker.Theme;

/**
 * Simple loader and cache for message bundles consumed by angular-translate.
 *
 * Note that these bundles are converted to JSON before being shipped to the UI.
 * Also, the content should be formatted such that it can be interpolated by
 * angular-translate.  This is somewhat different from an ordinary Java bundle.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class AdminMessagesLoader {
    protected static final Logger logger = Logger.getLogger(AdminConsole.class);

    //                         theme       locale   bundle
    protected static final Map<String, Map<String, Properties>> allMessages = new HashMap<String, Map<String, Properties>>();

    static Properties getMessages(Theme theme, String strLocale) throws IOException {
        String themeName = theme.getName();
        Map bundlesForTheme = allMessages.get(themeName);
        if (bundlesForTheme == null) {
            bundlesForTheme = new HashMap<String, Properties>();
            allMessages.put(themeName, bundlesForTheme);
        }

        return findMessagesForTheme(theme, strLocale, bundlesForTheme);
    }


    private static Properties findMessagesForTheme(Theme theme,
                                                   String strLocale,
                                                   Map<String, Properties> bundlesForTheme) throws IOException {
        Properties messages = bundlesForTheme.get(strLocale);
        if (messages != null) return messages; // use cached bundle

        // load bundle from theme
        Locale locale = Locale.forLanguageTag(strLocale);
        messages = theme.getMessages("admin-messages", locale);

        String themeName = theme.getName();
        if (messages == null) throw new NullPointerException(themeName + ": Unable to find admin-messages bundle for locale=" + strLocale);

        if (!bundlesForTheme.isEmpty()) {
            // use first bundle as the standard
            String standardLocale = bundlesForTheme.keySet().iterator().next();
            Properties standardBundle = bundlesForTheme.get(standardLocale);
            validateMessages(themeName, standardBundle, standardLocale, messages, strLocale);
        }

        bundlesForTheme.put(strLocale, messages);
        return messages;
    }

    private static void validateMessages(String themeName, Properties standardBundle, String standardLocale, Properties messages, String strLocale) {
        if (standardBundle.keySet().containsAll(messages.keySet()) &&
            (messages.keySet().containsAll(standardBundle.keySet()))) {
            return; // it all checks out
        }

        // otherwise, find the offending keys
        int warnCount = 0;
        for (Object key : standardBundle.keySet()) {
            if (!messages.containsKey(key)) {
                logger.error(themeName + " theme: Key '" + key + "' not found in admin-messages bundle for locale=" + strLocale +
                             ". However, this key exists in previously loaded bundle for locale=" + standardLocale);
                warnCount++;
            }

            if (warnCount > 4) return; // There could be lots of these.  Don't fill up the log.
        }

        for (Object key : messages.keySet()) {
            if (!standardBundle.containsKey(key)) {
                logger.error(themeName + " theme: Key '" + key + "' was found in admin-messages bundle for locale=" + strLocale +
                             ". However, this key does not exist in previously loaded bundle for locale=" + standardLocale);
                warnCount++;
            }

            if (warnCount > 4) return; // There could be lots of these.  Don't fill up the log.
        }
    }
}
