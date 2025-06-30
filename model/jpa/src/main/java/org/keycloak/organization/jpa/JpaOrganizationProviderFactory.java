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

package org.keycloak.organization.jpa;

import org.keycloak.Config;
import org.keycloak.Config.Scope;
import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupModel.GroupEvent;
import org.keycloak.models.ModelValidationException;
import org.keycloak.organization.authentication.authenticators.broker.IdpOrganizationAuthenticatorFactory;
import org.keycloak.organization.authentication.authenticators.browser.OrganizationAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmModel.RealmPostCreateEvent;
import org.keycloak.models.RealmModel.RealmRemovedEvent;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.OrganizationProviderFactory;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.provider.ProviderEvent;

public class JpaOrganizationProviderFactory implements OrganizationProviderFactory {

    @Override
    public OrganizationProvider create(KeycloakSession session) {
        return new JpaOrganizationProvider(session);
    }

    @Override
    public void init(Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(this::handleEvents);
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "jpa";
    }

    private void handleEvents(ProviderEvent event) {
        if (event instanceof RealmPostCreateEvent) {
            RealmModel realm = ((RealmPostCreateEvent) event).getCreatedRealm();
            configureAuthenticationFlows(realm);
        }
        if (event instanceof RealmRemovedEvent) {
            KeycloakSession session = ((RealmRemovedEvent) event).getKeycloakSession();
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
            provider.removeAll();
        }
        if (event instanceof GroupEvent) {
            GroupEvent groupEvent = (GroupEvent) event;
            KeycloakSession session = groupEvent.getKeycloakSession();
            GroupModel group = groupEvent.getGroup();
            if (!Organizations.canManageOrganizationGroup(session, group)) {
                throw new ModelValidationException("Can not update organization group");
            }
        }
    }

    private void configureAuthenticationFlows(RealmModel realm) {
        addOrganizationFirstBrokerFlowStep(realm);
        addOrganizationBrowserFlowStep(realm);
    }

    private void addOrganizationFirstBrokerFlowStep(RealmModel realm) {

        AuthenticationFlowModel firstBrokerLoginFlow = realm.getFirstBrokerLoginFlow();
        if (firstBrokerLoginFlow == null) {
            return;
        }

        if (realm.getAuthenticationExecutionsStream(firstBrokerLoginFlow.getId())
                .map(AuthenticationExecutionModel::getAuthenticator)
                .anyMatch(IdpOrganizationAuthenticatorFactory.ID::equals)) {
            return;
        }

        if (!Config.getAdminRealm().equals(realm.getName())) {
            // do not add the org flows to the master realm for now.
            AuthenticationFlowModel conditionalOrg = new AuthenticationFlowModel();
            conditionalOrg.setTopLevel(false);
            conditionalOrg.setBuiltIn(true);
            conditionalOrg.setAlias("First Broker Login - Conditional Organization");
            conditionalOrg.setDescription("Flow to determine if the authenticator that adds organization members is to be used");
            conditionalOrg.setProviderId("basic-flow");
            conditionalOrg = realm.addAuthenticationFlow(conditionalOrg);
            AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
            execution.setParentFlow(firstBrokerLoginFlow.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL);
            execution.setFlowId(conditionalOrg.getId());
            execution.setPriority(50);
            execution.setAuthenticatorFlow(true);
            realm.addAuthenticatorExecution(execution);

            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(conditionalOrg.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator("conditional-user-configured");
            execution.setPriority(10);
            execution.setAuthenticatorFlow(false);
            realm.addAuthenticatorExecution(execution);

            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(conditionalOrg.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(IdpOrganizationAuthenticatorFactory.ID);
            execution.setPriority(20);
            execution.setAuthenticatorFlow(false);
            realm.addAuthenticatorExecution(execution);
        }
    }

    public void addOrganizationBrowserFlowStep(RealmModel realm) {

        AuthenticationFlowModel browserFlow = realm.getBrowserFlow();
        if (browserFlow == null) {
            return;
        }

        if (realm.getAuthenticationExecutionsStream(browserFlow.getId())
                .map(AuthenticationExecutionModel::getAuthenticator)
                .anyMatch(OrganizationAuthenticatorFactory.ID::equals)) {
            return;
        }

        if (!Config.getAdminRealm().equals(realm.getName())) {
            // do not add the org flows to the master realm for now.
            AuthenticationFlowModel organizations = new AuthenticationFlowModel();
            organizations.setTopLevel(false);
            organizations.setBuiltIn(true);
            organizations.setAlias("Organization");
            organizations.setProviderId("basic-flow");
            organizations = realm.addAuthenticationFlow(organizations);
            AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
            execution.setParentFlow(browserFlow.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            execution.setFlowId(organizations.getId());
            execution.setPriority(26);
            execution.setAuthenticatorFlow(true);
            realm.addAuthenticatorExecution(execution);

            AuthenticationFlowModel conditionalOrg = new AuthenticationFlowModel();
            conditionalOrg.setTopLevel(false);
            conditionalOrg.setBuiltIn(true);
            conditionalOrg.setAlias("Browser - Conditional Organization");
            conditionalOrg.setDescription("Flow to determine if the organization identity-first login is to be used");
            conditionalOrg.setProviderId("basic-flow");
            conditionalOrg = realm.addAuthenticationFlow(conditionalOrg);
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(organizations.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL);
            execution.setFlowId(conditionalOrg.getId());
            execution.setPriority(10);
            execution.setAuthenticatorFlow(true);
            realm.addAuthenticatorExecution(execution);

            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(conditionalOrg.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator("conditional-user-configured");
            execution.setPriority(10);
            execution.setAuthenticatorFlow(false);
            realm.addAuthenticatorExecution(execution);

            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(conditionalOrg.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            execution.setAuthenticator(OrganizationAuthenticatorFactory.ID);
            execution.setPriority(20);
            execution.setAuthenticatorFlow(false);
            realm.addAuthenticatorExecution(execution);
        }
    }
}
