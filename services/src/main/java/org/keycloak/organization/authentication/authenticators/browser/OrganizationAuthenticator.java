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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.FlowStatus;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticator;
import org.keycloak.authentication.authenticators.browser.WebAuthnConditionalUIAuthenticator;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.freemarker.model.AuthenticationContextBean;
import org.keycloak.forms.login.freemarker.model.IdentityProviderBean;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationModel.IdentityProviderRedirectMode;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.forms.login.freemarker.model.OrganizationAwareAuthenticationContextBean;
import org.keycloak.organization.forms.login.freemarker.model.OrganizationAwareIdentityProviderBean;
import org.keycloak.organization.forms.login.freemarker.model.OrganizationAwareRealmBean;
import org.keycloak.organization.protocol.mappers.oidc.OrganizationScope;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.Booleans;

import static org.keycloak.authentication.AuthenticatorUtil.isSSOAuthentication;
import static org.keycloak.models.OrganizationDomainModel.ANY_DOMAIN;
import static org.keycloak.models.utils.KeycloakModelUtils.findUserByNameOrEmail;
import static org.keycloak.organization.utils.Organizations.getEmailDomain;
import static org.keycloak.organization.utils.Organizations.isEnabledAndOrganizationsPresent;
import static org.keycloak.organization.utils.Organizations.resolveHomeBroker;
import static org.keycloak.utils.StringUtil.isBlank;

public class OrganizationAuthenticator extends IdentityProviderAuthenticator {

    private final KeycloakSession session;
    private final WebAuthnConditionalUIAuthenticator webauthnAuth;

    public OrganizationAuthenticator(KeycloakSession session) {
        this.session = session;
        this.webauthnAuth = new WebAuthnConditionalUIAuthenticator(session, (context) -> createLoginForm(context));
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        OrganizationProvider provider = getOrganizationProvider();

        if (!isEnabledAndOrganizationsPresent(provider)) {
            attempted(context);
            return;
        }

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String loginHint = authSession.getClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM);
        OrganizationModel organization = Organizations.resolveOrganization(session);

        if (loginHint == null && organization == null) {
            initialChallenge(context);
            return;
        }

        if (organization != null) {
            // make sure the organization is set to the auth session to remember it when processing subsequent requests
            authSession.setAuthNote(OrganizationModel.ORGANIZATION_ATTRIBUTE, organization.getId());
        }

        action(context, loginHint);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        HttpRequest request = context.getHttpRequest();
        MultivaluedMap<String, String> parameters = request.getDecodedFormParameters();
        String username = parameters.getFirst(UserModel.USERNAME);

        // check if it's a webauthn submission and perform the webauth login
        if (webauthnAuth.isPasskeysEnabled() && (parameters.containsKey(WebAuthnConstants.AUTHENTICATOR_DATA)
                || parameters.containsKey(WebAuthnConstants.ERROR))) {
            webauthnAuth.action(context);
            if (FlowStatus.SUCCESS != context.getStatus()) {
                // if failure doing webauthn authentication return error; continue if success checking organizations
                return;
            }
        }

        UserModel user = context.getUser();

        if (user == null && isBlank(username)) {
            initialChallenge(context, form -> {
                form.addError(new FormMessage(UserModel.USERNAME, Messages.INVALID_USERNAME));
                return form.createLoginUsername();
            });
            return;
        }

