package org.keycloak.models.jpa.entities;

import org.hibernate.annotations.GenericGenerator;
import org.keycloak.models.UserModel;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
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
        @NamedQuery(name="getRealmUserByLoginName", query="select u from UserEntity u where u.loginName = :loginName and u.realm = :realm"),
        @NamedQuery(name="getRealmUserByEmail", query="select u from UserEntity u where u.email = :email and u.realm = :realm"),
        @NamedQuery(name="getRealmUserByLastName", query="select u from UserEntity u where u.lastName = :lastName and u.realm = :realm"),
        @NamedQuery(name="getRealmUserByFirstLastName", query="select u from UserEntity u where u.firstName = :first and u.lastName = :last and u.realm = :realm")
})
@Entity
public class UserEntity {
    @Id
    @GenericGenerator(name="uuid_generator", strategy="org.keycloak.models.jpa.utils.JpaIdGenerator")
    @GeneratedValue(generator = "uuid_generator")
    protected String id;

    protected String loginName;
    protected String firstName;
    protected String lastName;
    protected String email;
    protected boolean enabled;
    protected boolean totp;
    protected boolean emailVerified;

    @ManyToOne
    protected RealmEntity realm;

    @ElementCollection
    @MapKeyColumn(name="name")
    @Column(name="value")
    @CollectionTable
    protected Map<String, String> attributes = new HashMap<String, String>();

    @ElementCollection
    @CollectionTable
    protected Set<UserModel.RequiredAction> requiredActions = new HashSet<UserModel.RequiredAction>();

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    protected Collection<CredentialEntity> credentials = new ArrayList<CredentialEntity>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
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
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Set<UserModel.RequiredAction> getRequiredActions() {
        return requiredActions;
    }

    public void setRequiredActions(Set<UserModel.RequiredAction> requiredActions) {
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
}
