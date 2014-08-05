package org.keycloak.login.freemarker.model;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.OAuthClientModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientBean {
    protected ClientModel client;

    public ClientBean(ClientModel client) {
        this.client = client;
    }

    public boolean isApplication() {
        return client instanceof ApplicationModel;
    }

    public boolean isOauthClient() {
        return client instanceof OAuthClientModel;
    }

    public String getClientId() {
        return client.getClientId();
    }
}
