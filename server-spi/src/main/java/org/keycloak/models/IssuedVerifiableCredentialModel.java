package org.keycloak.models;

public class IssuedVerifiableCredentialModel {

    private String id;
    private String userId;
    private String verifiableCredentialId;
    private Long issuedAt;
    private Long expiresAt;
    // This represents UUID of the client, which acts as OID4VCI wallet
    private String clientId;
    private String revision;

    public IssuedVerifiableCredentialModel() {
    }

    public IssuedVerifiableCredentialModel(String userId, String verifiableCredentialId, String clientId) {
        this.userId = userId;
        this.verifiableCredentialId = verifiableCredentialId;
        this.clientId = clientId;
    }

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

    public String getVerifiableCredentialId() {
        return verifiableCredentialId;
    }

    public void setVerifiableCredentialId(String verifiableCredentialId) {
        this.verifiableCredentialId = verifiableCredentialId;
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
}
