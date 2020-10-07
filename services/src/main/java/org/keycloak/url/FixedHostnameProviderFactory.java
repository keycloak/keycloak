package org.keycloak.url;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.urls.HostnameProvider;
import org.keycloak.urls.HostnameProviderFactory;

@Deprecated
public class FixedHostnameProviderFactory implements HostnameProviderFactory {

    private static final Logger LOGGER = Logger.getLogger(RequestHostnameProviderFactory.class);

    private boolean loggedDeprecatedWarning = false;

    private String hostname;
    private int httpPort;
    private int httpsPort;
    private boolean alwaysHttps;

    @Override
    public HostnameProvider create(KeycloakSession session) {
        if (!loggedDeprecatedWarning) {
            loggedDeprecatedWarning = true;
            LOGGER.warn("fixed hostname provider is deprecated, please switch to the default hostname provider");
        }

        return new FixedHostnameProvider(session, alwaysHttps, hostname, httpPort, httpsPort);
    }

    @Override
    public void init(Config.Scope config) {
        this.hostname = config.get("hostname");
        this.httpPort = config.getInt("httpPort", -1);
        this.httpsPort = config.getInt("httpsPort", -1);
        this.alwaysHttps = config.getBoolean("alwaysHttps", false);
    }

    @Override
    public String getId() {
        return "fixed";
    }

}
