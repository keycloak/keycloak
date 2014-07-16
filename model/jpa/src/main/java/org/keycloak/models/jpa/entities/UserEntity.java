package org.keycloak.models.jpa.entities;

import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="getRealmUserById", query="select u from UserEntity u where u.id = :id and u.realm = :realm"),
        @NamedQuery(name="getRealmUserByUsername", query="select u from UserEntity u where u.username = :username and u.realm = :realm"),
        @NamedQuery(name="getRealmUserByEmail", query="select u from UserEntity u where u.email = :email and u.realm = :realm"),
        @NamedQuery(name="getRealmUserByLastName", query="select u from UserEntity u where u.lastName = :lastName and u.realm = :realm"),
        @NamedQuery(name="getRealmUserByFirstLastName", query="select u from UserEntity u where u.firstName = :first and u.lastName = :last and u.realm = :realm"),
        @NamedQuery(name="deleteUsersByRealm", query="delete from UserEntity u where u.realm = :realm")
})
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = { "realm", "username" }),
        @UniqueConstraint(columnNames = { "realm", "emailConstraint" })
})
public class UserEntity {
    @Id
    @Column(length = 36)
    protected String id;

    protected String username;
    protected String firstName;
    protected String lastName;
    protected String email;
    protected boolean enabled;
    protected boolean totp;
    protected boolean emailVerified;

    // Hack just to workaround the fact that on MS-SQL you can't have unique constraint with multiple NULL values TODO: Find better solution (like unique index with 'where' but that's proprietary)
    protected String emailConstraint = KeycloakModelUtils.generateId();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "realm")
    protected RealmEntity realm;

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy="user")
    protected Collection<UserAttributeEntity> attributes = new ArrayList<UserAttributeEntity>();

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy="user")
    protected Collection<UserRequiredActionEntity> requiredActions = new ArrayList<UserRequiredActionEntity>();

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy="user")
    protected Collection<CredentialEntity> credentials = new ArrayList<CredentialEntity>();

    @ManyToOne
    @JoinColumn(name="link_id")
    protected AuthenticationLinkEntity authenticationLink;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        this.emailConstraint = email != null ? email : KeycloakModelUtils.generateId();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEmailConstraint() {
        return emailConstraint;
    }

    public void setEmailConstraint(String emailConstraint) {
        this.emailConstraint = emailConstraint;
    }

    public boolean isTotp() {
        return totp;
    }

    public void setTotp(boolean totp) {
        this.totp = totp;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Collection<UserAttributeEntity> getAttributes() {
        return attributes;
    }

    public void setAttributes(Collection<UserAttributeEntity> attributes) {
        this.attributes = attributes;
    }

    public Collection<UserRequiredActionEntity> getRequiredActions() {
        return requiredActions;
    }

    public void setRequiredActions(Collection<UserRequiredActionEntity> requiredActions) {
        this.requiredActions = requiredActions;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public Collection<CredentialEntity> getCredentials() {
        return credentials;
    }

    public void setCredentials(Collection<CredentialEntity> credentials) {
        this.credentials = credentials;
    }

    public AuthenticationLinkEntity getAuthenticationLink() {
        return authenticationLink;
    }

    public void setAuthenticationLink(AuthenticationLinkEntity authenticationLink) {
        this.authenticationLink = authenticationLink;
    }

}
