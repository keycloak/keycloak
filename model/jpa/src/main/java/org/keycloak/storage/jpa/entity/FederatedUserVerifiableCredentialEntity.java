package org.keycloak.storage.jpa.entity;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;


@Entity
@Table(name="FED_USER_VER_CREDENTIAL", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"USER_ID", "CREDENTIAL_SCOPE_ID"})
})
@NamedQueries({
        @NamedQuery(name="federatedVerifiableCredentialsByUser", query="select vc from FederatedUserVerifiableCredentialEntity vc where vc.userId = :userId"),
        @NamedQuery(name="deleteFederatedVerifiableCredentialsByRealm", query="delete from FederatedUserVerifiableCredentialEntity vc where vc.realmId = :realmId"),
        @NamedQuery(name="deleteFederatedVerifiableCredentialsByClientScope", query="delete from FederatedUserVerifiableCredentialEntity vc where vc.credentialScopeName = :scopeName"),
        @NamedQuery(name="deleteFederatedVerifiableCredentialsByUser", query="delete from FederatedUserVerifiableCredentialEntity vc where vc.userId = :userId and vc.realmId = :realmId"),
        @NamedQuery(name="deleteFederatedVerifiableCredentialsByStorageProvider", query="delete from FederatedUserVerifiableCredentialEntity vc where vc.storageProviderId = :storageProviderId"),
})
public class FederatedUserVerifiableCredentialEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY)
    protected String id;

    @Column(name="USER_ID")
    protected String userId;

    @Column(name="REALM_ID")
    protected String realmId;

    @Column(name="STORAGE_PROVIDER_ID")
    protected String storageProviderId;

    @Column(name="CREDENTIAL_SCOPE_NAME")
    protected String credentialScopeName;

    @Column(name="REVISION")
    protected String revision;

    @Column(name="USER_ATTRIBUTES", length = 4000)
    protected String userAttributes;

    @Column(name="CREATED_DATE")
    private Long createdDate;

    @Column(name="UPDATED_DATE")
    private Long updatedDate;

    @Version
    @Column(name="VERSION")
    private int version;

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

    public String getUserAttributes() {
        return userAttributes;
    }

    public void setUserAttributes(String userAttributes) {
        this.userAttributes = userAttributes;
    }

    public Long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }

    public Long getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Long updatedDate) {
        this.updatedDate = updatedDate;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof FederatedUserVerifiableCredentialEntity that)) return false;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
