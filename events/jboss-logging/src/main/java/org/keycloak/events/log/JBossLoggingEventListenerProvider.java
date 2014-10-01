package org.keycloak.events.log;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;

import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JBossLoggingEventListenerProvider implements EventListenerProvider {

    private final Logger logger;

    public JBossLoggingEventListenerProvider(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void onEvent(Event event) {
        Logger.Level level = event.getError() != null ? Logger.Level.WARN : Logger.Level.INFO;

        if (logger.isEnabled(level)) {
            StringBuilder sb = new StringBuilder();

            sb.append("type=");
            sb.append(event.getType());
            sb.append(", realmId=");
            sb.append(event.getRealmId());
            sb.append(", clientId=");
            sb.append(event.getClientId());
            sb.append(", userId=");
            sb.append(event.getUserId());
            sb.append(", ipAddress=");
            sb.append(event.getIpAddress());

            if (event.getError() != null) {
                sb.append(", error=");
                sb.append(event.getError());
            }

            if (event.getDetails() != null) {
                for (Map.Entry<String, String> e : event.getDetails().entrySet()) {
                    sb.append(", ");
                    sb.append(e.getKey());
                    if (e.getValue() == null || e.getValue().indexOf(' ') == -1) {
                        sb.append("=");
                        sb.append(e.getValue());
                    } else {
                        sb.append("='");
                        sb.append(e.getValue());
                        sb.append("'");
                    }
                }
            }

            logger.log(level, sb.toString());
        }
    }

    @Override
    public void close() {
    }

}
