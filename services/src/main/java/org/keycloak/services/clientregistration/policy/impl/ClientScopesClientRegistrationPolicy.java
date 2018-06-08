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

package org.keycloak.services.clientregistration.policy.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.clientregistration.ClientRegistrationContext;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientScopesClientRegistrationPolicy implements ClientRegistrationPolicy {

    private static final Logger logger = Logger.getLogger(ClientScopesClientRegistrationPolicy.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final ComponentModel componentModel;

    public ClientScopesClientRegistrationPolicy(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
        this.realm = session.realms().getRealm(componentModel.getParentId());
    }

    @Override
    public void beforeRegister(ClientRegistrationContext context) throws ClientRegistrationPolicyException {
        List<String> requestedDefaultScopeNames = context.getClient().getDefaultClientScopes();
        List<String> requestedOptionalScopeNames = context.getClient().getOptionalClientScopes();

        List<String> allowedDefaultScopeNames = getAllowedScopeNames(realm, true);
        List<String> allowedOptionalScopeNames = getAllowedScopeNames(realm, false);

        checkClientScopesAllowed(requestedDefaultScopeNames, allowedDefaultScopeNames);
        checkClientScopesAllowed(requestedOptionalScopeNames, allowedOptionalScopeNames);
    }

    @Override
    public void afterRegister(ClientRegistrationContext context, ClientModel clientModel) {

    }

    @Override
    public void beforeUpdate(ClientRegistrationContext context, ClientModel clientModel) throws ClientRegistrationPolicyException {
        List<String> requestedDefaultScopeNames = context.getClient().getDefaultClientScopes();
        List<String> requestedOptionalScopeNames = context.getClient().getOptionalClientScopes();

        // Allow scopes, which were already presented before
        if (requestedDefaultScopeNames != null) {
            requestedDefaultScopeNames.removeAll(clientModel.getClientScopes(true, false).keySet());
        }
        if (requestedOptionalScopeNames != null) {
            requestedOptionalScopeNames.removeAll(clientModel.getClientScopes(false, false).keySet());
        }

        List<String> allowedDefaultScopeNames = getAllowedScopeNames(realm, true);
        List<String> allowedOptionalScopeNames = getAllowedScopeNames(realm, false);

        checkClientScopesAllowed(requestedDefaultScopeNames, allowedDefaultScopeNames);
        checkClientScopesAllowed(requestedOptionalScopeNames, allowedOptionalScopeNames);
    }

    @Override
    public void afterUpdate(ClientRegistrationContext context, ClientModel clientModel) {

    }

    @Override
    public void beforeView(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException {

    }

    @Override
    public void beforeDelete(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException {

    }

    private void checkClientScopesAllowed(List<String> requestedScopes, List<String> allowedScopes) throws ClientRegistrationPolicyException {
        if (requestedScopes != null) {
            for (String requested : requestedScopes) {
                if (!allowedScopes.contains(requested)) {
                    logger.warnf("Requested scope '%s' not trusted in the list: %s", requested, allowedScopes.toString());
                    throw new ClientRegistrationPolicyException("Not permitted to use specified clientScope");
                }
            }
        }
    }

    private List<String> getAllowedScopeNames(RealmModel realm, boolean defaultScopes) {
        List<String> allAllowed = new LinkedList<>();

        // Add client scopes allowed by config
        List<String> allowedScopesConfig = componentModel.getConfig().getList(ClientScopesClientRegistrationPolicyFactory.ALLOWED_CLIENT_SCOPES);
        if (allowedScopesConfig != null) {
            allAllowed.addAll(allowedScopesConfig);
        }

        // If allowDefaultScopes, then realm default scopes are allowed as default scopes (+ optional scopes are allowed as optional scopes)
        boolean allowDefaultScopes = componentModel.get(ClientScopesClientRegistrationPolicyFactory.ALLOW_DEFAULT_SCOPES, true);
        if (allowDefaultScopes) {
            List<String> scopeNames = realm.getDefaultClientScopes(defaultScopes).stream().map((ClientScopeModel clientScope) -> {

                return clientScope.getName();

            }).collect(Collectors.toList());

            allAllowed.addAll(scopeNames);
        }

        return allAllowed;
    }
}
