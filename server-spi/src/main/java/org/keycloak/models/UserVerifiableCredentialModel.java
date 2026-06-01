package org.keycloak.models;

import java.util.List;
import java.util.Map;

public class UserVerifiableCredentialModel {

    private final String credentialScopeName;
    private String revision;
    private Long createdDate;
    private Map<String, List<String>> userAttributes;

    public UserVerifiableCredentialModel(String credentialScopeName) {
        this.credentialScopeName = credentialScopeName;
    }

    public String getCredentialScopeName() {
        return credentialScopeName;
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
}
