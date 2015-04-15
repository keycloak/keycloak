package org.keycloak.services;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmModel;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultKeycloakContext implements KeycloakContext {

    private RealmModel realm;

    private ClientModel client;

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

}
