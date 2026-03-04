package org.keycloak.admin.client.wrapper;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.api.AdminRootV2;
import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.spi.ResteasyClientProvider;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.List;

public class Clients {

  private final Keycloak keycloak;
  private final ResteasyClientProvider CLIENT_PROVIDER;
  private final WebTarget target;
  private final String realmName;

  public Clients(Keycloak keycloak, ResteasyClientProvider CLIENT_PROVIDER, WebTarget target, String realmName) {
    this.keycloak = keycloak;
    this.CLIENT_PROVIDER = CLIENT_PROVIDER;
    this.target = target;
    this.realmName = realmName;
  }

  private ClientsResource legacyDelegate() {
    return keycloak.realm(realmName).clients();
  }

  public ClientResource get(String id) {
    return legacyDelegate().get(id);
  }

  public Response create(ClientRepresentation clientRepresentation) {
    return legacyDelegate().create(clientRepresentation);
  }

  public List<ClientRepresentation> findAll() {
    return legacyDelegate().findAll();
  }

  List<ClientRepresentation> findAll(boolean viewableOnly) {
    return legacyDelegate().findAll(viewableOnly);
  }

  List<ClientRepresentation> findAll(String clientId,
                                     Boolean viewableOnly,
                                     Boolean search,
                                     Integer firstResult,
                                     Integer maxResults) {
    return legacyDelegate().findAll(clientId, viewableOnly, search, firstResult, maxResults);
  }


  public List<ClientRepresentation> findByClientId(String clientId) {
    return legacyDelegate().findByClientId(clientId);
  }

  List<ClientRepresentation> query(String searchQuery) {
    return legacyDelegate().query(searchQuery);
  }

  public Response delete(String id) {
    return legacyDelegate().delete(id);
  }

  public ClientsApi v2() {
    return CLIENT_PROVIDER.targetProxy(target, AdminRootV2.class).adminApi(realmName).clients("v2");
  }
}
