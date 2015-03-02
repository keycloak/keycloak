package org.keycloak.models.file.adapter;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.entities.OAuthClientEntity;

/**
 * OAuthClientModel for JSON persistence.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OAuthClientAdapter extends ClientAdapter implements OAuthClientModel {

    private final OAuthClientEntity oauthClientEntity;

    public OAuthClientAdapter(KeycloakSession session, RealmModel realm, OAuthClientEntity oauthClientEntity) {
        super(session, realm, oauthClientEntity);
        this.oauthClientEntity = oauthClientEntity;
    }

    public String getName() {
        return oauthClientEntity.getName();
    }

    @Override
    public void setClientId(String id) {
        if (id == null) throw new NullPointerException("id == null");
        if (oauthClientEntity.getName().equals(id)) return;  // allow setting name to same name
        RealmAdapter realmAdapter = (RealmAdapter)realm;
        if (realmAdapter.hasOAuthClientWithClientId(id)) throw new ModelDuplicateException("Realm already has OAuthClient with client id " + id);
        oauthClientEntity.setName(id);
    }

    @Override
    public boolean isDirectGrantsOnly() {
        return oauthClientEntity.isDirectGrantsOnly();
    }

    @Override
    public void setDirectGrantsOnly(boolean flag) {
        oauthClientEntity.setDirectGrantsOnly(flag);
    }
}
