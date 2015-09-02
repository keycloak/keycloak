package org.keycloak.models;

import org.keycloak.ClientConnection;
import org.keycloak.models.utils.RealmImporter;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface KeycloakContext {

    String getContextPath();

    UriInfo getUri();

    HttpHeaders getRequestHeaders();

    <T> T getContextObject(Class<T> clazz);

    RealmModel getRealm();

    void setRealm(RealmModel realm);

    ClientModel getClient();

    void setClient(ClientModel client);

    ClientConnection getConnection();

    void setConnection(ClientConnection connection);

    RealmImporter getRealmManager();

}
