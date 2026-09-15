package org.keycloak.representations.idm.oid4vc;

import java.util.List;
import java.util.Map;
import java.util.Objects;


public class UserVerifiableCredentialRepresentation {

    private String credentialScopeName;
    private String credentialConfigurationId;
    private String revision;
    private Long createdDate;
    private Long updatedDate;
    private Map<String, List<String>> userAttributes;

    public String getCredentialConfigurationId() {
        return credentialConfigurationId;
    }

    public void setCredentialConfigurationId(String credentialConfigurationId) {
        this.credentialConfigurationId = credentialConfigurationId;
    }

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

    public Map<String, List<String>> getUserAttributes() { return userAttributes; }

    public void setUserAttributes(Map<String, List<String>> userAttributes) { this.userAttributes = userAttributes; }

    public Long getUpdatedDate() { return updatedDate; }

    public void setUpdatedDate(Long updatedDate) { this.updatedDate = updatedDate; }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserVerifiableCredentialRepresentation that = (UserVerifiableCredentialRepresentation) o;
        return Objects.equals(credentialScopeName, that.credentialScopeName)
                && Objects.equals(credentialConfigurationId, that.getCredentialConfigurationId())
                && Objects.equals(revision, that.revision)
                && Objects.equals(createdDate, that.createdDate)
                && Objects.equals(userAttributes, that.userAttributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(credentialScopeName, credentialConfigurationId, revision, createdDate, userAttributes);
    }
}
