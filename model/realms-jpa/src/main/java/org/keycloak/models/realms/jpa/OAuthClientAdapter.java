package org.keycloak.models.realms.jpa;

import org.keycloak.models.realms.RealmProvider;
import org.keycloak.models.realms.OAuthClient;
import org.keycloak.models.realms.Realm;
import org.keycloak.models.realms.jpa.entities.OAuthClientEntity;

import javax.persistence.EntityManager;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthClientAdapter extends ClientAdapter implements OAuthClient {

    protected final OAuthClientEntity oAuthClientEntity;

    public OAuthClientAdapter(RealmProvider provider, OAuthClientEntity entity, EntityManager em) {
        super(provider, entity, em);
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
        if (o == null || !(o instanceof OAuthClient)) return false;

        OAuthClient that = (OAuthClient) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
