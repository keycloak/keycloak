/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
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
package org.keycloak.services.messages;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;
import org.keycloak.models.KeycloakSession;
import org.keycloak.messages.MessagesProvider;
import org.keycloak.services.ServicesLogger;

/**
 * @author <a href="mailto:leonardo.zanivan@gmail.com">Leonardo Zanivan</a>
 */
public class AdminMessagesProvider implements MessagesProvider {

    private static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    private KeycloakSession session;
    private Locale locale;
    private Properties messagesBundle;

    public AdminMessagesProvider(KeycloakSession session, Locale locale) {
        this.session = session;
        this.locale = locale;
        this.messagesBundle = getMessagesBundle(locale);
    }

    @Override
    public String getMessage(String messageKey, Object... parameters) {
        String message = messagesBundle.getProperty(messageKey, messageKey);

        try {
            return new MessageFormat(message, locale).format(parameters);
        } catch (Exception e) {
            logger.failedToFormatMessage(e.getMessage());
            return message;
        }
    }

    @Override
    public void close() {
    }

    private Properties getMessagesBundle(Locale locale) {
        Properties properties = new Properties();

        if (locale == null) {
            return properties;
        }

        URL url = getClass().getClassLoader().getResource(
                "theme/base/admin/messages/messages_" + locale.toString() + ".properties");
        if (url != null) {
            try {
                properties.load(url.openStream());
            } catch (IOException ex) {
                logger.failedToloadMessages(ex);
            }
        }

        return properties;
    }

}
