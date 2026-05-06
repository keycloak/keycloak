package org.keycloak.models;

public class UserVerifiableCredentialModel {

    private final String credentialScopeName;
    private String revision;
    private Long createdDate;

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


}
