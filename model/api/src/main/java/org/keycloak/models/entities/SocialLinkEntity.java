package org.keycloak.models.entities;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SocialLinkEntity {

    private String socialUserId;
    private String socialUsername;
    private String socialProvider;

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

    public String getSocialProvider() {
        return socialProvider;
    }

    public void setSocialProvider(String socialProvider) {
        this.socialProvider = socialProvider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SocialLinkEntity that = (SocialLinkEntity) o;

        if (socialProvider != null && (that.socialProvider == null || !socialProvider.equals(that.socialProvider))) return false;
        if (socialUserId != null && (that.socialUserId == null || !socialUserId.equals(that.socialUserId))) return false;
        if (socialProvider == null && that.socialProvider != null)return false;
        if (socialUserId == null && that.socialUserId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int code = 1;
        if (socialUserId != null) {
            code = code * socialUserId.hashCode() * 13;
        }
        if (socialProvider != null) {
            code = code * socialProvider.hashCode() * 17;
        }
        return code;
    }
}
