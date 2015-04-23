package org.keycloak.models.jpa.entities;

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
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Entity
@Table(name="GRANTED_CONSENT", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"USER_ID", "CLIENT_ID"})
})
@NamedQueries({
        @NamedQuery(name="grantedConsentByUserAndClient", query="select consent from GrantedConsentEntity consent where consent.user.id = :userId and consent.clientId = :clientId"),
        @NamedQuery(name="grantedConsentsByUser", query="select consent from GrantedConsentEntity consent where consent.user.id = :userId"),
        @NamedQuery(name="deleteGrantedConsentsByRealm", query="delete from GrantedConsentEntity consent where consent.user IN (select user from UserEntity user where user.realmId = :realmId)"),
        @NamedQuery(name="deleteGrantedConsentsByUser", query="delete from GrantedConsentEntity consent where consent.user = :user"),
        @NamedQuery(name="deleteGrantedConsentsByClient", query="delete from GrantedConsentEntity consent where consent.clientId = :clientId"),
})
public class GrantedConsentEntity {

    @Id
    @Column(name="ID", length = 36)
    protected String id;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="USER_ID")
    protected UserEntity user;

    @Column(name="CLIENT_ID")
    protected String clientId;

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "grantedConsent")
    Collection<GrantedConsentRoleEntity> grantedRoles = new ArrayList<GrantedConsentRoleEntity>();

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "grantedConsent")
    Collection<GrantedConsentProtocolMapperEntity> grantedProtocolMappers = new ArrayList<GrantedConsentProtocolMapperEntity>();

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

    public Collection<GrantedConsentRoleEntity> getGrantedRoles() {
        return grantedRoles;
    }

    public void setGrantedRoles(Collection<GrantedConsentRoleEntity> grantedRoles) {
        this.grantedRoles = grantedRoles;
    }

    public Collection<GrantedConsentProtocolMapperEntity> getGrantedProtocolMappers() {
        return grantedProtocolMappers;
    }

    public void setGrantedProtocolMappers(Collection<GrantedConsentProtocolMapperEntity> grantedProtocolMappers) {
        this.grantedProtocolMappers = grantedProtocolMappers;
    }
}
