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

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
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
    private static final String CONFIG_DIR = System.getProperty("jboss.server.config.dir");
    private static final String BUNDLE_DIR = CONFIG_DIR + "/themes/base/admin/angular-messages/";

    private static final Map<String, Properties> allMessages = new HashMap<String, Properties>();

    static Properties getMessages(String locale) throws IOException {
        Properties messages = allMessages.get(locale);
        if (messages != null) return messages;

        return loadMessages(locale);
    }

    private static Properties loadMessages(String locale) throws IOException {
        Properties masterMsgs = new Properties();

        for (File file : getBundlesForLocale(locale)) {
            try (FileInputStream msgStream = new FileInputStream(file)){
                Properties propsFromFile = new Properties();
                propsFromFile.load(msgStream);
                checkForDups(masterMsgs, propsFromFile, file, locale);
                masterMsgs.putAll(propsFromFile);
            }
        }

        allMessages.put(locale, masterMsgs);
        return masterMsgs;
    }

    private static void checkForDups(Properties masterMsgs, Properties propsFromFile, File file, String locale) {
        for (String prop : propsFromFile.stringPropertyNames()) {
            if (masterMsgs.getProperty(prop) != null) {
                String errorMsg = "Message bundle " + file.getName() + " contains key '" + prop;
                errorMsg += "', which already exists in another bundle for " + locale + " locale.";
                throw new RuntimeException(errorMsg);
            }
        }
    }

    private static File[] getBundlesForLocale(String locale) {
        File bundleDir = new File(BUNDLE_DIR);
        return bundleDir.listFiles(new LocaleFilter(locale));
    }

    private static class LocaleFilter implements FilenameFilter {
        private final String locale;

        public LocaleFilter(String locale) {
            this.locale = locale;
        }

        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith("_" + locale + ".properties");
        }
    }
}
