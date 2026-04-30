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

@Entity
@Table(name="USER_VER_CREDENTIAL", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"USER_ID", "CREDENTIAL_SCOPE_ID"})
})
@NamedQueries({
        @NamedQuery(name="verifiableCredentialsByUser", query="select vc from UserVerifiableCredentialEntity vc where vc.user.id = :userId"),
        @NamedQuery(name="deleteVerifiableCredentialsByRealm", query="delete from UserVerifiableCredentialEntity vc where vc.user IN (select user from UserEntity user where user.realmId = :realmId)"),
        @NamedQuery(name="deleteVerifiableCredentialsByClientScope", query="delete from UserVerifiableCredentialEntity vc where vc.credentialScopeName = :scopeName"),
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

    @Column(name="CREDENTIAL_SCOPE_NAME")
    protected String credentialScopeName;

    @Column(name="REVISION")
    protected String revision;

    @Column(name = "CREATED_DATE")
    private Long createdDate;

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
