package org.keycloak.services.messages;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.keycloak.freemarker.LocaleHelper;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:leonardo.zanivan@gmail.com">Leonardo Zanivan</a>
 */
public class ThemeMessageProvider implements MessageProvider {

    private static final Logger logger = Logger.getLogger(ThemeMessageProvider.class);

    @Override
    public String getMessage(KeycloakSession session, String messageKey, Object... parameters) {
        RealmModel realm = session.getContext().getRealm();

        ThemeProvider themeProvider = session.getProvider(ThemeProvider.class, "extending");
        Theme theme;
        try {
            theme = themeProvider.getTheme(realm.getLoginTheme(), Theme.Type.LOGIN);
        } catch (IOException ex) {
            logger.error("Failed to get theme", ex);
            return messageKey;
        }

        HttpHeaders headers = session.getContext().getRequestHeaders();
        UriInfo uriInfo = session.getContext().getUri();
        
        Locale locale = LocaleHelper.getLocale(realm, null, uriInfo, headers);
        Properties messagesBundle;
        try {
            messagesBundle = theme.getMessages(locale);
        } catch (IOException ex) {
            logger.warn("Failed to load messages", ex);
            return messageKey;
        }

        String rawMessage = messagesBundle.getProperty(messageKey, messageKey);
        return new MessageFormat(rawMessage, locale).format(parameters);
    }

}
