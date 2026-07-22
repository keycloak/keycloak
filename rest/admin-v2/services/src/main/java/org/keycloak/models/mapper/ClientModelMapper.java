package org.keycloak.models.mapper;

import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public interface ClientModelMapper extends RepModelMapper<BaseClientRepresentation, ClientModel> {
    default void toModel(BaseClientRepresentation rep, ClientModel model, Set<String> excludedFields) {
        toModel(rep, model);
    }
}
