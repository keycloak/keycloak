package org.keycloak.services.models;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SocialLinkModel {

    private String socialUsername;
    private String socialProvider;

    public SocialLinkModel(String socialProvider, String socialUsername) {
        this.socialUsername = socialUsername;
        this.socialProvider = socialProvider;
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
}
