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
package org.keycloak.services.resources.admin;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.function.BiFunction;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.theme.Theme;
import org.keycloak.utils.StringUtil;

/**
 * Message formatter for Admin GUI/API messages. 
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class AdminMessageFormatter implements BiFunction<String, Object[], String> {

    private final Locale locale;
    private final Properties messages;

    /**
     * @param session to get context (including current Realm) from
     * @param user to resolve locale for
     */
    public AdminMessageFormatter(KeycloakSession session, UserModel user) {
        try {
            KeycloakContext context = session.getContext();
            locale = context.resolveLocale(user);
            messages = new Properties();
            messages.putAll(getTheme(session).getMessages(locale));
            RealmModel realm = context.getRealm();
            if(StringUtil.isNotBlank(realm.getDefaultLocale())) {
                messages.putAll(realm.getRealmLocalizationTextsByLocale(realm.getDefaultLocale()));
            }
            messages.putAll(realm.getRealmLocalizationTextsByLocale(locale.toLanguageTag()));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to configure error messages", cause);
        }
    }

    private Theme getTheme(KeycloakSession session) throws IOException {
        return session.theme().getTheme(Theme.Type.ADMIN);
    }

    @Override
    public String apply(String s, Object[] objects) {
        return new MessageFormat(messages.getProperty(s, s), locale).format(objects);
    }
}