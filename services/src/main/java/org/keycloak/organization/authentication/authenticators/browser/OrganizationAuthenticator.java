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
import java.util.Objects;

import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticator;
import org.keycloak.forms.login.freemarker.model.IdentityProviderBean;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.forms.login.freemarker.model.OrganizationAwareIdentityProviderBean;

public class OrganizationAuthenticator extends IdentityProviderAuthenticator {

    private final KeycloakSession session;

    public OrganizationAuthenticator(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        OrganizationProvider provider = getOrganizationProvider();

        if (!provider.isEnabled()) {
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

        OrganizationProvider provider = getOrganizationProvider();
        OrganizationModel organization = null;
        RealmModel realm = context.getRealm();
        UserModel user = session.users().getUserByEmail(realm, username);

        if (user != null) {
            // user exists, check if enabled
            if (!user.isEnabled()) {
                context.failure(AuthenticationFlowError.INVALID_USER);
                return;
            }

            organization = provider.getByMember(user);

            if (organization != null) {
                if (provider.isManagedMember(organization, user)) {
                    // user is a managed member, try to resolve the origin broker and redirect automatically
                    List<IdentityProviderModel> organizationBrokers = organization.getIdentityProviders().toList();
                    List<IdentityProviderModel> originBrokers = session.users().getFederatedIdentitiesStream(realm, user)
                            .map(f -> {
                                IdentityProviderModel broker = realm.getIdentityProviderByAlias(f.getIdentityProvider());

                                if (!organizationBrokers.contains(broker)) {
                                    return null;
                                }

                                FederatedIdentityModel identity = session.users().getFederatedIdentity(realm, user, broker.getAlias());

                                if (identity != null) {
                                    return broker;
                                }

                                return null;
                            }).filter(Objects::nonNull)
                            .toList();


                    if (originBrokers.size() == 1) {
                        redirect(context, originBrokers.get(0).getAlias());
                        return;
                    }
                } else {
                    context.attempted();
                    return;
                }
            }
        }

        if (organization == null) {
            organization = provider.getByDomainName(emailDomain);
        }

        if (organization == null) {
            // request does not map to any organization, go to the next step/sub-flow
            context.attempted();
            return;
        }

        List<IdentityProviderModel> domainBrokers = organization.getIdentityProviders().toList();

        if (domainBrokers.isEmpty()) {
            // no organization brokers to automatically redirect the user, go to the next step/sub-flow
            context.attempted();
            return;
        }

        if (domainBrokers.size() == 1) {
            // there is a single broker, redirect the user to authenticate
            redirect(context, domainBrokers.get(0).getAlias(), username);
            return;
        }

        for (IdentityProviderModel broker : domainBrokers) {
            String idpDomain = broker.getConfig().get(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);

            if (emailDomain.equals(idpDomain)) {
                // redirect the user using the broker that matches the email domain
                redirect(context, broker.getAlias(), username);
                return;
            }
        }

        // the user is authenticating in the scope of the organization, show the identity-first login page and the
        // public organization brokers for selection
        context.challenge(context.form()
                .setAttributeMapper(attributes -> {
                    attributes.computeIfPresent("social",
                            (key, bean) -> new OrganizationAwareIdentityProviderBean((IdentityProviderBean) bean, session, true)
                    );
                    return attributes;
                })
                .createLoginUsername());
    }

    private OrganizationProvider getOrganizationProvider() {
        return session.getProvider(OrganizationProvider.class);
    }

    private void challenge(AuthenticationFlowContext context){
        // the default challenge won't show any broker but just the identity-first login page and the option to try a different authentication mechanism
        context.challenge(context.form()
                .setAttributeMapper(attributes -> {
                    // removes identity provider related attributes from forms
                    attributes.remove("social");
                    return attributes;
                })
                .createLoginUsername());
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
}
