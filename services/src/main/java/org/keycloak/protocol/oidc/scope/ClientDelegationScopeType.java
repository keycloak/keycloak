package org.keycloak.protocol.oidc.scope;

import jakarta.annotation.Nonnull;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.utils.StringUtil;

public class ClientDelegationScopeType implements ParameterizedScopeTypeProvider {

    public static final String TYPE = "client-delegation";

    private final KeycloakSession session;

    public ClientDelegationScopeType() {
        this.session = null;
    }

    public ClientDelegationScopeType(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getTypeName() {
        return TYPE;
    }

    @Override
    public boolean isRepeatable() {
        return false;
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
    public void validateParameterWithUser(@Nonnull UserModel currentUser, @Nonnull ClientScopeModel scope, @Nonnull String parameter) throws InvalidScopeParameterException {
        ClientModel targetClient = resolveClient(scope, parameter);
        UserModel serviceAccountUser = session.users().getServiceAccount(targetClient);
        if (serviceAccountUser == null) {
            throw new InvalidScopeParameterException(
                    String.format("Client '%s' does not have a service account in realm '%s'", targetClient.getClientId(), scope.getRealm().getName()));
        }
        if (serviceAccountUser.getId().equals(currentUser.getId())) {
            throw new InvalidScopeParameterException("User cannot target themselves");
        }
    }

    protected ClientModel resolveClient(ClientScopeModel scope, String parameter) throws InvalidScopeParameterException {
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
        if (!Boolean.parseBoolean(client.getAttribute(OIDCConfigAttributes.CLIENT_DELEGATION_ENABLED))) {
            throw new InvalidScopeParameterException(String.format("Client '%s' does not have client delegation enabled in realm '%s'", parameter, realm.getName()));
        }
        return client;
    }
}
