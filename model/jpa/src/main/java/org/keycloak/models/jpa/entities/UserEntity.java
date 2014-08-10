package org.keycloak.models.jpa.entities;

import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="getAllUsersByRealm", query="select u from UserEntity u where u.realmId = :realmId order by u.username"),
        @NamedQuery(name="searchForUser", query="select u from UserEntity u where u.realmId = :realmId and ( lower(u.username) like :search or lower(concat(u.firstName, ' ', u.lastName)) like :search or u.email like :search ) order by u.username"),
        @NamedQuery(name="getRealmUserById", query="select u from UserEntity u where u.id = :id and u.realmId = :realmId"),
        @NamedQuery(name="getRealmUserByUsername", query="select u from UserEntity u where u.username = :username and u.realmId = :realmId"),
        @NamedQuery(name="getRealmUserByEmail", query="select u from UserEntity u where u.email = :email and u.realmId = :realmId"),
        @NamedQuery(name="getRealmUserByLastName", query="select u from UserEntity u where u.lastName = :lastName and u.realmId = :realmId"),
        @NamedQuery(name="getRealmUserByFirstLastName", query="select u from UserEntity u where u.firstName = :first and u.lastName = :last and u.realmId = :realmId"),
        @NamedQuery(name="getRealmUserCount", query="select count(u) from UserEntity u where u.realmId = :realmId"),
        @NamedQuery(name="deleteUsersByRealm", query="delete from UserEntity u where u.realmId = :realmId"),
        @NamedQuery(name="deleteUsersByRealmAndLink", query="delete from UserEntity u where u.realmId = :realmId and u.federationLink=:link")
})
@Entity
@Table(name="USER_ENTITY", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "REALM_ID", "USERNAME" }),
        @UniqueConstraint(columnNames = { "REALM_ID", "EMAIL_CONSTRAINT" })
})
public class UserEntity {
    @Id
    @Column(name="ID", length = 36)
    protected String id;

    @Column(name = "USERNAME")
    protected String username;
    @Column(name = "FIRST_NAME")
    protected String firstName;
    @Column(name = "LAST_NAME")
    protected String lastName;
    @Column(name = "EMAIL")
    protected String email;
    @Column(name = "ENABLED")
    protected boolean enabled;
    @Column(name = "TOTP")
    protected boolean totp;
    @Column(name = "EMAIL_VERIFIED")
    protected boolean emailVerified;

    // Hack just to workaround the fact that on MS-SQL you can't have unique constraint with multiple NULL values TODO: Find better solution (like unique index with 'where' but that's proprietary)
    @Column(name = "EMAIL_CONSTRAINT")
    protected String emailConstraint = KeycloakModelUtils.generateId();

    @Column(name = "REALM_ID")
    protected String realmId;

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy="user")
    protected Collection<UserAttributeEntity> attributes = new ArrayList<UserAttributeEntity>();

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy="user")
    protected Collection<UserRequiredActionEntity> requiredActions = new ArrayList<UserRequiredActionEntity>();

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy="user")
    protected Collection<CredentialEntity> credentials = new ArrayList<CredentialEntity>();

    @Column(name="federation_link")
    protected String federationLink;

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

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public Collection<CredentialEntity> getCredentials() {
        return credentials;
    }

    public void setCredentials(Collection<CredentialEntity> credentials) {
        this.credentials = credentials;
    }

    public String getFederationLink() {
        return federationLink;
    }

    public void setFederationLink(String federationLink) {
        this.federationLink = federationLink;
    }
}
