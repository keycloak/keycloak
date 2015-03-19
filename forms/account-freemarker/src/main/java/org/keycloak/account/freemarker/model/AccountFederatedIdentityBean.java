package org.keycloak.account.freemarker.model;

import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.AccountService;
import org.keycloak.services.resources.flows.Urls;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AccountFederatedIdentityBean {

    private final List<FederatedIdentityEntry> identities;
    private final boolean removeLinkPossible;
    private final KeycloakSession session;

    public AccountFederatedIdentityBean(KeycloakSession session, RealmModel realm, UserModel user, URI baseUri, String stateChecker) {
        this.session = session;
        URI accountIdentityUpdateUri = Urls.accountFederatedIdentityUpdate(baseUri, realm.getName());
        this.identities = new LinkedList<FederatedIdentityEntry>();

        List<IdentityProviderModel> identityProviders = realm.getIdentityProviders();
        Set<FederatedIdentityModel> identities = session.users().getFederatedIdentities(user, realm);

        int availableIdentities = 0;
        if (identityProviders != null && !identityProviders.isEmpty()) {
            for (IdentityProviderModel provider : identityProviders) {
                String providerId = provider.getAlias();

                FederatedIdentityModel identity = getIdentity(identities, providerId);

                if (identity != null) {
                    availableIdentities++;
                }

                String action = identity != null ? "remove" : "add";
                String actionUrl = UriBuilder.fromUri(accountIdentityUpdateUri)
                        .queryParam("action", action)
                        .queryParam("provider_id", providerId)
                        .queryParam("stateChecker", stateChecker)
                        .build().toString();

                FederatedIdentityEntry entry = new FederatedIdentityEntry(identity, provider.getAlias(), actionUrl);
                this.identities.add(entry);
            }
        }

        // Removing last social provider is not possible if you don't have other possibility to authenticate
        this.removeLinkPossible = availableIdentities > 1 || user.getFederationLink() != null || AccountService.isPasswordSet(user);
    }

    private FederatedIdentityModel getIdentity(Set<FederatedIdentityModel> identities, String providerId) {
        for (FederatedIdentityModel link : identities) {
            if (providerId.equals(link.getIdentityProvider())) {
                return link;
            }
        }
        return null;
    }

    public List<FederatedIdentityEntry> getIdentities() {
        return identities;
    }

    public boolean isRemoveLinkPossible() {
        return removeLinkPossible;
    }

    public class FederatedIdentityEntry {

        private FederatedIdentityModel federatedIdentityModel;
        private final String providerId;
        private final String actionUrl;

        public FederatedIdentityEntry(FederatedIdentityModel federatedIdentityModel, String providerId, String actionUrl) {
            this.federatedIdentityModel = federatedIdentityModel;
            this.providerId = providerId;
            this.actionUrl = actionUrl;
        }

        public String getProviderId() {
            return providerId;
        }

        public String getUserId() {
            return federatedIdentityModel != null ? federatedIdentityModel.getUserId() : null;
        }

        public String getUserName() {
            return federatedIdentityModel != null ? federatedIdentityModel.getUserName() : null;
        }

        public boolean isConnected() {
            return federatedIdentityModel != null;
        }

        public String getActionUrl() {
            return actionUrl;
        }
    }
}
