package org.keycloak.migration.migrators;

import java.util.Objects;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.KeycloakSession;

public class MigrateTo12_0_0 implements Migration {

  public static final ModelVersion VERSION = new ModelVersion("12.0.0");

  @Override
  public void migrate(KeycloakSession session) {
    session.realms()
        .getRealms()
        .stream()
        .map(realm -> realm.getClientByClientId("account"))
        .filter(client -> Objects.isNull(client.getRole(AccountRoles.DELETE_ACCOUNT)))
        .forEach(client -> client.addRole(AccountRoles.DELETE_ACCOUNT)
            .setDescription("${" + AccountRoles.DELETE_ACCOUNT + "}"));
  }

  @Override
  public ModelVersion getVersion() {
    return VERSION;
  }
}
