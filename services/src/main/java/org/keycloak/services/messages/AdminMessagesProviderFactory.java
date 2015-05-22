package org.keycloak.services.messages;

import java.util.Locale;
import org.keycloak.Config;
import org.keycloak.messages.MessagesProvider;
import org.keycloak.messages.MessagesProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:leonardo.zanivan@gmail.com">Leonardo Zanivan</a>
 */
public class AdminMessagesProviderFactory implements MessagesProviderFactory {

    @Override
    public MessagesProvider create(KeycloakSession session) {
        return new AdminMessagesProvider(session, Locale.ENGLISH);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "admin";
    }

}
