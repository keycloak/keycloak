package org.keycloak.models.jpa.entities;

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
import java.io.Serializable;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NamedQueries({
        @NamedQuery(name="deleteGrantedConsentRolesByRealm", query="delete from GrantedConsentRoleEntity grantedRole where grantedRole.grantedConsent IN (select consent from GrantedConsentEntity consent where consent.user IN (select user from UserEntity user where user.realmId = :realmId))"),
        @NamedQuery(name="deleteGrantedConsentRolesByUser", query="delete from GrantedConsentRoleEntity grantedRole where grantedRole.grantedConsent IN (select consent from GrantedConsentEntity consent where consent.user = :user)"),
        @NamedQuery(name="deleteGrantedConsentRolesByRole", query="delete from GrantedConsentRoleEntity grantedRole where grantedRole.roleId = :roleId)"),
        @NamedQuery(name="deleteGrantedConsentRolesByClient", query="delete from GrantedConsentRoleEntity grantedRole where grantedRole.grantedConsent IN (select consent from GrantedConsentEntity consent where consent.clientId = :clientId)"),
})
@Entity
@Table(name="GRANTED_CONSENT_ROLE")
@IdClass(GrantedConsentRoleEntity.Key.class)
public class GrantedConsentRoleEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "GRANTED_CONSENT_ID")
    protected GrantedConsentEntity grantedConsent;

    @Id
    @Column(name="ROLE_ID")
    protected String roleId;

    public GrantedConsentEntity getGrantedConsent() {
        return grantedConsent;
    }

    public void setGrantedConsent(GrantedConsentEntity grantedConsent) {
        this.grantedConsent = grantedConsent;
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

        GrantedConsentRoleEntity that = (GrantedConsentRoleEntity)o;
        Key myKey = new Key(this.grantedConsent, this.roleId);
        Key hisKey = new Key(that.grantedConsent, that.roleId);
        return myKey.equals(hisKey);
    }

    @Override
    public int hashCode() {
        Key myKey = new Key(this.grantedConsent, this.roleId);
        return myKey.hashCode();
    }

    public static class Key implements Serializable {

        protected GrantedConsentEntity grantedConsent;

        protected String roleId;

        public Key() {
        }

        public Key(GrantedConsentEntity grantedConsent, String roleId) {
            this.grantedConsent = grantedConsent;
            this.roleId = roleId;
        }

        public GrantedConsentEntity getGrantedConsent() {
            return grantedConsent;
        }

        public String getRoleId() {
            return roleId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (grantedConsent != null ? !grantedConsent.getId().equals(key.grantedConsent != null ? key.grantedConsent.getId() : null) : key.grantedConsent != null) return false;
            if (roleId != null ? !roleId.equals(key.roleId) : key.roleId != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = grantedConsent != null ? grantedConsent.getId().hashCode() : 0;
            result = 31 * result + (roleId != null ? roleId.hashCode() : 0);
            return result;
        }
    }

}
