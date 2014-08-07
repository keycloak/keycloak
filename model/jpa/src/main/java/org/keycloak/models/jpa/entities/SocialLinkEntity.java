package org.keycloak.models.jpa.entities;

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
        @NamedQuery(name="findSocialLinkByUser", query="select link from SocialLinkEntity link where link.user = :user"),
        @NamedQuery(name="findSocialLinkByUserAndProvider", query="select link from SocialLinkEntity link where link.user = :user and link.socialProvider = :socialProvider"),
        @NamedQuery(name="findUserByLinkAndRealm", query="select link.user from SocialLinkEntity link where link.realmId = :realmId and link.socialProvider = :socialProvider and link.socialUserId = :socialUserId"),
        @NamedQuery(name="deleteSocialLinkByRealm", query="delete from SocialLinkEntity social where social.user IN (select u from UserEntity u where realmId=:realmId)"),
        @NamedQuery(name="deleteSocialLinkByRealmAndLink", query="delete from SocialLinkEntity social where social.user IN (select u from UserEntity u where realmId=:realmId and u.federationLink=:link)"),
        @NamedQuery(name="deleteSocialLinkByUser", query="delete from SocialLinkEntity social where social.user = :user")
})
@Table(name="USER_SOCIAL_LINK")
@Entity
@IdClass(SocialLinkEntity.Key.class)
public class SocialLinkEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private UserEntity user;

    @Column(name = "REALM_ID")
    protected String realmId;

    @Id
    @Column(name = "SOCIAL_PROVIDER")
    protected String socialProvider;
    @Column(name = "SOCIAL_USER_ID")
    protected String socialUserId;
    @Column(name = "SOCIAL_USERNAME")
    protected String socialUsername;

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

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public static class Key implements Serializable {

        protected UserEntity user;

        protected String socialProvider;

        public Key() {
        }

        public Key(UserEntity user, String socialProvider) {
            this.user = user;
            this.socialProvider = socialProvider;
        }

        public UserEntity getUser() {
            return user;
        }

        public String getSocialProvider() {
            return socialProvider;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (socialProvider != null ? !socialProvider.equals(key.socialProvider) : key.socialProvider != null)
                return false;
            if (user != null ? !user.getId().equals(key.user != null ? key.user.getId() : null) : key.user != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = user != null ? user.getId().hashCode() : 0;
            result = 31 * result + (socialProvider != null ? socialProvider.hashCode() : 0);
            return result;
        }
    }

}
