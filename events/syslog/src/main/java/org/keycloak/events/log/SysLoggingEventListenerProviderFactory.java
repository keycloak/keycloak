package org.keycloak.events.log;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.productivity.java.syslog4j.Syslog;
import org.productivity.java.syslog4j.SyslogIF;

/**
 * @author <a href="mailto:giriraj.sharma27@gmail.com">Giriraj Sharma</a>
 */
public class SysLoggingEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String ID = "syslog";

    private SyslogIF syslogger;
    private String protocol;
    private String host;
    private int port;
    
    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new SysLoggingEventListenerProvider(syslogger);
    }

    @Override
    public void init(Config.Scope config) {
        protocol = config.get("protocol");
        host = config.get("host");
        port = config.getInt("port");
        
        syslogger = Syslog.getInstance(protocol);
        syslogger.getConfig().setHost(host);
        syslogger.getConfig().setPort(port);
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

}
