package org.keycloak.account.freemarker.model;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.AccountService;
import org.keycloak.services.resources.flows.Urls;
import org.keycloak.social.SocialLoader;
import org.keycloak.social.SocialProvider;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AccountSocialBean {

    private final List<SocialLinkEntry> socialLinks;
    private final boolean removeLinkPossible;
    private final KeycloakSession session;

    public AccountSocialBean(KeycloakSession session, RealmModel realm, UserModel user, URI baseUri, String stateChecker) {
        this.session = session;
        URI accountSocialUpdateUri = Urls.accountSocialUpdate(baseUri, realm.getName());
        this.socialLinks = new LinkedList<SocialLinkEntry>();

        Map<String, String> socialConfig = realm.getSocialConfig();
        Set<SocialLinkModel> userSocialLinks = session.users().getSocialLinks(user, realm);

        int availableLinks = 0;
        if (socialConfig != null && !socialConfig.isEmpty()) {
            for (SocialProvider provider : SocialLoader.load()) {
                String socialProviderId = provider.getId();
                if (socialConfig.containsKey(socialProviderId + ".key")) {
                    SocialLinkModel socialLink = getSocialLink(userSocialLinks, socialProviderId);

                    if (socialLink != null) {
                        availableLinks++;
                    }
                    String action = socialLink != null ? "remove" : "add";
                    String actionUrl = UriBuilder.fromUri(accountSocialUpdateUri)
                            .queryParam("action", action)
                            .queryParam("provider_id", socialProviderId)
                            .queryParam("stateChecker", stateChecker)
                            .build().toString();

                    SocialLinkEntry entry = new SocialLinkEntry(socialLink, provider.getName(), actionUrl);
                    this.socialLinks.add(entry);
                }
            }
        }

        // Removing last social provider is not possible if you don't have other possibility to authenticate
        this.removeLinkPossible = availableLinks > 1 || user.getFederationLink() != null || AccountService.isPasswordSet(user);
    }

    private SocialLinkModel getSocialLink(Set<SocialLinkModel> userSocialLinks, String socialProviderId) {
        for (SocialLinkModel link : userSocialLinks) {
            if (socialProviderId.equals(link.getSocialProvider())) {
                return link;
            }
        }
        return null;
    }

    public List<SocialLinkEntry> getLinks() {
        return socialLinks;
    }

    public boolean isRemoveLinkPossible() {
        return removeLinkPossible;
    }

    public class SocialLinkEntry {

        private SocialLinkModel link;
        private final String providerName;
        private final String actionUrl;

        public SocialLinkEntry(SocialLinkModel link, String providerName, String actionUrl) {
            this.link = link;
            this.providerName = providerName;
            this.actionUrl = actionUrl;
        }

        public String getProviderId() {
            return link != null ? link.getSocialProvider() : null;
        }

        public String getProviderName() {
            return providerName;
        }

        public String getSocialUserId() {
            return link != null ? link.getSocialUserId() : null;
        }

        public String getSocialUsername() {
            return link != null ? link.getSocialUsername() : null;
        }

        public boolean isConnected() {
            return link != null;
        }

        public String getActionUrl() {
            return actionUrl;
        }
    }
}
