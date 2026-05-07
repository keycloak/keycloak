package org.keycloak.representations.idm.oid4vc;

public class UserVerifiableCredentialRepresentation {

    private String credentialScopeName;
    private String revision;
    private Long createdDate;

    public String getCredentialScopeName() {
        return credentialScopeName;
    }

    public void setCredentialScopeName(String credentialScopeName) {
        this.credentialScopeName = credentialScopeName;
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
