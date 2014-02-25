package org.keycloak.account.freemarker.model;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.UriBuilder;

import org.keycloak.models.RealmModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.flows.Urls;
import org.keycloak.social.SocialLoader;
import org.keycloak.social.SocialProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AccountSocialBean {

    private final List<SocialLinkEntry> socialLinks;

    public AccountSocialBean(RealmModel realm, UserModel user, URI baseUri) {
        URI accountSocialUpdateUri = Urls.accountSocialUpdate(baseUri, realm.getName());
        this.socialLinks = new LinkedList<SocialLinkEntry>();

        Map<String, String> socialConfig = realm.getSocialConfig();
        Set<SocialLinkModel> userSocialLinks = realm.getSocialLinks(user);

        if (socialConfig != null && !socialConfig.isEmpty()) {
            for (SocialProvider provider : SocialLoader.load()) {
                String socialProviderId = provider.getId();
                if (socialConfig.containsKey(socialProviderId + ".key")) {
                    String socialUsername = getSocialUsername(userSocialLinks, socialProviderId);

                    String action = socialUsername!=null ? "remove" : "add";
                    String actionUrl = UriBuilder.fromUri(accountSocialUpdateUri).queryParam("action", action).queryParam("provider_id", socialProviderId).build().toString();

                    SocialLinkEntry entry = new SocialLinkEntry(socialProviderId, provider.getName(), socialUsername, actionUrl);
                    this.socialLinks.add(entry);
                }
            }
        }
    }

    private String getSocialUsername(Set<SocialLinkModel> userSocialLinks, String socialProviderId) {
        for (SocialLinkModel link : userSocialLinks) {
            if (socialProviderId.equals(link.getSocialProvider())) {
                return link.getSocialUsername();
            }
        }
        return null;
    }

    public List<SocialLinkEntry> getLinks() {
        return socialLinks;
    }

    public class SocialLinkEntry {

        private final String providerId;
        private final String providerName;
        private final String socialUsername;
        private final String actionUrl;

        public SocialLinkEntry(String providerId, String providerName, String socialUsername, String actionUrl) {
            this.providerId = providerId;
            this.providerName = providerName;
            this.socialUsername = socialUsername!=null ? socialUsername : "";
            this.actionUrl = actionUrl;
        }

        public String getProviderId() {
            return providerId;
        }

        public String getProviderName() {
            return providerName;
        }

        public String getSocialUsername() {
            return socialUsername;
        }

        public boolean isConnected() {
            return !socialUsername.isEmpty();
        }

        public String getActionUrl() {
            return actionUrl;
        }
    }
}
