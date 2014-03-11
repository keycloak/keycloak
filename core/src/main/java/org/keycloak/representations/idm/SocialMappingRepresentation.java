package org.keycloak.representations.idm;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SocialMappingRepresentation {

    protected String self; // link
    protected String username;
    protected List<SocialLinkRepresentation> socialLinks;

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<SocialLinkRepresentation> getSocialLinks() {
        return socialLinks;
    }

    public SocialLinkRepresentation socialLink(String socialProvider, String socialUserId, String socialUsername) {
        SocialLinkRepresentation link = new SocialLinkRepresentation();
        link.setSocialProvider(socialProvider);
        link.setSocialUserId(socialUserId);
        link.setSocialUsername(socialUsername);
        if (socialLinks == null) socialLinks = new ArrayList<SocialLinkRepresentation>();
        socialLinks.add(link);
        return link;
    }
}
