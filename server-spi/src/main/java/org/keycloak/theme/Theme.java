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

package org.keycloak.theme;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;

import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface Theme {

    String ACCOUNT_RESOURCE_PROVIDER_KEY = "accountResourceProvider";
    String CONTENT_HASH_PATTERN = "contentHashPattern";

    enum Type { LOGIN, ACCOUNT, ADMIN, EMAIL, WELCOME, COMMON };

    String getName();

    String getParentName();

    String getImportName();

    Type getType();

    URL getTemplate(String name) throws IOException;

    InputStream getResourceAsStream(String path) throws IOException;

    /**
     * Same as getMessages(baseBundlename, locale), but uses a default baseBundlename
     * such as "messages".
     *
     * @param locale The locale of the desired message bundle.
     * @return The localized messages from the bundle.
     * @throws IOException If bundle can not be read.
     */
    Properties getMessages(Locale locale) throws IOException;

    /**
     * Retrieve localized messages from a message bundle.
     *
     * @param baseBundlename The base name of the bundle, such as "messages" in
     * messages_en.properties.
     * @param locale The locale of the desired message bundle.
     * @return The localized messages from the bundle.
     * @throws IOException If bundle can not be read.
     */
    Properties getMessages(String baseBundlename, Locale locale) throws IOException;

    /**
     * Retrieve localized messages from a message bundle named "messages" and enhance those messages with messages from
     * realm localization.
     * <p>
     * In general, the translation for the most specific applicable language is used. If a translation exists both in the message bundle and realm localization, the realm localization translation is used.
     * </p>
     *
     * @param realm The realm from which the localization should be retrieved
     * @param locale The locale of the desired message bundle.
     * @return The localized messages from the bundle, enhanced with realm localization
     * @throws IOException If bundle can not be read.
     */
    Properties getEnhancedMessages(RealmModel realm, Locale locale) throws IOException;

    Properties getProperties() throws IOException;

    /**
     * Check if the given path contains a content hash.
     * If a resource is requested from this path, and it has a content hash, this guarantees that if the file
     * exists in two versions of the theme, it will contain the same contents.
     * With this guarantee, a different version of Keycloak can return the same contents even if a caller asks for
     * a different version of Keycloak.
     *
     * @param path path to check for a content hash
     */
    default boolean hasContentHash(String path) throws IOException {
        Object contentHashPattern = getProperties().get(CONTENT_HASH_PATTERN);
        if (contentHashPattern != null) {
            return path.matches(contentHashPattern.toString());
        } else {
            return false;
        }
    }

}
