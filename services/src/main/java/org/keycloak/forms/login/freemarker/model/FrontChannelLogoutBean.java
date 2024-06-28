package org.keycloak.forms.login.freemarker.model;

import java.util.List;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.FrontChannelLogoutHandler;

public class FrontChannelLogoutBean {

    private final FrontChannelLogoutHandler logoutInfo;

    public FrontChannelLogoutBean(KeycloakSession session) {
        logoutInfo = FrontChannelLogoutHandler.current(session);
    }

    public String getLogoutRedirectUri() {
        return logoutInfo.getLogoutRedirectUri();
    }

    public List<FrontChannelLogoutHandler.ClientInfo> getClients() {
        return logoutInfo.getClients();
    }

}
