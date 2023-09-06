package org.keycloak.services.resource;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.theme.Theme;

/**
 * <p>A factory that creates {@link AccountResourceProvider} instances.
 */
public interface AccountResourceProviderFactory extends ProviderFactory<AccountResourceProvider> {

  default Theme getTheme(KeycloakSession session) {
    try {
      return session.theme().getTheme(Theme.Type.ACCOUNT);
    } catch (IOException e) {
      throw new InternalServerErrorException(e);
    }
  }

  default ClientModel getAccountManagementClient(RealmModel realm) {
    ClientModel client = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
    if (client == null || !client.isEnabled()) {
      throw new NotFoundException("account management not enabled");
    }
    return client;
  }
}
