package org.keycloak.scim.resource.spi;

import org.keycloak.models.ModelValidationException;
import org.keycloak.provider.Provider;
import org.keycloak.scim.resource.ScimResource;

public interface ScimResourceTypeProvider<R extends ScimResource> extends Provider {

    void validate(R resource) throws ModelValidationException;
    R create(R resource);
    void update(R user);
    R get(String id);
    boolean delete(String id);

}
