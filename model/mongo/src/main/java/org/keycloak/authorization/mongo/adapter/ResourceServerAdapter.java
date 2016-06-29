package org.keycloak.authorization.mongo.adapter;

import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.mongo.entities.ResourceServerEntity;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.adapters.AbstractMongoAdapter;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourceServerAdapter extends AbstractMongoAdapter<ResourceServerEntity> implements ResourceServer{

    private final ResourceServerEntity entity;

    public ResourceServerAdapter(ResourceServerEntity entity, MongoStoreInvocationContext invocationContext) {
        super(invocationContext);
        this.entity = entity;
    }

    @Override
    public String getId() {
        return getMongoEntity().getId();
    }

    @Override
    public String getClientId() {
        return getMongoEntity().getClientId();
    }

    @Override
    public boolean isAllowRemoteResourceManagement() {
        return getMongoEntity().isAllowRemoteResourceManagement();
    }

    @Override
    public void setAllowRemoteResourceManagement(boolean allowRemoteResourceManagement) {
        getMongoEntity().setAllowRemoteResourceManagement(allowRemoteResourceManagement);
        updateMongoEntity();
    }

    @Override
    public PolicyEnforcementMode getPolicyEnforcementMode() {
        return getMongoEntity().getPolicyEnforcementMode();
    }

    @Override
    public void setPolicyEnforcementMode(PolicyEnforcementMode enforcementMode) {
        getMongoEntity().setPolicyEnforcementMode(enforcementMode);
        updateMongoEntity();
    }

    @Override
    protected ResourceServerEntity getMongoEntity() {
        return this.entity;
    }
}
