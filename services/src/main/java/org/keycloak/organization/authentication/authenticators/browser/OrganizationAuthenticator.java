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

import static org.keycloak.organization.utils.Organizations.isEnabledAndOrganizationsPresent;
import static org.keycloak.organization.utils.Organizations.resolveBroker;

import java.util.List;

import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.freemarker.model.AuthenticationContextBean;
import org.keycloak.forms.login.freemarker.model.IdentityProviderBean;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationModel.IdentityProviderRedirectMode;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.forms.login.freemarker.model.OrganizationAwareAuthenticationContextBean;
import org.keycloak.organization.forms.login.freemarker.model.OrganizationAwareIdentityProviderBean;
import org.keycloak.organization.forms.login.freemarker.model.OrganizationAwareRealmBean;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;

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

        challenge(context);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        HttpRequest request = context.getHttpRequest();
        MultivaluedMap<String, String> parameters = request.getDecodedFormParameters();
        String username = parameters.getFirst(UserModel.USERNAME);
        String emailDomain = getEmailDomain(username);

        if (emailDomain == null) {
            // username does not map to any email domain, go to the next authentication step/sub-flow
            context.attempted();
            return;
        }

        RealmModel realm = context.getRealm();
        UserModel user = session.users().getUserByEmail(realm, username);

        if (user != null) {
            // user exists, check if enabled
            if (!user.isEnabled()) {
                context.failure(AuthenticationFlowError.INVALID_USER);
                return;
            }

            List<IdentityProviderModel> broker = resolveBroker(session, user);

            if (broker.isEmpty()) {
                // not a managed member, continue with the regular flow
                context.attempted();
            } else if (broker.size() == 1) {
                // user is a managed member and associated with a broker, redirect automatically
                redirect(context, broker.get(0).getAlias(), user.getEmail());
            }

            return;
        }

        OrganizationProvider provider = getOrganizationProvider();
        OrganizationModel organization = provider.getByDomainName(emailDomain);

        if (organization == null || !organization.isEnabled()) {
            // request does not map to any organization, go to the next step/sub-flow
            context.attempted();
            return;
        }

        List<IdentityProviderModel> brokers = organization.getIdentityProviders().toList();

        if (redirect(context, brokers, username, emailDomain)) {
            return;
        }

        if (!hasPublicBrokers(brokers)) {
            // the user does not exist, and there is no broker available for selection, redirect the user to the identity-first login page at the realm
            challenge(username, context);
            return;
        }

        // the user does not exist and is authenticating in the scope of the organization, show the identity-first login page and the
        // public organization brokers for selection
        LoginFormsProvider form = context.form()
                .setAttributeMapper(attributes -> {
                    attributes.computeIfPresent("social",
                            (key, bean) -> new OrganizationAwareIdentityProviderBean((IdentityProviderBean) bean, session, true)
                    );
                    attributes.computeIfPresent("auth",
                            (key, bean) -> new OrganizationAwareAuthenticationContextBean((AuthenticationContextBean) bean, false)
                    );
                    attributes.computeIfPresent("realm",
                            (key, bean) -> new OrganizationAwareRealmBean(realm)
                    );
                    return attributes;
                });
        form.addError(new FormMessage("Your email domain matches the " + organization.getName() + " organization but you don't have an account yet."));
        context.challenge(form
                .createLoginUsername());
    }

    private static boolean hasPublicBrokers(List<IdentityProviderModel> brokers) {
        return brokers.stream().anyMatch(p -> Boolean.parseBoolean(p.getConfig().getOrDefault(OrganizationModel.BROKER_PUBLIC, Boolean.FALSE.toString())));
    }

    private OrganizationProvider getOrganizationProvider() {
        return session.getProvider(OrganizationProvider.class);
    }

    private void challenge(AuthenticationFlowContext context) {
        challenge(null, context);
    }

    private void challenge(String username, AuthenticationFlowContext context){
        // the default challenge won't show any broker but just the identity-first login page and the option to try a different authentication mechanism
        LoginFormsProvider form = context.form()
                .setAttributeMapper(attributes -> {
                    attributes.computeIfPresent("social",
                            (key, bean) -> new OrganizationAwareIdentityProviderBean((IdentityProviderBean) bean, session, false, true)
                    );
                    attributes.computeIfPresent("auth",
                            (key, bean) -> new OrganizationAwareAuthenticationContextBean((AuthenticationContextBean) bean, false)
                    );
                    return attributes;
                });

        if (username != null) {
            form.addError(new FormMessage(Validation.FIELD_USERNAME, Messages.INVALID_USER));
        }

        context.challenge(form.createLoginUsername());
    }

    private String getEmailDomain(String email) {
        if (email == null) {
            return null;
        }

        int domainSeparator = email.indexOf('@');

        if (domainSeparator == -1) {
            return null;
        }

        return email.substring(domainSeparator + 1);
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return realm.isOrganizationsEnabled();
    }

    protected boolean redirect(AuthenticationFlowContext context, List<IdentityProviderModel> brokers, String username, String emailDomain) {
        for (IdentityProviderModel broker : brokers) {
            if (IdentityProviderRedirectMode.EMAIL_MATCH.isSet(broker)) {
                String idpDomain = broker.getConfig().get(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);

                if (emailDomain.equals(idpDomain)) {
                    // redirect the user using the broker that matches the email domain
                    redirect(context, broker.getAlias(), username);
                    return true;
                }
            }
        }

        return false;
    }
}
