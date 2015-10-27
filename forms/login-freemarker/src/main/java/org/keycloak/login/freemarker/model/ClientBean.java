package org.keycloak.login.freemarker.model;

import org.keycloak.models.ClientModel;
import org.keycloak.services.util.ResolveRelative;

import java.net.URI;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientBean {

    protected ClientModel client;

    private URI requestUri;

    public ClientBean(ClientModel client, URI requestUri) {
        this.client = client;
        this.requestUri = requestUri;
    }

    public String getClientId() {
        return client.getClientId();
    }

    public String getName() {
        return client.getName();
    }

    public String getBaseUrl() {
        return ResolveRelative.resolveRelativeUri(requestUri, client.getRootUrl(), client.getBaseUrl());
    }

    public Map<String,String> getAttributes(){
        return client.getAttributes();
    }

    public String getAttribute(String key){
        return client.getAttribute(key);
    }
}
