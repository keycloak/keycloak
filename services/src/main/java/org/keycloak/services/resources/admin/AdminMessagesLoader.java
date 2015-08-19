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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Simple loader for message bundles consumed by angular-translate.
 *
 * Note that these bundles are converted to JSON before being shipped to the UI.
 * Also, the content should be formatted such that it can be interpolated by
 * angular-translate.  This is somewhat different from an ordinary Java bundle.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class AdminMessagesLoader {

    private static final Map<String, Properties> allMessages = new HashMap<String, Properties>();

    static Properties getMessages(String locale) throws IOException {
        Properties messages = allMessages.get(locale);
        if (messages != null) return messages;

        return loadMessages(locale);
    }

    private static Properties loadMessages(String locale) throws IOException {
        Properties msgs = new Properties();

        try (InputStream msgStream = getBundleStream(locale)){
            if (msgStream == null) return msgs;
            msgs.load(msgStream);
        }

        allMessages.put(locale, msgs);
        return msgs;
    }

    private static InputStream getBundleStream(String locale) {
        String filename = "admin-messages_" + locale + ".properties";
        return AdminMessagesLoader.class.getResourceAsStream(filename);
    }
}
