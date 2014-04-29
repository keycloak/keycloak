package org.keycloak.models;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SocialLinkModel {

    private String socialUserId;
    private String socialProvider;
    private String socialUsername;

    public SocialLinkModel() {};

    public SocialLinkModel(String socialProvider, String socialUserId, String socialUsername) {
        this.socialUserId = socialUserId;
        this.socialProvider = socialProvider;
        this.socialUsername = socialUsername;
    }

    public String getSocialUserId() {
        return socialUserId;
    }

    public void setSocialUserId(String socialUserId) {
        this.socialUserId = socialUserId;
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
}
