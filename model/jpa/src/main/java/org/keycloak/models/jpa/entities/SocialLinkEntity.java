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

import org.hibernate.annotations.GenericGenerator;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="findSocialLinkByUser", query="select link from SocialLinkEntity link where link.user = :user"),
        @NamedQuery(name="findSocialLinkByUserAndProvider", query="select link from SocialLinkEntity link where link.user = :user and link.socialProvider = :socialProvider"),
        @NamedQuery(name="findUserByLinkAndRealm", query="select link.user from SocialLinkEntity link where link.realm = :realm and link.socialProvider = :socialProvider and link.socialUserId = :socialUserId"),
        @NamedQuery(name="deleteSocialLinkByRealm", query="delete from SocialLinkEntity social where social.user IN (select u from UserEntity u where realm=:realm)")
})
@Entity
public class SocialLinkEntity {
    @Id
    @GeneratedValue
    protected long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    protected RealmEntity realm;

    protected String socialProvider;
    protected String socialUserId;
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

    public String getSocialUserId() {
        return socialUserId;
    }

    public void setSocialUserId(String socialUserId) {
        this.socialUserId = socialUserId;
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
