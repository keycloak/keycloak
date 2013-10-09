package org.keycloak.models.mongo.keycloak.adapters;

import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.NoSQL;
import org.keycloak.models.mongo.keycloak.data.OAuthClientData;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OAuthClientAdapter implements OAuthClientModel {

    private final OAuthClientData delegate;
    private final UserAdapter oauthAgent;
    private final NoSQL noSQL;

    public OAuthClientAdapter(OAuthClientData oauthClientData, UserAdapter oauthAgent, NoSQL noSQL) {
        this.delegate = oauthClientData;
        this.oauthAgent = oauthAgent;
        this.noSQL = noSQL;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public UserModel getOAuthAgent() {
        return oauthAgent;
    }

    @Override
    public String getBaseUrl() {
        return delegate.getBaseUrl();
    }

    @Override
    public void setBaseUrl(String base) {
        delegate.setBaseUrl(base);
        noSQL.saveObject(delegate);
    }
}
