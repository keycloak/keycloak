package org.keycloak.services.resources.account;

import java.io.IOException;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;

import org.keycloak.Config.Scope;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resource.AccountResourceProvider;
import org.keycloak.services.resource.AccountResourceProviderFactory;
import org.keycloak.theme.Theme;

/**
 * Provides the {@code default} {@link AccountConsole} implementation backed by the
 * {@code account} management client.
 */
public class AccountConsoleFactory implements AccountResourceProviderFactory {

  @Override
  public String getId() {
    return "default";
  }

  @Override
  public AccountResourceProvider create(KeycloakSession session) {
    RealmModel realm = session.getContext().getRealm();
    ClientModel client = getAccountManagementClient(realm);
    Theme theme = getTheme(session);
    return createAccountConsole(session, client, theme);
  }

  protected AccountConsole createAccountConsole(KeycloakSession session, ClientModel client, Theme theme) {
    return new AccountConsole(session, client, theme);
  }

  @Override
  public void init(Scope config) {}

  @Override
  public void postInit(KeycloakSessionFactory factory) {}

  @Override
  public void close() {}

  protected Theme getTheme(KeycloakSession session) {
    try {
      return session.theme().getTheme(Theme.Type.ACCOUNT);
    } catch (IOException e) {
      throw new InternalServerErrorException(e);
    }
  }

  protected  ClientModel getAccountManagementClient(RealmModel realm) {
    ClientModel client = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
    if (client == null || !client.isEnabled()) {
      throw new NotFoundException("account management not enabled");
    }
    return client;
  }
}
