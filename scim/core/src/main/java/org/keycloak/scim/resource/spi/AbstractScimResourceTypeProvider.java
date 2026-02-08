package org.keycloak.scim.resource.spi;

import java.util.List;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelIdentifier;
import org.keycloak.models.ModelValidationException;
import org.keycloak.scim.resource.ScimResource;
import org.keycloak.scim.resource.schema.ScimModelSchema;

public abstract class AbstractScimResourceTypeProvider<M extends ModelIdentifier, R extends ScimResource> implements ScimResourceTypeProvider<R> {

    protected final KeycloakSession session;
    protected final List<ScimModelSchema<M, R>> schemas;

    public AbstractScimResourceTypeProvider(KeycloakSession session, List<ScimModelSchema<M, R>> schemas) {
        this.session = session;
        this.schemas = schemas;
    }

    public void validate(R resource) throws ModelValidationException {
        onValidate(resource);
        for (ScimModelSchema<M, R> schema : schemas) {
            schema.validate(resource);
        }
    }

    @Override
    public R create(R resource) {
        M model = onCreate(resource);
        for (ScimModelSchema<M, R> schema : schemas) {
            schema.populate(model, resource);
        }
        return resource;
    }

    @Override
    public void update(R resource) {
        M model = getModel(resource.getId());
        for (ScimModelSchema<M, R> schema : schemas) {
            schema.populate(model, resource);
        }
    }

    @Override
    public R get(String id) {
        M model = getModel(id);

        if (model == null) {
            return null;
        }

        R resource = createResourceTypeInstance();

        for (ScimModelSchema<M, R> schema : schemas) {
            schema.populate(resource, model);
        }

        return resource;
    }

    protected abstract R createResourceTypeInstance();

    protected abstract M getModel(String id);

    protected abstract M onCreate(R resource);

    protected abstract void onValidate(R resource);
}
