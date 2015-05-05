package org.keycloak.models.jpa.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NamedQueries({
        @NamedQuery(name="deleteUserConsentRolesByRealm", query="delete from UserConsentRoleEntity grantedRole where grantedRole.userConsent IN (select consent from UserConsentEntity consent where consent.user IN (select user from UserEntity user where user.realmId = :realmId))"),
        @NamedQuery(name="deleteUserConsentRolesByUser", query="delete from UserConsentRoleEntity grantedRole where grantedRole.userConsent IN (select consent from UserConsentEntity consent where consent.user = :user)"),
        @NamedQuery(name="deleteUserConsentRolesByRole", query="delete from UserConsentRoleEntity grantedRole where grantedRole.roleId = :roleId)"),
        @NamedQuery(name="deleteUserConsentRolesByClient", query="delete from UserConsentRoleEntity grantedRole where grantedRole.userConsent IN (select consent from UserConsentEntity consent where consent.clientId = :clientId)"),
})
@Entity
@Table(name="USER_CONSENT_ROLE")
@IdClass(UserConsentRoleEntity.Key.class)
public class UserConsentRoleEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "USER_CONSENT_ID")
    protected UserConsentEntity userConsent;

    @Id
    @Column(name="ROLE_ID")
    protected String roleId;

    public UserConsentEntity getUserConsent() {
        return userConsent;
    }

    public void setUserConsent(UserConsentEntity userConsent) {
        this.userConsent = userConsent;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserConsentRoleEntity that = (UserConsentRoleEntity)o;
        Key myKey = new Key(this.userConsent, this.roleId);
        Key hisKey = new Key(that.userConsent, that.roleId);
        return myKey.equals(hisKey);
    }

    @Override
    public int hashCode() {
        Key myKey = new Key(this.userConsent, this.roleId);
        return myKey.hashCode();
    }

    public static class Key implements Serializable {

        protected UserConsentEntity userConsent;

        protected String roleId;

        public Key() {
        }

        public Key(UserConsentEntity userConsent, String roleId) {
            this.userConsent = userConsent;
            this.roleId = roleId;
        }

        public UserConsentEntity getUserConsent() {
            return userConsent;
        }

        public String getRoleId() {
            return roleId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (userConsent != null ? !userConsent.getId().equals(key.userConsent != null ? key.userConsent.getId() : null) : key.userConsent != null) return false;
            if (roleId != null ? !roleId.equals(key.roleId) : key.roleId != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = userConsent != null ? userConsent.getId().hashCode() : 0;
            result = 31 * result + (roleId != null ? roleId.hashCode() : 0);
            return result;
        }
    }

}
