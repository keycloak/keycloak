package org.keycloak.login.freemarker.model;

import org.keycloak.models.ClientModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientBean {

    protected ClientModel client;

    public ClientBean(ClientModel client) {
        this.client = client;
    }

    public String getClientId() {
        return client.getClientId();
    }

    public String getName() {
        return client.getName();
    }

    public String getBaseUrl() {
        return client.getBaseUrl();
    }

}
