package org.keycloak.authorization.mongo.adapter;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.mongo.entities.ResourceEntity;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.adapters.AbstractMongoAdapter;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourceAdapter extends AbstractMongoAdapter<ResourceEntity> implements Resource {

    private final ResourceEntity entity;
    private final AuthorizationProvider authorizationProvider;

    public ResourceAdapter(ResourceEntity entity, MongoStoreInvocationContext invocationContext, AuthorizationProvider authorizationProvider) {
        super(invocationContext);
        this.entity = entity;
        this.authorizationProvider = authorizationProvider;
    }

    @Override
    public String getId() {
        return getMongoEntity().getId();
    }

    @Override
    public String getName() {
        return getMongoEntity().getName();
    }

    @Override
    public void setName(String name) {
        getMongoEntity().setName(name);
        updateMongoEntity();
    }

    @Override
    public String getUri() {
        return getMongoEntity().getUri();
    }

    @Override
    public void setUri(String uri) {
        getMongoEntity().setUri(uri);
        updateMongoEntity();
    }

    @Override
    public String getType() {
        return getMongoEntity().getType();
    }

    @Override
    public void setType(String type) {
        getMongoEntity().setType(type);
        updateMongoEntity();
    }

    @Override
    public List<Scope> getScopes() {
        return getMongoEntity().getScopes().stream()
                .map(id -> authorizationProvider.getStoreFactory().getScopeStore().findById(id))
                .collect(toList());
    }

    @Override
    public String getIconUri() {
        return getMongoEntity().getIconUri();
    }

    @Override
    public void setIconUri(String iconUri) {
        getMongoEntity().setIconUri(iconUri);
        updateMongoEntity();
    }

    @Override
    public ResourceServer getResourceServer() {
        return this.authorizationProvider.getStoreFactory().getResourceServerStore().findById(getMongoEntity().getResourceServerId());
    }

    @Override
    public String getOwner() {
        return getMongoEntity().getOwner();
    }

    @Override
    public void updateScopes(Set<Scope> scopes) {
        getMongoEntity().updateScopes(scopes);
        updateMongoEntity();
    }

    @Override
    protected ResourceEntity getMongoEntity() {
        return this.entity;
    }
}
