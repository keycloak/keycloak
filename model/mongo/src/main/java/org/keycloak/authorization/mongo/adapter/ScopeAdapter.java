package org.keycloak.authorization.mongo.adapter;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.mongo.entities.ScopeEntity;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.adapters.AbstractMongoAdapter;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ScopeAdapter extends AbstractMongoAdapter<ScopeEntity> implements Scope {

    private final ScopeEntity entity;
    private final AuthorizationProvider authorizationProvider;

    public ScopeAdapter(ScopeEntity entity, MongoStoreInvocationContext invocationContext, AuthorizationProvider authorizationProvider) {
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
    protected ScopeEntity getMongoEntity() {
        return this.entity;
    }
}
