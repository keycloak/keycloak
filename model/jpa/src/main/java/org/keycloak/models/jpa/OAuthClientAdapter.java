package org.keycloak.models.jpa;

import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.OAuthClientEntity;

import javax.persistence.EntityManager;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthClientAdapter extends ClientAdapter implements OAuthClientModel {

    protected final OAuthClientEntity oAuthClientEntity;

    public OAuthClientAdapter(RealmModel realm, OAuthClientEntity entity, EntityManager em) {
        super(realm, entity, em);
        oAuthClientEntity = entity;
    }

    @Override
    public void setClientId(String id) {
        entity.setName(id);

    }

    @Override
    public boolean isDirectGrantsOnly() {
        return oAuthClientEntity.isDirectGrantsOnly();
    }

    @Override
    public void setDirectGrantsOnly(boolean flag) {
        oAuthClientEntity.setDirectGrantsOnly(flag);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof OAuthClientModel)) return false;

        OAuthClientModel that = (OAuthClientModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
