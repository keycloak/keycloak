package org.keycloak.models;

import java.util.List;
import java.util.Map;

public class UserVerifiableCredentialModel {

    private String id;
    private String clientScopeId;
    private String revision;
    private Long createdDate;
    private Long updatedDate;
    private Map<String, List<String>> userAttributes;

    public UserVerifiableCredentialModel(String id, String clientScopeId) {
        this.id = id;
        this.clientScopeId = clientScopeId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientScopeId() {
        return clientScopeId;
    }

    public void setClientScopeId(String clientScopeId) {
        this.clientScopeId = clientScopeId;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public Long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }

    public Map<String, List<String>> getUserAttributes() { return userAttributes; }

    public void setUserAttributes(Map<String, List<String>> userAttributes) { this.userAttributes = userAttributes; }

    public Long getUpdatedDate() { return updatedDate; }

    public void setUpdatedDate(Long updatedDate) { this.updatedDate = updatedDate; }
}
