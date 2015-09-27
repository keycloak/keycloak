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
    private static final Map<String, Properties> allMessages = new HashMap<String, Properties>();

    static Properties getMessages(Theme theme, String strLocale) throws IOException {
        String allMessagesKey = theme.getName() + "_" + strLocale;
        Properties messages = allMessages.get(allMessagesKey);
        if (messages != null) return messages;

        Locale locale = Locale.forLanguageTag(strLocale);
        messages = theme.getMessages("admin-messages", locale);
        if (messages == null) return new Properties();

        allMessages.put(allMessagesKey, messages);
        return messages;
    }
}
