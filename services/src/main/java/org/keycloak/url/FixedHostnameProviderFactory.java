package org.keycloak.url;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.urls.HostnameProvider;
import org.keycloak.urls.HostnameProviderFactory;

public class FixedHostnameProviderFactory implements HostnameProviderFactory {

    private String hostname;
    private int httpPort;
    private int httpsPort;

    @Override
    public HostnameProvider create(KeycloakSession session) {
        return new FixedHostnameProvider(session, hostname, httpPort, httpsPort);
    }

    @Override
    public void init(Config.Scope config) {
        this.hostname = config.get("hostname");
        if (this.hostname == null) {
            throw new RuntimeException("hostname not set");
        }

        this.httpPort = config.getInt("httpPort", -1);
        this.httpsPort = config.getInt("httpsPort", -1);
    }

    @Override
    public String getId() {
        return "fixed";
    }

}
