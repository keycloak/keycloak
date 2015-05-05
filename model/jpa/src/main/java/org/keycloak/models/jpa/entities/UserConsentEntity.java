package org.keycloak.models.jpa.entities;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Entity
@Table(name="USER_CONSENT", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"USER_ID", "CLIENT_ID"})
})
@NamedQueries({
        @NamedQuery(name="userConsentByUserAndClient", query="select consent from UserConsentEntity consent where consent.user.id = :userId and consent.clientId = :clientId"),
        @NamedQuery(name="userConsentsByUser", query="select consent from UserConsentEntity consent where consent.user.id = :userId"),
        @NamedQuery(name="deleteUserConsentsByRealm", query="delete from UserConsentEntity consent where consent.user IN (select user from UserEntity user where user.realmId = :realmId)"),
        @NamedQuery(name="deleteUserConsentsByUser", query="delete from UserConsentEntity consent where consent.user = :user"),
        @NamedQuery(name="deleteUserConsentsByClient", query="delete from UserConsentEntity consent where consent.clientId = :clientId"),
})
public class UserConsentEntity {

    @Id
    @Column(name="ID", length = 36)
    protected String id;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="USER_ID")
    protected UserEntity user;

    @Column(name="CLIENT_ID")
    protected String clientId;

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "userConsent")
    Collection<UserConsentRoleEntity> grantedRoles = new ArrayList<UserConsentRoleEntity>();

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "userConsent")
    Collection<UserConsentProtocolMapperEntity> grantedProtocolMappers = new ArrayList<UserConsentProtocolMapperEntity>();

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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Collection<UserConsentRoleEntity> getGrantedRoles() {
        return grantedRoles;
    }

    public void setGrantedRoles(Collection<UserConsentRoleEntity> grantedRoles) {
        this.grantedRoles = grantedRoles;
    }

    public Collection<UserConsentProtocolMapperEntity> getGrantedProtocolMappers() {
        return grantedProtocolMappers;
    }

    public void setGrantedProtocolMappers(Collection<UserConsentProtocolMapperEntity> grantedProtocolMappers) {
        this.grantedProtocolMappers = grantedProtocolMappers;
    }
}
