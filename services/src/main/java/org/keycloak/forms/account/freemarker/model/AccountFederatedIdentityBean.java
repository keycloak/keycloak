/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.forms.account.freemarker.model;

import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrderedModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.resources.account.AccountFormService;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:velias@redhat.com">Vlastimil Elias</a>
 */
public class AccountFederatedIdentityBean {

    private static OrderedModel.OrderedModelComparator<FederatedIdentityEntry> IDP_COMPARATOR_INSTANCE = new OrderedModel.OrderedModelComparator<>();

    private final List<FederatedIdentityEntry> identities = new ArrayList<>();
    private final boolean removeLinkPossible;
    private final KeycloakSession session;

    public AccountFederatedIdentityBean(KeycloakSession session, RealmModel realm, UserModel user, URI baseUri, String stateChecker) {
        this.session = session;

        List<IdentityProviderModel> identityProviders = realm.getIdentityProviders();
        Set<FederatedIdentityModel> identities = session.users().getFederatedIdentities(user, realm);

        int availableIdentities = 0;
        if (identityProviders != null && !identityProviders.isEmpty()) {
            for (IdentityProviderModel provider : identityProviders) {
                if (!provider.isEnabled()) {
                    continue;
                }
                String providerId = provider.getAlias();

                FederatedIdentityModel identity = getIdentity(identities, providerId);

                if (identity != null) {
                    availableIdentities++;
                }

                String displayName = KeycloakModelUtils.getIdentityProviderDisplayName(session, provider);
                FederatedIdentityEntry entry = new FederatedIdentityEntry(identity, displayName, provider.getAlias(), provider.getAlias(),
                		  															provider.getConfig() != null ? provider.getConfig().get("guiOrder") : null);
                this.identities.add(entry);
            }
        }
        
        this.identities.sort(IDP_COMPARATOR_INSTANCE);

        // Removing last social provider is not possible if you don't have other possibility to authenticate
        this.removeLinkPossible = availableIdentities > 1 || user.getFederationLink() != null || AccountFormService.isPasswordSet(session, realm, user);
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

    public class FederatedIdentityEntry implements OrderedModel {

        private FederatedIdentityModel federatedIdentityModel;
        private final String providerId;
		private final String providerName;
        private final String guiOrder;
        private final String displayName;

        public FederatedIdentityEntry(FederatedIdentityModel federatedIdentityModel, String displayName, String providerId,
                                      String providerName, String guiOrder) {
            this.federatedIdentityModel = federatedIdentityModel;
            this.displayName = displayName;
            this.providerId = providerId;
            this.providerName = providerName;
            this.guiOrder = guiOrder;
        }

        public String getProviderId() {
            return providerId;
        }
        
        public String getProviderName() {
          return providerName;
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

        @Override
        public String getGuiOrder() {
            return guiOrder;
        }

        public String getDisplayName() {
            return displayName;
        }

    }

}
