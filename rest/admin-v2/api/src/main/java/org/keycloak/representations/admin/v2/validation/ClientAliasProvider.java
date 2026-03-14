package org.keycloak.representations.admin.v2.validation;

import java.util.Optional;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.BaseRepresentation;

/**
 * @author Vaclav Muzikar <vmuzikar@ibm.com>
 */
public class ClientAliasProvider implements AliasProvider {
    @Override
    public String getPersistedAlias(KeycloakSession session, RealmModel realm, String uuid) {
        return Optional.ofNullable(realm.getClientById(uuid)).map(ClientModel::getClientId).orElse(null);
    }

    @Override
    public String getAliasFromRepresentation(BaseRepresentation representation) {
        return ((BaseClientRepresentation) representation).getClientId();
    }
}
