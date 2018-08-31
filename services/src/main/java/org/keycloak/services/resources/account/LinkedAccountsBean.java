/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.resources.account;

import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.keycloak.credential.CredentialModel;
import org.keycloak.representations.account.LinkedAccountRepresentation;

/**
 * @author Stan Silvert
 */
public class LinkedAccountsBean {

    private final Set<LinkedAccountRepresentation> linkedAccounts;
    private final boolean hasAlternativeAuthentication;

    public LinkedAccountsBean(KeycloakSession session, RealmModel realm, UserModel user) {
        List<IdentityProviderModel> identityProviders = realm.getIdentityProviders();
        Set<FederatedIdentityModel> identities = session.users().getFederatedIdentities(user, realm);

        this.linkedAccounts = new HashSet<>();
        if (identityProviders != null && !identityProviders.isEmpty()) {
            for (IdentityProviderModel provider : identityProviders) {
                if (!provider.isEnabled()) {
                    continue;
                }
                String providerId = provider.getAlias();

                FederatedIdentityModel identity = getIdentity(identities, providerId);

                String displayName = KeycloakModelUtils.getIdentityProviderDisplayName(session, provider);
                String guiOrder = provider.getConfig() != null ? provider.getConfig().get("guiOrder") : null;
                
                LinkedAccountRepresentation rep = new LinkedAccountRepresentation();
                rep.setConnected(identity != null);
                rep.setProviderId(providerId);
                rep.setDisplayName(displayName);
                rep.setGuiOrder(guiOrder);
                rep.setProviderName(provider.getAlias());
                if (identity != null) {
                    rep.setUserId(identity.getUserId());
                    rep.setUserName(identity.getUserName());
                }
                this.linkedAccounts.add(rep);
            }
        }

        this.hasAlternativeAuthentication = session.userCredentialManager().isConfiguredFor(realm, user, CredentialModel.PASSWORD);
    }

    private FederatedIdentityModel getIdentity(Set<FederatedIdentityModel> identities, String providerId) {
        for (FederatedIdentityModel link : identities) {
            if (providerId.equals(link.getIdentityProvider())) {
                return link;
            }
        }
        return null;
    }

    public Set<LinkedAccountRepresentation> getLinkedAccounts() {
        return linkedAccounts;
    }

    public boolean getHasAlternativeAuthentication() {
        return this.hasAlternativeAuthentication;
    }

}
