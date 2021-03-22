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
package org.keycloak.forms.login.freemarker.model;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrderedModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.Urls;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class IdentityProviderBean {

    public static OrderedModel.OrderedModelComparator<IdentityProvider> IDP_COMPARATOR_INSTANCE = new OrderedModel.OrderedModelComparator<>();

    private boolean displaySocial;
    private List<IdentityProvider> providers;
    private RealmModel realm;
    private final KeycloakSession session;

    public IdentityProviderBean(RealmModel realm, KeycloakSession session, List<IdentityProviderModel> identityProviders, URI baseURI) {
        this.realm = realm;
        this.session = session;

        if (!identityProviders.isEmpty()) {
            List<IdentityProvider> orderedList = new ArrayList<>();
            for (IdentityProviderModel identityProvider : identityProviders) {
                if (identityProvider.isEnabled() && !identityProvider.isLinkOnly()) {
                    addIdentityProvider(orderedList, realm, baseURI, identityProvider);
                }
            }

            if (!orderedList.isEmpty()) {
                orderedList.sort(IDP_COMPARATOR_INSTANCE);
                providers = orderedList;
                displaySocial = true;
            }
        }
    }

    private void addIdentityProvider(List<IdentityProvider> orderedSet, RealmModel realm, URI baseURI, IdentityProviderModel identityProvider) {
        String loginUrl = Urls.identityProviderAuthnRequest(baseURI, identityProvider.getAlias(), realm.getName()).toString();
        String displayName = KeycloakModelUtils.getIdentityProviderDisplayName(session, identityProvider);
        Map<String, String> config = identityProvider.getConfig();
        boolean hideOnLoginPage = config != null && Boolean.parseBoolean(config.get("hideOnLoginPage"));
        if (!hideOnLoginPage) {
            orderedSet.add(new IdentityProvider(identityProvider.getAlias(),
                    displayName, identityProvider.getProviderId(), loginUrl,
                    config != null ? config.get("guiOrder") : null));
        }
    }

    public List<IdentityProvider> getProviders() {
        return providers;
    }

    public boolean isDisplayInfo() {
        return  realm.isRegistrationAllowed() || displaySocial;
    }

    public static class IdentityProvider implements OrderedModel {

        private final String alias;
        private final String providerId; // This refer to providerType (facebook, google, etc.)
        private final String loginUrl;
        private final String guiOrder;
        private final String displayName;

        public IdentityProvider(String alias, String displayName, String providerId, String loginUrl, String guiOrder) {
            this.alias = alias;
            this.displayName = displayName;
            this.providerId = providerId;
            this.loginUrl = loginUrl;
            this.guiOrder = guiOrder;
        }

        public String getAlias() {
            return alias;
        }

        public String getLoginUrl() {
            return loginUrl;
        }

        public String getProviderId() {
            return providerId;
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
