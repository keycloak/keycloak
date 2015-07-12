package org.keycloak.services.messages;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.messages.MessagesProvider;

/**
 * @author <a href="mailto:leonardo.zanivan@gmail.com">Leonardo Zanivan</a>
 */
public class AdminMessagesProvider implements MessagesProvider {

    private static final Logger logger = Logger.getLogger(AdminMessagesProvider.class);

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
            logger.warnf("Failed to format message due to: %s", e.getMessage());
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
                logger.warn("Failed to load messages", ex);
            }
        }

        return properties;
    }

}
