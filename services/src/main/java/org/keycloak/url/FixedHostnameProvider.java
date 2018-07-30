package org.keycloak.url;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.urls.HostnameProvider;

import javax.ws.rs.core.UriInfo;

public class FixedHostnameProvider implements HostnameProvider {

    private final KeycloakSession session;
    private final String globalHostname;
    private final int httpPort;
    private final int httpsPort;

    public FixedHostnameProvider(KeycloakSession session, String globalHostname, int httpPort, int httpsPort) {
        this.session = session;
        this.globalHostname = globalHostname;
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
    }

    @Override
    public String getHostname(UriInfo originalUriInfo) {
        RealmModel realm = session.getContext().getRealm();
        if (realm != null) {
            String realmHostname = session.getContext().getRealm().getAttribute("hostname");
            if (realmHostname != null && !realmHostname.isEmpty()) {
                return realmHostname;
            }
        }
        return this.globalHostname;
    }

    @Override
    public int getPort(UriInfo originalUriInfo) {
        return originalUriInfo.getRequestUri().getScheme().equals("https") ? httpsPort : httpPort;
    }

}
