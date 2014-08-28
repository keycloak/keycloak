package org.keycloak.events.log;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JBossLoggingEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String ID = "jboss-logging";

    private static final Logger logger = Logger.getLogger("org.keycloak.events");

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new JBossLoggingEventListenerProvider(logger);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

}
