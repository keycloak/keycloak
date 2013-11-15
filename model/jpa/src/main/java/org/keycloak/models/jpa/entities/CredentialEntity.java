package org.keycloak.models.jpa.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="credentialByUserAndType", query="select cred from CredentialEntity cred where cred.user = :user and cred.type = :type")
})
@Entity
public class CredentialEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected String id;

    protected String type;
    protected String value;
    protected String device;

    @ManyToOne
    protected UserEntity user;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
}
