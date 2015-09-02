package org.keycloak.services;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.ClientConnection;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.RealmImporter;
import org.keycloak.services.managers.RealmManager;
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

    private KeycloakSession session;

    public DefaultKeycloakContext(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getContextPath() {
        KeycloakApplication app = getContextObject(KeycloakApplication.class);
        return app.getContextPath();
    }

    @Override
    public UriInfo getUri() {
        return getContextObject(UriInfo.class);
    }

    @Override
    public HttpHeaders getRequestHeaders() {
        return getContextObject(HttpHeaders.class);
    }

    @Override
    public <T> T getContextObject(Class<T> clazz) {
        return ResteasyProviderFactory.getContextData(clazz);
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

    @Override
    public RealmImporter getRealmManager() {
        RealmManager manager = new RealmManager(session);
        manager.setContextPath(getContextPath());
        return manager;
    }
}
