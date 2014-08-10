package org.keycloak.models.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="credentialByUserAndType", query="select cred from CredentialEntity cred where cred.user = :user and cred.type = :type"),
        @NamedQuery(name="deleteCredentialsByRealm", query="delete from CredentialEntity cred where cred.user IN (select u from UserEntity u where u.realmId=:realmId)"),
        @NamedQuery(name="deleteCredentialsByRealmAndLink", query="delete from CredentialEntity cred where cred.user IN (select u from UserEntity u where u.realmId=:realmId and u.federationLink=:link)")

})
@Table(name="CREDENTIAL")
@Entity
public class CredentialEntity {
    @Id
    @Column(name="ID", length = 36)
    protected String id;

    @Column(name="TYPE")
    protected String type;
    @Column(name="VALUE")
    protected String value;
    @Column(name="DEVICE")
    protected String device;
    @Column(name="SALT")
    protected byte[] salt;
    @Column(name="HASH_ITERATIONS")
    protected int hashIterations;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="USER_ID")
    protected UserEntity user;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public int getHashIterations() {
        return hashIterations;
    }

    public void setHashIterations(int hashIterations) {
        this.hashIterations = hashIterations;
    }
}
