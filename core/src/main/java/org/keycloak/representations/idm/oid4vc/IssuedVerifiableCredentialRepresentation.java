package org.keycloak.representations.idm.oid4vc;

import java.util.Objects;

public class IssuedVerifiableCredentialRepresentation {

    private String id;
    private String userId;
    private String credentialType;
    private Long issuedAt;
    private Long expiresAt;
    // This represents UUID of the client, which acts as OID4VCI wallet
    private String clientId;
    private String clientName;
    private String clientBaseUrl;

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientBaseUrl() {
        return clientBaseUrl;
    }

    public void setClientBaseUrl(String clientBaseUrl) {
        this.clientBaseUrl = clientBaseUrl;
    }

    private String revision;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    public Long getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Long issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IssuedVerifiableCredentialRepresentation that = (IssuedVerifiableCredentialRepresentation) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(credentialType, that.credentialType) &&
                Objects.equals(issuedAt, that.issuedAt) &&
                Objects.equals(expiresAt, that.expiresAt) &&
                Objects.equals(clientId, that.clientId) &&
                Objects.equals(revision, that.revision);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, credentialType, issuedAt, expiresAt, clientId, revision);
    }
}
