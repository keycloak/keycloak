/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.organization.authentication.authenticators.browser;

import static org.keycloak.authentication.AuthenticatorUtil.isSSOAuthentication;
import static org.keycloak.organization.utils.Organizations.getEmailDomain;
import static org.keycloak.organization.utils.Organizations.isEnabledAndOrganizationsPresent;
import static org.keycloak.organization.utils.Organizations.resolveHomeBroker;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticator;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.freemarker.model.AuthenticationContextBean;
import org.keycloak.forms.login.freemarker.model.IdentityProviderBean;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationModel.IdentityProviderRedirectMode;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.forms.login.freemarker.model.OrganizationAwareAuthenticationContextBean;
import org.keycloak.organization.forms.login.freemarker.model.OrganizationAwareIdentityProviderBean;
import org.keycloak.organization.forms.login.freemarker.model.OrganizationAwareRealmBean;
import org.keycloak.organization.protocol.mappers.oidc.OrganizationScope;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.sessions.AuthenticationSessionModel;

public class OrganizationAuthenticator extends IdentityProviderAuthenticator {

    private final KeycloakSession session;

    public OrganizationAuthenticator(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        OrganizationProvider provider = getOrganizationProvider();

        if (!isEnabledAndOrganizationsPresent(provider)) {
            context.attempted();
            return;
        }

        OrganizationModel organization = Organizations.resolveOrganization(session);

        if (organization == null) {
            initialChallenge(context);
        } else {
            // make sure the organization is set to the auth session to remember it when processing subsequent requests
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            authSession.setAuthNote(OrganizationModel.ORGANIZATION_ATTRIBUTE, organization.getId());
            action(context);
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        HttpRequest request = context.getHttpRequest();
        MultivaluedMap<String, String> parameters = request.getDecodedFormParameters();
        String username = parameters.getFirst(UserModel.USERNAME);
        RealmModel realm = context.getRealm();
        UserModel user = resolveUser(context, username);
        String domain = getEmailDomain(username);
        OrganizationModel organization = resolveOrganization(user, domain);

        if (organization == null) {
            if (shouldUserSelectOrganization(context, user)) {
                return;
            }
            // request does not map to any organization, go to the next step/sub-flow
            context.attempted();
            return;
        }

        // make sure the organization is set to the session to make it available to templates
        session.getContext().setOrganization(organization);

        if (tryRedirectBroker(context, organization, user, username, domain)) {
            return;
        }

        if (user == null) {
            unknownUserChallenge(context, organization, realm, domain != null);
            return;
        }

        // user exists, check if enabled
        if (!user.isEnabled()) {
            context.failure(AuthenticationFlowError.INVALID_USER);
            return;
        }

        if (isSSOAuthentication(context.getAuthenticationSession())) {
            // if re-authenticating in the scope of an organization
            context.success();
        } else {
            context.attempted();
        }
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return realm.isOrganizationsEnabled();
    }

    private OrganizationModel resolveOrganization(UserModel user, String domain) {
        KeycloakContext context = session.getContext();
        HttpRequest request = context.getHttpRequest();
        MultivaluedMap<String, String> parameters = request.getDecodedFormParameters();
        List<String> alias = parameters.getOrDefault(OrganizationModel.ORGANIZATION_ATTRIBUTE, List.of());
        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        if (alias.isEmpty()) {
            OrganizationModel organization = Organizations.resolveOrganization(session, user, domain);

            if (organization != null) {
                // make sure the organization selected by the user is available from the client session when running mappers and issuing tokens
                authSession.setClientNote(OrganizationModel.ORGANIZATION_ATTRIBUTE, organization.getId());
            }

            return organization;
        }

        OrganizationProvider provider = getOrganizationProvider();
        OrganizationModel organization = provider.getByAlias(alias.get(0));

        if (organization == null) {
            return null;
        }

        // make sure the organization selected by the user is available from the client session when running mappers and issuing tokens
        authSession.setClientNote(OrganizationModel.ORGANIZATION_ATTRIBUTE, organization.getId());

        return organization;
    }

    private boolean shouldUserSelectOrganization(AuthenticationFlowContext context, UserModel user) {
        OrganizationProvider provider = getOrganizationProvider();
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String rawScope = authSession.getClientNote(OAuth2Constants.SCOPE);
        OrganizationScope scope = OrganizationScope.valueOfScope(rawScope);

        if (!OrganizationScope.ANY.equals(scope) || user == null) {
            return false;
        }

        if (authSession.getClientNote(OrganizationModel.ORGANIZATION_ATTRIBUTE) != null) {
            // organization already selected
            return false;
        }

        Stream<OrganizationModel> organizations = provider.getByMember(user);

        if (organizations.count() > 1) {
            LoginFormsProvider form = context.form();
            form.setAttribute("user", new ProfileBean(user, session));
            form.setAttributeMapper(new Function<Map<String, Object>, Map<String, Object>>() {
                @Override
                public Map<String, Object> apply(Map<String, Object> attributes) {
                    attributes.computeIfPresent("auth",
                            (key, bean) -> new OrganizationAwareAuthenticationContextBean((AuthenticationContextBean) bean, false)
                    );
                    return attributes;
                }
            });
            context.challenge(form.createForm("select-organization.ftl"));
            return true;
        }

        return false;
    }

    private boolean tryRedirectBroker(AuthenticationFlowContext context, OrganizationModel organization, UserModel user, String username, String domain) {
        // the user has credentials set; do not redirect to allow the user to pick how to authenticate
        if (user != null && user.credentialManager().getStoredCredentialsStream().findAny().isPresent()) {
            return false;
        }

        List<IdentityProviderModel> broker = resolveHomeBroker(session, user);

        if (broker.size() == 1) {
            // user is a managed member and associated with a broker, redirect automatically
            redirect(context, broker.get(0).getAlias(), user.getEmail());
            return true;
        }

        return redirect(context, organization, username, domain);
    }

    private boolean redirect(AuthenticationFlowContext context, OrganizationModel organization, String username, String domain) {
        if (domain == null) {
            return false;
        }

        List<IdentityProviderModel> brokers = organization.getIdentityProviders().toList();

        for (IdentityProviderModel broker : brokers) {
            if (IdentityProviderRedirectMode.EMAIL_MATCH.isSet(broker)) {
                String idpDomain = broker.getConfig().get(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);

                if (domain.equals(idpDomain)) {
                    // redirect the user using the broker that matches the email domain
                    redirect(context, broker.getAlias(), username);
                    return true;
                }
            }
        }

        return false;
    }

    private UserModel resolveUser(AuthenticationFlowContext context, String username) {
        if (context.getUser() != null) {
            return context.getUser();
        }

        if (username == null) {
            return null;
        }

        UserProvider users = session.users();
        RealmModel realm = session.getContext().getRealm();
        UserModel user = Optional.ofNullable(users.getUserByEmail(realm, username)).orElseGet(() -> users.getUserByUsername(realm, username));

        context.setUser(user);

        return user;
    }

    private void unknownUserChallenge(AuthenticationFlowContext context, OrganizationModel organization, RealmModel realm, boolean domainMatch) {
        // the user does not exist and is authenticating in the scope of the organization, show the identity-first login page and the
        // public organization brokers for selection
        LoginFormsProvider form = context.form()
                .setAttributeMapper(attributes -> {
                    if (hasPublicBrokers(organization)) {
                        attributes.computeIfPresent("social",
                                (key, bean) -> new OrganizationAwareIdentityProviderBean((IdentityProviderBean) bean, true)
                        );
                        // do not show the self-registration link if there are public brokers available from the organization to force the user to register using a broker
                        attributes.computeIfPresent("realm",
                                (key, bean) -> new OrganizationAwareRealmBean(realm)
                        );
                    } else {
                        attributes.computeIfPresent("social",
                                (key, bean) -> new OrganizationAwareIdentityProviderBean((IdentityProviderBean) bean, false, true)
                        );
                    }

                    attributes.computeIfPresent("auth",
                            (key, bean) -> new OrganizationAwareAuthenticationContextBean((AuthenticationContextBean) bean, false)
                    );

                    return attributes;
                });

        if (domainMatch) {
            form.addError(new FormMessage("Your email domain matches the " + organization.getName() + " organization but you don't have an account yet."));
        }

        context.challenge(form.createLoginUsername());
    }

    private void initialChallenge(AuthenticationFlowContext context) {
        UserModel user = context.getUser();

        if (user == null) {
            // the default challenge won't show any broker but just the identity-first login page and the option to try a different authentication mechanism
            LoginFormsProvider form = context.form()
                    .setAttributeMapper(attributes -> {
                        attributes.computeIfPresent("social",
                                (key, bean) -> new OrganizationAwareIdentityProviderBean((IdentityProviderBean) bean, false, true)
                        );
                        attributes.computeIfPresent("auth",
                                (key, bean) -> new OrganizationAwareAuthenticationContextBean((AuthenticationContextBean) bean, false)
                        );
                        return attributes;
                    });

            context.challenge(form.createLoginUsername());
        } else if (isSSOAuthentication(context.getAuthenticationSession())) {
            if (shouldUserSelectOrganization(context, user)) {
                return;
            }

            // user is re-authenticating and there are no organizations to select
            context.success();
        }
    }

    private boolean hasPublicBrokers(OrganizationModel organization) {
        return organization.getIdentityProviders().anyMatch(Predicate.not(IdentityProviderModel::isHideOnLogin));
    }

    private OrganizationProvider getOrganizationProvider() {
        return session.getProvider(OrganizationProvider.class);
    }
}
