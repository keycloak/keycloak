package org.keycloak.models;

public class SamlArtifactSessionMappingModel {
    
    private final String userSessionId;
    private final String clientSessionId;

    public SamlArtifactSessionMappingModel(String userSessionId, String clientSessionId) {
        this.userSessionId = userSessionId;
        this.clientSessionId = clientSessionId;
    }

    public String getUserSessionId() {
        return userSessionId;
    }

    public String getClientSessionId() {
        return clientSessionId;
    }
}
