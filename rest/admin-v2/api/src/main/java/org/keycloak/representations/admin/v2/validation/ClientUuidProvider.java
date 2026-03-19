package org.keycloak.representations.admin.v2.validation;

import java.util.Optional;

import org.keycloak.models.ClientModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.BaseRepresentation;
import org.keycloak.validation.jakarta.ValidationContext;

/**
 * @author Vaclav Muzikar <vmuzikar@ibm.com>
 */
public class ClientUuidProvider implements UuidProvider {
    @Override
    public boolean uuidExists(ValidationContext context, String uuid) {
        return Optional.ofNullable(context.realm().getClientById(uuid)).isPresent();
    }

    @Override
    public String getPersistedUuid(ValidationContext context, BaseRepresentation representation) {
        String clientId = ((BaseClientRepresentation) representation).getClientId();
        return Optional.ofNullable(context.realm().getClientByClientId(clientId)).map(ClientModel::getId).orElse(null);
    }
}
