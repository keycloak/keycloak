package org.keycloak.models.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;

import org.hibernate.annotations.GenericGenerator;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NamedQueries({
        @NamedQuery(name="deleteAuthenticationLinksByRealm", query="delete from AuthenticationLinkEntity authLink where authLink.user IN (select u from UserEntity u where realm=:realm)")
})
@Entity
public class AuthenticationLinkEntity {

    @Id
    @GeneratedValue
    protected long id;

    protected String authProvider;
    protected String authUserId;

    // NOTE: @OnetoOne creates a constraint race condition if the join column is on AuthenticationLinkEntity.
    // The race is that user gets loaded concurrently, creates link concurrently, and sets it.  Therefore, we have
    // a @ManyToOne on both sides.  Broken yes, but, I think we're going to replace AuthenticationLinkEntity anyways.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="userId")
    protected UserEntity user;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public String getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }
}
