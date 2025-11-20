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

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.common.Profile;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderStorageProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrderedModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.Theme;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class IdentityProviderBean {

    public static OrderedModel.OrderedModelComparator<IdentityProvider> IDP_COMPARATOR_INSTANCE = new OrderedModel.OrderedModelComparator<>();
    private static final String ICON_THEME_PREFIX = "kcLogoIdP-";
    private static final String IDP_THEME_CONFIG_PREFIX = "kcTheme-";

    protected AuthenticationFlowContext context;
    protected List<IdentityProvider> providers;
    protected KeycloakSession session;
    protected RealmModel realm;
    protected URI baseURI;

    public IdentityProviderBean(KeycloakSession session, RealmModel realm, URI baseURI, AuthenticationFlowContext context) {
        this.session = session;
        this.realm = realm;
        this.baseURI = baseURI;
        this.context = context;
    }

    public List<IdentityProvider> getProviders() {
        if (this.providers == null) {
            String existingIDP = this.getExistingIDP(session, context);
            Set<String> federatedIdentities = this.getLinkedBrokerAliases(session, realm, context);
            if (federatedIdentities != null) {
                this.providers = getFederatedIdentityProviders(federatedIdentities, existingIDP);
            } else {
                this.providers = searchForIdentityProviders(existingIDP);
            }
        }
        return this.providers;
    }

    public KeycloakSession getSession() {
        return this.session;
    }

    public RealmModel getRealm() {
        return this.realm;
    }

    public URI getBaseURI() {
        return this.baseURI;
    }

    public AuthenticationFlowContext getFlowContext() {
        return this.context;
    }

    /**
     * Creates an {@link IdentityProvider} instance from the specified {@link IdentityProviderModel}.
     *
     * @param realm a reference to the realm.
     * @param baseURI the base URI.
     * @param identityProvider the {@link IdentityProviderModel} from which the freemarker {@link IdentityProvider} is
     *                         to be built.
     * @return the constructed {@link IdentityProvider}.
     */
    protected IdentityProvider createIdentityProvider(RealmModel realm, URI baseURI, IdentityProviderModel identityProvider) {
        String loginUrl = Urls.identityProviderAuthnRequest(baseURI, identityProvider.getAlias(), realm.getName()).toString();
        String displayName = KeycloakModelUtils.getIdentityProviderDisplayName(session, identityProvider);
        Map<String, String> themeConfig = identityProvider.getConfig().entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(IDP_THEME_CONFIG_PREFIX))
            .collect(Collectors.toMap(
                entry -> entry.getKey().substring(IDP_THEME_CONFIG_PREFIX.length()),
                Map.Entry::getValue
                 ));
        return new IdentityProvider(identityProvider.getAlias(),
                displayName, identityProvider.getProviderId(), loginUrl,
                identityProvider.getConfig().get("guiOrder"), getLoginIconClasses(identityProvider), themeConfig);
    }

    // Get icon classes defined in properties of current theme with key 'kcLogoIdP-{alias}'
    // OR from IdentityProviderModel.getDisplayIconClasses if not defined in theme (for third-party IDPs like Sign-In-With-Apple)
    // f.e. kcLogoIdP-github = fa fa-github
    private String getLoginIconClasses(IdentityProviderModel identityProvider) {
        try {
            Theme theme = session.theme().getTheme(Theme.Type.LOGIN);
            Optional<String> classesFromTheme = Optional.ofNullable(getLogoIconClass(identityProvider, theme.getProperties()));
            Optional<String> classesFromModel = Optional.ofNullable(identityProvider.getDisplayIconClasses());
            return classesFromTheme.orElse(classesFromModel.orElse(""));
        } catch (IOException e) {
            //NOP
        }
        return "";
    }

    private String getLogoIconClass(IdentityProviderModel identityProvider, Properties themeProperties) throws IOException {
        String iconClass = themeProperties.getProperty(ICON_THEME_PREFIX + identityProvider.getAlias());

        if (iconClass == null) {
            return themeProperties.getProperty(ICON_THEME_PREFIX + identityProvider.getProviderId());
        }

        return iconClass;
    }

    /**
     * Checks if an IDP is being connected to the user's account. In this case the currentUser is {@code null} and the current flow
     * is the {@code FIRST_BROKER_LOGIN_PATH}, so we should retrieve the IDP they used for login and filter it out of the list
     * of IDPs that are available for login. (GHI #14173).
     *
     * @param session a reference to the {@link KeycloakSession}.
     * @param context a reference to the {@link AuthenticationFlowContext}.
     * @return the alias of the IDP used for login before linking a new IDP to the user's account (if any).
     */
    protected String getExistingIDP(KeycloakSession session, AuthenticationFlowContext context) {

        String existingIDPAlias = null;
        if (context != null) {
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            String currentFlowPath = authSession.getAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH);
            UserModel currentUser = context.getUser();

            if (currentUser == null && Objects.equals(LoginActionsService.FIRST_BROKER_LOGIN_PATH, currentFlowPath)) {
                SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromAuthenticationSession(authSession, AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);
                final IdentityProviderModel existingIdp = (serializedCtx == null) ? null : serializedCtx.deserialize(session, authSession).getIdpConfig();
                if (existingIdp != null) {
                    existingIDPAlias = existingIdp.getAlias();
                }
            }
        }
        return existingIDPAlias;
    }

    /**
     * Returns the list of IDPs linked with the user's federated identities, if any. In case these IDPs exist, the login
     * page should show only the IDPs already linked to the user. Returning {@code null} indicates that all public enabled IDPs
     * should be available.
     * </p>
     * Returning an empty set essentially narrows the list of available IDPs to zero, so no IDPs will be shown for login.
     *
     * @param session a reference to the {@link KeycloakSession}.
     * @param realm a reference to the realm.
     * @param context a reference to the {@link AuthenticationFlowContext}.
     * @return a {@link Set} containing the aliases of the IDPs that should be available for login. An empty set indicates
     * that no IDPs should be available.
     */
    protected Set<String> getLinkedBrokerAliases(KeycloakSession session, RealmModel realm, AuthenticationFlowContext context) {
        Set<String> result = null;
        if (context != null) {
            UserModel currentUser = context.getUser();
            if (currentUser != null) {
                Set<String> federatedIdentities = session.users().getFederatedIdentitiesStream(session.getContext().getRealm(), currentUser)
                        .map(FederatedIdentityModel::getIdentityProvider)
                        .collect(Collectors.toSet());

                if (!federatedIdentities.isEmpty() || organizationsDisabled(realm))
                    // if orgs are enabled, we don't want to return an empty set - we want the organization IDPs to be shown if those are available.
                    result = new HashSet<>(federatedIdentities);

            }
        }
        return result;
    }

    /**
     * Builds and returns a list of {@link IdentityProvider} instances from the specified set of federated IDPs. The IDPs
     * must be enabled, not link-only, and not set to be hidden on login page. If any IDP has an alias that matches the
     * {@code existingIDP} parameter, it must be filtered out.
     *
     * @param federatedProviders a {@link Set} containing the aliases of the federated IDPs that should be considered for login.
     * @param existingIDP the alias of the IDP that must be filtered out from the result (used when linking a new IDP to a user's account).
     * @return a {@link List} containing the constructed {@link IdentityProvider}s.
     */
    protected List<IdentityProvider> getFederatedIdentityProviders(Set<String> federatedProviders, String existingIDP) {
        return federatedProviders.stream()
                .filter(alias -> !Objects.equals(existingIDP, alias))
                .map(alias -> session.identityProviders().getByAlias(alias))
                .filter(federatedProviderPredicate())
                .map(idp -> createIdentityProvider(this.realm, this.baseURI, idp))
                .sorted(IDP_COMPARATOR_INSTANCE).toList();
    }

    /**
     * Returns a predicate that can filter out IDPs associated with the current user's federated identities before those
     * are converted into {@link IdentityProvider}s. Subclasses may use this as a way to further refine the IDPs that are
     * to be returned.
     *
     * @return the custom {@link Predicate} used as a last filter before conversion into {@link IdentityProvider}
     */
    protected Predicate<IdentityProviderModel> federatedProviderPredicate() {
        return IdentityProviderStorageProvider.LoginFilter.getLoginPredicate();
    }

    /**
     * Builds and returns a list of {@link IdentityProvider} instances that will be available for login. This method goes
     * to the {@link IdentityProviderStorageProvider} to fetch the IDPs that can be used for login (enabled, not link-only and not set to be
     * hidden on login page).
     *
     * @param existingIDP the alias of the IDP that must be filtered out from the result (used when linking a new IDP to a user's account).
     * @return a {@link List} containing the constructed {@link IdentityProvider}s.
     */
    protected List<IdentityProvider> searchForIdentityProviders(String existingIDP) {
        return session.identityProviders().getForLogin(IdentityProviderStorageProvider.FetchMode.REALM_ONLY, null)
                .filter(idp -> !Objects.equals(existingIDP, idp.getAlias()))
                .map(idp -> createIdentityProvider(this.realm, this.baseURI, idp))
                .sorted(IDP_COMPARATOR_INSTANCE).toList();
    }

    private static boolean organizationsDisabled(RealmModel realm) {
        return !Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION) || !realm.isOrganizationsEnabled();
    }

    public static class IdentityProvider implements OrderedModel {

        private final String alias;
        private final String providerId; // This refers to providerType (facebook, google, etc.)
        private final String loginUrl;
        private final String guiOrder;
        private final String displayName;
        private final String iconClasses;
        private final Map<String, String> themeConfig;

        public IdentityProvider(String alias, String displayName, String providerId, String loginUrl, String guiOrder) {
            this(alias, displayName, providerId, loginUrl, guiOrder, "", null);
        }

        public IdentityProvider(String alias, String displayName, String providerId, String loginUrl, String guiOrder, String iconClasses, Map<String, String> themeConfig) {
            this.alias = alias;
            this.displayName = displayName;
            this.providerId = providerId;
            this.loginUrl = loginUrl;
            this.guiOrder = guiOrder;
            this.iconClasses = iconClasses;
            this.themeConfig = themeConfig;
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

        public Map<String, String> getThemeConfig() {
            return themeConfig;
        }
    }


}
