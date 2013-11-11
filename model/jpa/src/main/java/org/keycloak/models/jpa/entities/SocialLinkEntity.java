package org.keycloak.models.jpa.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="findSocialLinkByUser", query="select link from SocialLinkEntity link where link.user = :user"),
        @NamedQuery(name="findUserByLinkAndRealm", query="select link.user from SocialLinkEntity link where link.realm = :realm and link.socialProvider = :socialProvider and link.socialUsername = :socialUsername"),
        @NamedQuery(name="findSocialLinkByAll", query="select link.user from SocialLinkEntity link where link.realm = :realm and link.socialProvider = :socialProvider and link.socialUsername = :socialUsername and link.user = :user")
})
@Entity
public class SocialLinkEntity {
    @Id
    @GeneratedValue
    private long id;

    @ManyToOne
    private UserEntity user;

    @ManyToOne
    protected RealmEntity realm;

    protected String socialProvider;
    protected String socialUsername;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getSocialProvider() {
        return socialProvider;
    }

    public void setSocialProvider(String socialProvider) {
        this.socialProvider = socialProvider;
    }

    public String getSocialUsername() {
        return socialUsername;
    }

    public void setSocialUsername(String socialUsername) {
        this.socialUsername = socialUsername;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }
}
