package org.keycloak.services.clientpolicy.context;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

public class PreTokenRequestContext implements ClientPolicyContext {

    private final String clientId;
    private final MultivaluedMap<String, String> requestParameters;

    public PreTokenRequestContext(String clientId, MultivaluedMap<String, String> requestParameters) {
        this.clientId = clientId;
        this.requestParameters = requestParameters;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.PRE_TOKEN_REQUEST;
    }

    public String getClientId() {
        return clientId;
    }

    public MultivaluedMap<String, String> getRequestParameters() {
        return requestParameters;
    }

}
