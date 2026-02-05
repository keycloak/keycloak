package org.keycloak.scim.resource.schema;

import org.keycloak.models.ModelIdentifier;
import org.keycloak.scim.resource.ScimResource;

public interface ScimModelSchema<M extends ModelIdentifier, R extends ScimResource> {

    void populate(M model, R user);
    void populate(R user, M model);
    void validate(R resource);
}
