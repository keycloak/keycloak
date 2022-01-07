package org.keycloak.protocol.oidc;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.StringUtil;

public class FrontChannelLogoutHandler {

    public static FrontChannelLogoutHandler current(KeycloakSession session) {
        return (FrontChannelLogoutHandler) session.getAttribute(FrontChannelLogoutHandler.class.getName());
    }

    public static FrontChannelLogoutHandler currentOrCreate(KeycloakSession session, AuthenticatedClientSessionModel clientSession) {
        FrontChannelLogoutHandler current = current(session);

        if (current == null) {
            return new FrontChannelLogoutHandler(session, clientSession);
        }

        return current;
    }

    private final KeycloakSession session;
    private final String sid;
    private final String issuer;
    private final List<ClientInfo> clients = new ArrayList<>();

    private String logoutRedirectUri;

    private FrontChannelLogoutHandler(KeycloakSession session, AuthenticatedClientSessionModel clientSession) {
        this.session = session;
        this.sid = clientSession.getUserSession().getId();
        this.issuer = clientSession.getNote(OIDCLoginProtocol.ISSUER);
        this.session.setAttribute(getClass().getName(), this);
    }

    public void addClient(ClientModel client) {
        clients.add(new ClientInfo(client));
    }

    public List<ClientInfo> getClients() {
        return clients;
    }

    public String getLogoutRedirectUri() {
        return logoutRedirectUri;
    }

    public Response renderLogoutPage(String redirectUri) {
        configureCSP();
        this.logoutRedirectUri = redirectUri;
        return session.getProvider(LoginFormsProvider.class).createFrontChannelLogoutPage();
    }

    private void configureCSP() {
        StringBuilder allowFrameSrc = new StringBuilder();

        for (ClientInfo client : clients) {
            allowFrameSrc.append(client.frontChannelLogoutUrl.getAuthority()).append(' ');
        }

        session.getProvider(SecurityHeadersProvider.class).options().allowAnyFrameAncestor();
        session.getProvider(SecurityHeadersProvider.class).options().allowFrameSrc(allowFrameSrc.toString());
    }

    private URI createFrontChannelLogoutUrl(ClientModel client) {
        String frontChannelLogoutUrl = OIDCAdvancedConfigWrapper.fromClientModel(client).getFrontChannelLogoutUrl();

        if (StringUtil.isBlank(frontChannelLogoutUrl)) {
            frontChannelLogoutUrl = client.getBaseUrl();
        }

        if (frontChannelLogoutUrl == null) {
            throw new RuntimeException("Client [" + client.getClientId() + "] does not have a valid frontend logout URL");
        }

        UriBuilder builder = UriBuilder.fromUri(frontChannelLogoutUrl);

        builder.queryParam("sid", FrontChannelLogoutHandler.this.sid);
        builder.queryParam("iss", FrontChannelLogoutHandler.this.issuer);

        return builder.build();
    }

    public class ClientInfo {

        private final ClientModel client;
        private final URI frontChannelLogoutUrl;

        public ClientInfo(ClientModel client) {
            this.client = client;
            this.frontChannelLogoutUrl = createFrontChannelLogoutUrl(client);
        }

        public String getFrontChannelLogoutUrl() {
            return frontChannelLogoutUrl.toString();
        }

        public String getName() {
            String name = client.getName();

            if (name == null) {
                return client.getClientId();
            }

            return name;
        }
    }
}
