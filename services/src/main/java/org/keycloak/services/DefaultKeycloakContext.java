package org.keycloak.services;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.ClientConnection;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.KeycloakApplication;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultKeycloakContext implements KeycloakContext {

    private RealmModel realm;

    private ClientModel client;

    private ClientConnection connection;

    @Override
    public String getContextPath() {
        KeycloakApplication app = ResteasyProviderFactory.getContextData(KeycloakApplication.class);
        return app.getContextPath();
    }

    @Override
    public UriInfo getUri() {
        return ResteasyProviderFactory.getContextData(UriInfo.class);
    }

    @Override
    public HttpHeaders getRequestHeaders() {
        return ResteasyProviderFactory.getContextData(HttpHeaders.class);
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public void setRealm(RealmModel realm) {
        this.realm = realm;
    }

    @Override
    public ClientModel getClient() {
        return client;
    }

    @Override
    public void setClient(ClientModel client) {
        this.client = client;
    }

    @Override
    public ClientConnection getConnection() {
        return connection;
    }

    @Override
    public void setConnection(ClientConnection connection) {
        this.connection = connection;
    }
}
