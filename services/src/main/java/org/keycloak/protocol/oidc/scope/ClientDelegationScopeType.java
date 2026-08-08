package org.keycloak.protocol.oidc.scope;

import jakarta.annotation.Nonnull;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

public class ClientDelegationScopeType extends DelegationScopeType {

    public static final String TYPE = "client-delegation";

    public ClientDelegationScopeType() {
    }

    public ClientDelegationScopeType(KeycloakSession session) {
        super(session);
    }

    @Override
    public String getTypeName() {
        return TYPE;
    }

    @Override
    public ParameterizedScopeTypeProvider create(KeycloakSession session) {
        return new ClientDelegationScopeType(session);
    }

    @Override
    public void validateParameter(@Nonnull ClientScopeModel scope, @Nonnull String parameter) throws InvalidScopeParameterException {
        if (StringUtil.isBlank(parameter)) {
            throw new InvalidScopeParameterException("Client ID parameter must not be blank");
        }
    }

    @Override
    protected UserModel resolveUser(ClientScopeModel scope, String parameter) throws InvalidScopeParameterException {
        RealmModel realm = scope.getRealm();
        ClientModel client = realm.getClientByClientId(parameter);
        if (client == null) {
            throw new InvalidScopeParameterException(String.format("Client '%s' not found in realm '%s'", parameter, realm.getName()));
        }
        if (!client.isEnabled()) {
            throw new InvalidScopeParameterException(String.format("Client '%s' is disabled in realm '%s'", parameter, realm.getName()));
        }
        if (!client.isServiceAccountsEnabled()) {
            throw new InvalidScopeParameterException(String.format("Client '%s' does not have service accounts enabled in realm '%s'", parameter, realm.getName()));
        }
        UserModel serviceAccountUser = session.users().getServiceAccount(client);
        if (serviceAccountUser == null) {
            throw new InvalidScopeParameterException(
                    String.format("Client '%s' does not have a service account in realm '%s'", client.getClientId(), realm.getName()));
        }
        return serviceAccountUser;
    }
}
