/*
 * Copyright 2026 Andrzej Świętek
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

package org.keycloak.organization.authentication.authenticators.conditional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.authentication.authenticators.conditional.OrganizationAttributeCondition.Config;
import org.keycloak.organization.authentication.authenticators.conditional.OrganizationAttributeCondition.Source;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.jboss.logging.Logger;

import static org.keycloak.organization.utils.Organizations.getProvider;
import static org.keycloak.organization.utils.Organizations.isEnabled;

public class ConditionalOrganizationAttributeAuthenticator implements ConditionalAuthenticator {

    static final ConditionalOrganizationAttributeAuthenticator SINGLETON = new ConditionalOrganizationAttributeAuthenticator();

    private static final Logger logger = Logger.getLogger(ConditionalOrganizationAttributeAuthenticator.class);

    private static final String NOT_CONFIGURED_MESSAGE = "The organization attribute condition of execution '%s' lacks an attribute name or an expected value, or has an unknown organization option, so its sub-flow is skipped. Complete the condition settings.";

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        return Config.from(configOf(context))
                .map(config -> isEnabled(context.getSession()) && userMatches(context, config))
                .orElseGet(() -> notConfigured(context));
    }

    private static Map<String, String> configOf(AuthenticationFlowContext context) {
        return Optional.ofNullable(context.getAuthenticatorConfig())
                .map(AuthenticatorConfigModel::getConfig)
                .orElse(Map.of());
    }

    private static boolean userMatches(AuthenticationFlowContext context, Config config) {
        return Optional.ofNullable(context.getUser())
                .map(user -> OrganizationAttributeCondition.matches(config, organizationAttributes(context, user, config.source())))
                .orElse(false);
    }

    private static boolean notConfigured(AuthenticationFlowContext context) {
        logger.warnf(NOT_CONFIGURED_MESSAGE, context.getExecution().getId());
        return false;
    }

    private static Stream<Map<String, List<String>>> organizationAttributes(AuthenticationFlowContext context, UserModel user, Source source) {
        return organizations(getProvider(context.getSession()), user, context.getAuthenticationSession(), source)
                .filter(OrganizationModel::isEnabled)
                .map(OrganizationModel::getAttributes);
    }

    private static Stream<OrganizationModel> organizations(OrganizationProvider provider, UserModel user,
            AuthenticationSessionModel authSession, Source source) {
        return switch (source) {
            case ANY_MEMBERSHIP -> provider.getByMember(user);
            case SELECTED -> selectedOrganization(provider, user, authSession).stream();
        };
    }

    // the browser flow also records an organization matching the user's email domain, member or not
    private static Optional<OrganizationModel> selectedOrganization(OrganizationProvider provider, UserModel user,
            AuthenticationSessionModel authSession) {
        return Optional.ofNullable(authSession.getAuthNote(OrganizationModel.ORGANIZATION_ATTRIBUTE))
                .map(provider::getById)
                .filter(organization -> provider.isMember(organization, user));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // no-op
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }
}
