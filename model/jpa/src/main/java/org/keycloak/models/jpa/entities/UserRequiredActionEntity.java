package org.keycloak.models.jpa.entities;

import org.keycloak.models.UserModel;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="deleteUserRequiredActionsByRealm", query="delete from UserRequiredActionEntity action where action.user IN (select u from UserEntity u where realm=:realm)")
})
@Entity
public class UserRequiredActionEntity {
    @Id
    @GeneratedValue
    protected long id;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="userId")
    protected UserEntity user;

    protected UserModel.RequiredAction action;

    public long getId() {
        return id;
    }

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

}
