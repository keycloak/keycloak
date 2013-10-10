package org.keycloak.models.mongo.keycloak.adapters;

import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.NoSQL;
import org.keycloak.models.mongo.keycloak.data.OAuthClientData;
import org.keycloak.models.mongo.keycloak.data.UserData;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OAuthClientAdapter implements OAuthClientModel {

    private final OAuthClientData delegate;
    private UserAdapter oauthAgent;
    private final NoSQL noSQL;

    public OAuthClientAdapter(OAuthClientData oauthClientData, UserAdapter oauthAgent, NoSQL noSQL) {
        this.delegate = oauthClientData;
        this.oauthAgent = oauthAgent;
        this.noSQL = noSQL;
    }

    public OAuthClientAdapter(OAuthClientData oauthClientData, NoSQL noSQL) {
        this.delegate = oauthClientData;
        this.noSQL = noSQL;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public UserModel getOAuthAgent() {
        // This is not thread-safe. Assumption is that OAuthClientAdapter instance is per-client object
        if (oauthAgent == null) {
            UserData user = noSQL.loadObject(UserData.class, delegate.getOauthAgentId());
            oauthAgent = user!=null ? new UserAdapter(user, noSQL) : null;
        }
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
