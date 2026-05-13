package org.keycloak.representations.idm.oid4vc;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserVerifiableCredentialRepresentation that = (UserVerifiableCredentialRepresentation) o;
        return Objects.equals(credentialScopeName, that.credentialScopeName) && Objects.equals(revision, that.revision) && Objects.equals(createdDate, that.createdDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(credentialScopeName, revision, createdDate);
    }
}
