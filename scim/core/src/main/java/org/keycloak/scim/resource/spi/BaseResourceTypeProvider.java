package org.keycloak.scim.resource.spi;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.Model;
import org.keycloak.scim.protocol.ForbiddenException;
import org.keycloak.scim.resource.schema.ModelSchema;

public abstract class BaseResourceTypeProvider<M extends Model, R> implements ScimResourceTypeProvider<R> {

    protected final KeycloakSession session;
    protected final List<ModelSchema<M, R>> schemas;

    public BaseResourceTypeProvider(KeycloakSession session, Stream<? extends ModelSchema<M, ? extends R>> schemas) {
        this.session = session;
        this.schemas = schemas.map(s -> (ModelSchema<M, R>)s).toList();
    }
    
    @Override
    public R create(R resource) {
        if (!hasPermission(getRealmResourceType(), AdminPermissionsSchema.MANAGE)) {
            throw new ForbiddenException();
        }

        return onCreate(resource);
    }
    
    protected abstract String getModelId(R resource);

    @Override
    public R update(R resource) {
        M model = getModel(getModelId(resource));

        if (!hasPermission(model, getRealmResourceType(), AdminPermissionsSchema.MANAGE)) {
            throw new ForbiddenException();
        }

        populate(model, resource);

        return onUpdate(model, resource);
    }

    @Override
    public R get(String id) {
        return get(id, null, null);
    }

    public R get(String id, List<String> attributes, List<String> excludedAttributes) {
        M model = getModel(id);

        if (model == null) {
            // Do not leak the existence of a resource to callers without view permission:
            // a missing resource must be reported as forbidden (403), not as not found (404),
            // otherwise callers can probe which resource ids exist.
            if (!canQuery()) {
                throw new ForbiddenException();
            }
            return null;
        }

        if (!hasPermission(model, getRealmResourceType(), AdminPermissionsSchema.VIEW)) {
            throw new ForbiddenException();
        }

        return createResourceTypeInstance(model, attributes, excludedAttributes);
    }

    @Override
    public Stream<R> getAll(SearchOptions searchOptions) {
        if (!canQuery()) {
            throw new ForbiddenException();
        }

        return getModels(searchOptions)
                .map(m -> createResourceTypeInstance(m, searchOptions.getAttributes(), searchOptions.getExcludedAttributes()));
    }

    @Override
    public boolean delete(String id) {
        M model = getModel(id);
        
        if (!hasPermission(model, getRealmResourceType(), AdminPermissionsSchema.MANAGE)) {
            throw new ForbiddenException();
        }
        
        return onDelete(model);
    }
    
    @Override
    public List<ModelSchema<M, R>> getSchemas() {
        return schemas;
    }

    protected abstract R onCreate(R resource);

    protected abstract R onUpdate(M model, R resource);

    protected abstract boolean onDelete(M m);

    protected abstract Stream<M> getModels(SearchOptions searchOptions);

    protected abstract M getModel(String id);

    protected abstract String getRealmResourceType();

    protected abstract void populate(M model, R resource);
    
    protected abstract R createResourceTypeInstance(M model, List<String> attributes, List<String> excludedAttributes);

    protected boolean canQuery() {
        return hasPermission(getRealmResourceType(), AdminPermissionsSchema.QUERY)
                || hasPermission(getRealmResourceType(), AdminPermissionsSchema.VIEW);
    }

    public boolean hasPermission(R representation, String scope) {
        return hasPermission(Optional.ofNullable(representation).map(this::getModelId).map(this::getModel).orElse(null), getRealmResourceType(), scope);
    }

    protected boolean hasPermission(String realmResourceType, String scope) {
        return hasPermission(null, realmResourceType, scope);
    }

    protected boolean hasPermission(M model, String realmResourceType, String scope) {
        if (AdminPermissionsSchema.VIEW.equals(scope)) {
            return session.getContext().getPermissions().hasPermission(model, realmResourceType, scope);
        }

        return session.getContext().getPermissions().hasPermission(model, realmResourceType, scope) && (model == null || isManageable(model));
    }
    
    protected boolean isManageable(M model) {
        return true;
    }
}
