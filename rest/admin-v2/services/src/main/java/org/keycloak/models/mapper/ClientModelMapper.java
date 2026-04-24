package org.keycloak.models.mapper;

import org.keycloak.models.ClientModel;
import org.keycloak.provider.Provider;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public interface ClientModelMapper extends Provider, RepModelMapper<BaseClientRepresentation, ClientModel> {
}
