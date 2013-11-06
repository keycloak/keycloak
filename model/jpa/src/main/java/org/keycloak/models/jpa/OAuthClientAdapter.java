package org.keycloak.models.jpa;

import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.OAuthClientEntity;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthClientAdapter implements OAuthClientModel {
    protected OAuthClientEntity entity;

    public OAuthClientAdapter(OAuthClientEntity entity) {
        this.entity = entity;
    }

    public OAuthClientEntity getEntity() {
        return entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public UserModel getOAuthAgent() {
        return new UserAdapter(entity.getAgent());
    }

}
