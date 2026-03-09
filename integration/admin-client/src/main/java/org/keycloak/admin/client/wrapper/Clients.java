package org.keycloak.admin.client.wrapper;

import jakarta.ws.rs.client.WebTarget;

import org.keycloak.admin.api.AdminRootV2;
import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.admin.client.spi.ResteasyClientProvider;

public class Clients {

  private final ResteasyClientProvider clientProvider;
  private final WebTarget target;
  private final String realmName;

  public Clients(ResteasyClientProvider clientProvider, WebTarget target, String realmName) {
    this.clientProvider = clientProvider;
    this.target = target;
    this.realmName = realmName;
  }

  public ClientsApi v2() {
    return clientProvider.targetProxy(target, AdminRootV2.class).adminApi(realmName).clients("v2");
  }
}
