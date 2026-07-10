package org.keycloak.models.jpa.entities;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name="USER_VER_CREDENTIAL", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"USER_ID", "CLIENT_SCOPE_ID"})
})
@NamedQueries({
        @NamedQuery(name="verifiableCredentialsByUser", query="select vc from UserVerifiableCredentialEntity vc where vc.user.id = :userId"),
        @NamedQuery(name="deleteVerifiableCredentialsByRealm", query="delete from UserVerifiableCredentialEntity vc where vc.user IN (select user from UserEntity user where user.realmId = :realmId)"),
        @NamedQuery(name="deleteVerifiableCredentialsByClientScope", query="delete from UserVerifiableCredentialEntity vc where vc.clientScopeId = :scopeId"),
        @NamedQuery(name="deleteVerifiableCredentialsByUser", query="delete from UserVerifiableCredentialEntity vc where vc.user = :user"),
})
public class UserVerifiableCredentialEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="USER_ID")
    protected UserEntity user;

    @Column(name="CLIENT_SCOPE_ID")
    protected String clientScopeId;

    @Column(name="REVISION")
    protected String revision;

    @Column(name = "USER_ATTRIBUTES")
    protected String userAttributes;

    @Column(name = "CREATED_DATE")
    private Long createdDate;

    @Column(name = "UPDATED_DATE")
    private Long updatedDate;

    @Version
    @Column(name = "VERSION")
    private int version;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
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

    public String getUserAttributes() { return userAttributes; }

    public void setUserAttributes(String userAttributes) { this.userAttributes = userAttributes; }

    public Long getUpdatedDate() { return updatedDate; }

    public void setUpdatedDate(Long updatedDate) { this.updatedDate = updatedDate; }

    public int getVersion() { return version; }

    public void setVersion(int version) { this.version = version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof UserVerifiableCredentialEntity)) return false;

        UserVerifiableCredentialEntity that = (UserVerifiableCredentialEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
