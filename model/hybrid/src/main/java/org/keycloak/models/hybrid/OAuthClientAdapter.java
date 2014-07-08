package org.keycloak.models.hybrid;

import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.realms.OAuthClient;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OAuthClientAdapter extends ClientAdapter implements OAuthClientModel {

    private OAuthClient oauthClient;

    OAuthClientAdapter(HybridModelProvider provider, OAuthClient oauthClient) {
        super(provider, oauthClient);
        this.oauthClient = oauthClient;
    }

    OAuthClient getOauthClient() {
        return oauthClient;
    }

    @Override
    public void setClientId(String id) {
        oauthClient.setClientId(id);
    }

}
