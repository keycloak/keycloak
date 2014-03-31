package org.keycloak.audit.log;

import org.jboss.logging.Logger;
import org.keycloak.audit.AuditListener;
import org.keycloak.audit.Event;

import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JBossLoggingAuditListener implements AuditListener {

    private static final Logger logger = Logger.getLogger("org.keycloak.audit");

    @Override
    public String getId() {
        return "jboss-logging";
    }

    @Override
    public void onEvent(Event event) {
        Logger.Level level = event.isError() ? Logger.Level.WARN : Logger.Level.INFO;

        if (logger.isEnabled(level)) {
            StringBuilder sb = new StringBuilder();

            sb.append("event=");
            sb.append(event.getEvent());
            sb.append(", realmId=");
            sb.append(event.getRealmId());
            sb.append(", clientId=");
            sb.append(event.getClientId());
            sb.append(", userId=");
            sb.append(event.getUserId());
            sb.append(", ipAddress=");
            sb.append(event.getIpAddress());

            if (event.isError()) {
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

}
