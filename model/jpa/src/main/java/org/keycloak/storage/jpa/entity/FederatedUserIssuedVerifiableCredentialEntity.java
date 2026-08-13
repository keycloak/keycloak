package org.keycloak.storage.jpa.entity;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;


@Entity
@Table(name="FED_ISSUED_VER_CREDENTIAL")
@NamedQueries({
        @NamedQuery(name="federatedIssuedVcsByUser",
                    query="select vc from FederatedUserIssuedVerifiableCredentialEntity vc where vc.userId = :userId order by vc.issuedAt desc"),
        @NamedQuery(name="deleteFederatedIssuedVcsByRealm",
                    query="delete from FederatedUserIssuedVerifiableCredentialEntity vc where vc.realmId = :realmId"),
        @NamedQuery(name="deleteFederatedIssuedVcsByUser",
                    query="delete from FederatedUserIssuedVerifiableCredentialEntity vc where vc.userId = :userId and vc.realmId = :realmId"),
        @NamedQuery(name="deleteFederatedIssuedVcsByClientScope",
                    query="delete from FederatedUserIssuedVerifiableCredentialEntity vc where vc.verifiableCredentialId in (select fuvc.id from FederatedUserVerifiableCredentialEntity fuvc where fuvc.clientScopeId = :scopeId)"),
        @NamedQuery(name="deleteExpiredFederatedIssuedVcs",
                    query="delete from FederatedUserIssuedVerifiableCredentialEntity vc where vc.expiresAt IS NOT NULL and vc.expiresAt < :currentTime"),
        @NamedQuery(name="deleteFederatedIssuedVcsByUserAndType",
                    query="delete from FederatedUserIssuedVerifiableCredentialEntity vc where vc.userId = :userId and vc.realmId = :realmId and vc.verifiableCredentialId = :verifiableCredentialId"),
        @NamedQuery(name="deleteFederatedIssuedVcsByStorageProvider",
                    query="delete from FederatedUserIssuedVerifiableCredentialEntity vc where vc.storageProviderId = :storageProviderId"),
        @NamedQuery(name="deleteFederatedIssuedVcsByClient",
                query="delete from FederatedUserIssuedVerifiableCredentialEntity vc where vc.clientId = :clientId")
})
public class FederatedUserIssuedVerifiableCredentialEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY)
    protected String id;

    @Column(name="USER_ID", nullable = false)
    protected String userId;

    @Column(name="REALM_ID", nullable = false, length = 36)
    protected String realmId;

    @Column(name="STORAGE_PROVIDER_ID", length = 36, nullable = false)
    protected String storageProviderId;

    @Column(name="VER_CREDENTIAL_ID", nullable = false)
    protected String verifiableCredentialId;

    @Column(name="ISSUED_AT", nullable = false)
    protected Long issuedAt;

    @Column(name="EXPIRES_AT")
    protected Long expiresAt;

    @Column(name="CLIENT_ID")
    protected String clientId;

    @Column(name="REVISION", nullable = false)
    protected String revision;

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

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getStorageProviderId() {
        return storageProviderId;
    }

    public void setStorageProviderId(String storageProviderId) {
        this.storageProviderId = storageProviderId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FederatedUserIssuedVerifiableCredentialEntity that = (FederatedUserIssuedVerifiableCredentialEntity) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
