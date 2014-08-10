package org.keycloak.models.jpa.entities;

import org.keycloak.models.UserModel;

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
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="deleteUserRequiredActionsByRealm", query="delete from UserRequiredActionEntity action where action.user IN (select u from UserEntity u where u.realmId=:realmId)"),
        @NamedQuery(name="deleteUserRequiredActionsByRealmAndLink", query="delete from UserRequiredActionEntity action where action.user IN (select u from UserEntity u where u.realmId=:realmId and u.federationLink=:link)")
})
@Entity
@Table(name="USER_REQUIRED_ACTION")
@IdClass(UserRequiredActionEntity.Key.class)
public class UserRequiredActionEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="USER_ID")
    protected UserEntity user;

    @Id
    @Column(name="ACTION")
    protected UserModel.RequiredAction action;

    public UserModel.RequiredAction getAction() {
        return action;
    }

    public void setAction(UserModel.RequiredAction action) {
        this.action = action;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public static class Key implements Serializable {

        protected UserEntity user;

        protected UserModel.RequiredAction action;

        public Key() {
        }

        public Key(UserEntity user, UserModel.RequiredAction action) {
            this.user = user;
            this.action = action;
        }

        public UserEntity getUser() {
            return user;
        }

        public UserModel.RequiredAction getAction() {
            return action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (action != key.action) return false;
            if (user != null ? !user.getId().equals(key.user != null ? key.user.getId() : null) : key.user != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = user != null ? user.getId().hashCode() : 0;
            result = 31 * result + (action != null ? action.hashCode() : 0);
            return result;
        }
    }

}
