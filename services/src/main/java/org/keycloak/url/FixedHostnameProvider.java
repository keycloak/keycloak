package org.keycloak.url;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.urls.HostnameProvider;

import javax.ws.rs.core.UriInfo;

@Deprecated
public class FixedHostnameProvider implements HostnameProvider {

    private final KeycloakSession session;
    private final String globalHostname;
    private final boolean alwaysHttps;
    private final int httpPort;
    private final int httpsPort;

    public FixedHostnameProvider(KeycloakSession session, boolean alwaysHttps, String globalHostname, int httpPort, int httpsPort) {
        this.session = session;
        this.alwaysHttps = alwaysHttps;
        this.globalHostname = globalHostname;
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
    }

    @Override
    public String getScheme(UriInfo originalUriInfo) {
        return alwaysHttps ? "https" : originalUriInfo.getRequestUri().getScheme();
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
        boolean https = originalUriInfo.getRequestUri().getScheme().equals("https");
        if (https) {
            if (httpsPort == -1) {
                return originalUriInfo.getRequestUri().getPort();
            } else if (httpsPort == 443) {
                return -1;
            } else {
                return httpsPort;
            }
        } else if (alwaysHttps) {
            if (httpsPort == 443) {
                return -1;
            } else {
                return httpsPort;
            }
        } else {
            if (httpPort == -1) {
                return originalUriInfo.getRequestUri().getPort();
            } else if (httpPort == 80) {
                return -1;
            } else {
                return httpPort;
            }
        }
    }

}
