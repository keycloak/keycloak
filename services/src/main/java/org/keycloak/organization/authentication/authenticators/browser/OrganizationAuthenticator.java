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

import java.util.function.BiFunction;

import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticator;
import org.keycloak.forms.login.freemarker.model.IdentityProviderBean;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;

public class OrganizationAuthenticator extends IdentityProviderAuthenticator {

    private final KeycloakSession session;

    public OrganizationAuthenticator(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        OrganizationProvider provider = getOrganizationProvider();

        if (!provider.isEnabled()) {
            attempted(context);
            return;
        }

        challenge(context);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        HttpRequest request = context.getHttpRequest();
        MultivaluedMap<String, String> parameters = request.getDecodedFormParameters();
        String username = parameters.getFirst(UserModel.USERNAME);

        if (username == null) {
            challenge(context);
            return;
        }

        String domain = getEmailDomain(username);

        if (domain == null) {
            attempted(context);
            return;
        }

        OrganizationProvider provider = getOrganizationProvider();
        OrganizationModel organization = provider.getByDomainName(domain);

        if (organization == null) {
            attempted(context);
            return;
        }

        IdentityProviderModel identityProvider = organization.getIdentityProvider();

        if (identityProvider == null) {
            attempted(context);
            return;
        }

        redirect(context, identityProvider.getAlias(), username);
    }

    private void attempted(AuthenticationFlowContext context) {
        context.form()
                .setAttributeMapper(attributes -> {
                    attributes.computeIfPresent("social", createOrganizationAwareSocialBean());
                    return attributes;
                });
        context.attempted();
    }

    private BiFunction<String, Object, IdentityProviderBean> createOrganizationAwareSocialBean() {
        return (key, bean) -> new OrganizationAwareIdentityProviderBean((IdentityProviderBean) bean, session);
    }

    private OrganizationProvider getOrganizationProvider() {
        return session.getProvider(OrganizationProvider.class);
    }

    private void challenge (AuthenticationFlowContext context){
        context.challenge(context.form()
                .setAttributeMapper(attributes -> {
                    // removes identity provider related attributes from forms
                    attributes.remove("social");
                    return attributes;
                })
                .createLoginUsername());
    }

    private String getEmailDomain(String email) {
        int domainSeparator = email.indexOf('@');

        if (domainSeparator == -1) {
            return null;
        }

        return email.substring(domainSeparator + 1);
    }
}
