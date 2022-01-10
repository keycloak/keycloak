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
import org.keycloak.theme.Theme;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                    config != null ? config.get("guiOrder") : null, getLoginIconClasses(identityProvider.getAlias())));
        }
    }

    // Get icon classes defined in properties of current theme with key 'kcLogoIdP-{alias}'
    // f.e. kcLogoIdP-github = fa fa-github
    private String getLoginIconClasses(String alias) {
        final String ICON_THEME_PREFIX = "kcLogoIdP-";

        try {
            Theme theme = session.theme().getTheme(Theme.Type.LOGIN);
            return Optional.ofNullable(theme.getProperties().getProperty(ICON_THEME_PREFIX + alias)).orElse("");
        } catch (IOException e) {
            //NOP
        }
        return "";
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
        private final String iconClasses;

        public IdentityProvider(String alias, String displayName, String providerId, String loginUrl, String guiOrder) {
            this(alias, displayName, providerId, loginUrl, guiOrder, "");
        }

        public IdentityProvider(String alias, String displayName, String providerId, String loginUrl, String guiOrder, String iconClasses) {
            this.alias = alias;
            this.displayName = displayName;
            this.providerId = providerId;
            this.loginUrl = loginUrl;
            this.guiOrder = guiOrder;
            this.iconClasses = iconClasses;
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

        public String getIconClasses() {
            return iconClasses;
        }
    }

}
