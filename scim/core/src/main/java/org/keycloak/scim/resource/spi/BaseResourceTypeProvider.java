package org.keycloak.scim.resource.spi;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.Profile;
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

        Stream<M> models = getModels(searchOptions);

        // The query scope only allows a caller to reach this endpoint, it does not entitle them to read the
        // resources it matches. When fine-grained admin permissions v2 are enabled for the realm, the queries
        // behind getModels are already narrowed at the SQL level by AdminPermissionsSchema#applyAuthorizationFilters,
        // which is gated on that same realm flag. In every other configuration, including the deprecated v1
        // feature, nothing narrows the result set, so the per-resource VIEW check is the only enforcement point.
        // This mirrors UsersResource#toRepresentation and GroupsResource#getGroups in the Admin REST API.
        // Resources are filtered before being converted so that representations are never built for resources the
        // caller cannot see.
        if (!AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(session.getContext().getRealm())
                && !hasPermission(getRealmResourceType(), AdminPermissionsSchema.VIEW)) {
            // The page was already narrowed by the query, so this filter removes resources from a page that has
            // been paginated. Record how many resources the query matched before filtering: the count of total
            // results is derived from the size of the page the query produced, and a page shortened by this filter
            // must not be mistaken for the last page.
            List<M> page = models.toList();

            searchOptions.setMatchedSize(page.size());

            if (isViewFullyDenied()) {
                // Plain RBAC: the VIEW check above evaluates the same way for every resource of this type, so
                // lacking it here means the caller cannot view any resource of this type, on any page. count()
                // independently recognizes this and reports zero instead of falling through to a DB count that
                // does not account for permissions at all.
                models = Stream.empty();
            } else {
                // Deprecated fine-grained admin permissions v1: a per-resource policy can grant view access to a
                // subset of resources, so some, none, or all resources on this page may pass. Because this is not
                // narrowed at the SQL level, count() falls back to an unfiltered DB count, making totalResults an
                // upper bound rather than an exact count.
                models = page.stream().filter(m -> hasPermission(m, getRealmResourceType(), AdminPermissionsSchema.VIEW));
            }
        }

        return models.map(m -> createResourceTypeInstance(m, searchOptions.getAttributes(), searchOptions.getExcludedAttributes()));
    }

    @Override
    public boolean delete(String id) {
        M model = getModel(id);
        
        if (model == null || !hasPermission(model, getRealmResourceType(), AdminPermissionsSchema.MANAGE)) {
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

    /**
     * Whether the caller cannot view any resource of this type, on any page, because neither fine-grained admin
     * permissions v2 nor the deprecated v1 are in effect for the realm and the caller lacks the blanket VIEW
     * permission. Without any fine-grained admin permissions, the per-resource VIEW check evaluates the same way
     * for every resource of this type, so this is an all-or-nothing decision. Implementations of {@code count()}
     * must honor this by reporting zero rather than falling back to a DB count that does not account for
     * permissions at all.
     *
     * @return {@code true} when the caller is denied view access to every resource of this type
     */
    protected boolean isViewFullyDenied() {
        return !AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(session.getContext().getRealm())
                && !hasPermission(getRealmResourceType(), AdminPermissionsSchema.VIEW)
                && !Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ);
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