        action(context, username);
    }

    private void action(AuthenticationFlowContext context, String username) {
        UserModel user = resolveUser(context, username);
        RealmModel realm = context.getRealm();
        String domain = getEmailDomain(username);
        OrganizationModel organization = resolveOrganization(user, domain);

        if (organization == null) {
            if (shouldUserSelectOrganization(context, user)) {
                return;
            }

            if (isMembershipRequired(context, null, user)) {
                return;
            }

            clearAuthenticationSession(context);
            // request does not map to any organization, go to the next step/sub-flow
            attempted(context, username);
            return;
        }

        // remember the organization during the lifetime of the authentication session
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        authSession.setAuthNote(OrganizationModel.ORGANIZATION_ATTRIBUTE, organization.getId());
        // make sure the organization is set to the session to make it available to templates
        session.getContext().setOrganization(organization);

        if (isMembershipRequired(context, organization, user)) {
            return;
        }

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

        if (isSSOAuthentication(authSession)) {
            // if re-authenticating in the scope of an organization
            context.success();
        } else {
            attempted(context);
        }
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return realm.isOrganizationsEnabled();
    }

    private OrganizationModel resolveOrganization(UserModel user, String domain) {
        KeycloakContext context = session.getContext();
        HttpRequest request = context.getHttpRequest();
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        MultivaluedMap<String, String> parameters = request.getDecodedFormParameters();
        // parameter from the organization selection page
        List<String> alias = parameters.getOrDefault(OrganizationModel.ORGANIZATION_ATTRIBUTE, List.of());

        if (alias.isEmpty()) {
            OrganizationModel organization = Organizations.resolveOrganization(session, user, domain);

            if (isSSOAuthentication(authSession) && organization != null) {
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
        if (user == null || !OrganizationScope.ANY.equals(OrganizationScope.valueOfScope(session))) {
            return false;
        }

        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        if (authSession.getClientNote(OrganizationModel.ORGANIZATION_ATTRIBUTE) != null) {
            // organization already selected
            return false;
        }

        OrganizationProvider provider = getOrganizationProvider();
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
            clearAuthenticationSession(context);
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

        domain = domain == null ? getEmailDomain(user) : domain;

        return redirect(context, organization, username, domain);
    }

    private boolean redirect(AuthenticationFlowContext context, OrganizationModel organization, String username, String domain) {
        if (domain == null) {
            return false;
        }

        // first look for an IDP that matches exactly the specified domain (case-insensitive)
        IdentityProviderModel idp = organization.getIdentityProviders()
                .filter(broker -> IdentityProviderRedirectMode.EMAIL_MATCH.isSet(broker) &&
                    domain.equalsIgnoreCase(broker.getConfig().get(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE))).findFirst().orElse(null);

        if (idp != null) {
            // redirect the user using the broker that matches the specified domain
            redirect(context, idp.getAlias(), username);
            return true;
        }

        // look for an idp that can match any of the org domains
        idp = organization.getIdentityProviders().filter(IdentityProviderRedirectMode.EMAIL_MATCH::isSet)
                .filter(broker -> ANY_DOMAIN.equals(broker.getConfig().get(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE)))
                .filter(broker -> organization.getDomains().map(OrganizationDomainModel::getName).anyMatch(domain::equals))
                .findFirst().orElse(null);

        if (idp != null) {
            redirect(context, idp.getAlias(), username);
            return true;
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

        RealmModel realm = session.getContext().getRealm();
        UserModel user = findUserByNameOrEmail(session, realm, username);

        // make sure the organization will be resolved based on the username provided
        clearAuthenticationSession(context);
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
        initialChallenge(context, null);
    }

    private void initialChallenge(AuthenticationFlowContext context, Function<LoginFormsProvider, Response> formCreator) {
        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();
        UserModel user = context.getUser();

        if (user == null) {
            // setup webauthn data when the user is not already selected
            if (webauthnAuth.isPasskeysEnabled()) {
                webauthnAuth.fillContextForm(context);
            }

            context.challenge(createLoginForm(context, formCreator));
        } else if (isSSOAuthentication(authenticationSession)) {
            if (shouldUserSelectOrganization(context, user)) {
                return;
            }

            // user is re-authenticating, and there are no organizations to select
            context.success();
        } else {
            // user is re-authenticating, there is no organization to process
            attempted(context, user.getUsername());
        }
    }

    private Response createLoginForm(AuthenticationFlowContext context) {
        return createLoginForm(context, null);
    }

    private Response createLoginForm(AuthenticationFlowContext context, Function<LoginFormsProvider, Response> formCreator) {
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

        String loginHint = context.getAuthenticationSession().getClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM);

        if (loginHint != null) {
            form.setFormData(new MultivaluedHashMap<>(Map.of(UserModel.USERNAME, loginHint)));
        }

        return formCreator == null ? form.createLoginUsername() : formCreator.apply(form);
    }

    private void attempted(AuthenticationFlowContext context) {
        attempted(context, null);
    }

    private void attempted(AuthenticationFlowContext context, String username) {
        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();

        if (username != null) {
            authenticationSession.setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, username);
            authenticationSession.setAuthNote(AbstractUsernameFormAuthenticator.USERNAME_HIDDEN, Boolean.TRUE.toString());
        }

        context.attempted();
    }

    private boolean hasPublicBrokers(OrganizationModel organization) {
        return organization.getIdentityProviders().anyMatch(i -> Booleans.isFalse(i.isHideOnLogin()));
    }

    private OrganizationProvider getOrganizationProvider() {
        return session.getProvider(OrganizationProvider.class);
    }

    private boolean isRequiresMembership(AuthenticationFlowContext context) {
        return Boolean.parseBoolean(getConfig(context).getOrDefault(OrganizationAuthenticatorFactory.REQUIRES_USER_MEMBERSHIP, Boolean.FALSE.toString()));
    }

    private Map<String, String> getConfig(AuthenticationFlowContext context) {
        return Optional.ofNullable(context.getAuthenticatorConfig()).map(AuthenticatorConfigModel::getConfig).orElse(Map.of());
    }

    private void clearAuthenticationSession(AuthenticationFlowContext context) {
        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();
        authenticationSession.removeAuthNote(OrganizationModel.ORGANIZATION_ATTRIBUTE);
    }

    private boolean isMembershipRequired(AuthenticationFlowContext context, OrganizationModel organization, UserModel user) {
        if (user == null || !isRequiresMembership(context)) {
            return false;
        }

        if (organization == null) {
            OrganizationScope scope = OrganizationScope.valueOfScope(session);

            if (OrganizationScope.SINGLE.equals(scope)) {
                organization = scope.resolveOrganizations(session).findAny().orElse(null);
            }
        }

        if (organization != null && organization.isMember(user)) {
            return false;
        }

        // do not show try another way
        context.setAuthenticationSelections(List.of());

        LoginFormsProvider form = context.form();
        String errorMessage;
        String failureMessage;

        if (organization == null) {
            errorMessage = "notMemberOfAnyOrganization";
            failureMessage = "User " + user.getUsername() + " not a member of any organization";
            form.setError(errorMessage);
        } else {
            errorMessage = "notMemberOfOrganization";
            failureMessage = "User " + user.getUsername() + " not a member of organization " + organization.getAlias();
            form.setError(errorMessage, organization.getName());
        }

        context.failure(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR,
                form.createErrorPage(Response.Status.FORBIDDEN),
                failureMessage, errorMessage);

        return true;
    }
}
