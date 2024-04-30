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

package org.keycloak.organization.authentication.authenticators.broker;

import java.util.List;
import java.util.stream.Stream;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;

public class IdpAddOrganizationMemberAuthenticator extends AbstractIdpAuthenticator {

    @Override
    protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
    }

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        OrganizationProvider provider = context.getSession().getProvider(OrganizationProvider.class);
        UserModel user = context.getUser();
        OrganizationModel organization = (OrganizationModel) context.getSession().getAttribute(OrganizationModel.class.getName());

        if (organization == null) {
            context.attempted();
            return;
        }

        Stream<IdentityProviderModel> expectedBrokers = organization.getIdentityProviders();
        IdentityProviderModel broker = brokerContext.getIdpConfig();

        if (expectedBrokers.noneMatch(broker::equals)) {
            context.failure(AuthenticationFlowError.ACCESS_DENIED);
            return;
        }

        provider.addMember(organization, user);
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        OrganizationProvider provider = session.getProvider(OrganizationProvider.class);

        if (!provider.isEnabled()) {
            return false;
        }

        OrganizationModel organization = (OrganizationModel) session.getAttribute(OrganizationModel.class.getName());

        if (organization == null) {
            return false;
        }

        return provider.getIdentityProviders(organization).findAny().isPresent();
    }
}