package org.keycloak.migration.migrators;

import java.util.Objects;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RequiredActionProviderModel;

public class MigrateTo12_0_0 implements Migration {

  public static final ModelVersion VERSION = new ModelVersion("12.0.0");

  public static final RequiredActionProviderModel deleteAccount = new RequiredActionProviderModel();

  static {
    deleteAccount.setEnabled(false);
    deleteAccount.setAlias("delete_account");
    deleteAccount.setName("Delete Account");
    deleteAccount.setProviderId("delete_account");
    deleteAccount.setDefaultAction(false);
    deleteAccount.setPriority(60);
  }


  @Override
  public void migrate(KeycloakSession session) {
    session.realms()
        .getRealms()
        .stream()
        .map(realm -> realm.getClientByClientId("account"))
        .filter(client -> Objects.isNull(client.getRole(AccountRoles.DELETE_ACCOUNT)))
        .forEach(client -> client.addRole(AccountRoles.DELETE_ACCOUNT)
            .setDescription("${role_"+AccountRoles.DELETE_ACCOUNT+"}"));

    session.realms().getRealms().stream().filter(realm -> Objects.isNull(realm.getRequiredActionProviderByAlias("delete_account"))).forEach(realm -> realm.addRequiredActionProvider(deleteAccount));
  }

  @Override
  public ModelVersion getVersion() {
    return VERSION;
  }
}
