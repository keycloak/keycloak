package org.keycloak.models.mapper;

import org.keycloak.models.ClientModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;

import java.util.Set;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public interface ClientModelMapper extends RepModelMapper<BaseClientRepresentation, ClientModel> {
    void toModel(BaseClientRepresentation rep, ClientModel model, Set<String> excludedFields);
}
