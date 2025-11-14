/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.migration.migrators;

import java.util.List;
import java.util.Objects;

import org.keycloak.OAuth2Constants;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.DefaultClientScopes;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MigrateTo4_0_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("4.0.0");

    private static final Logger LOG = Logger.getLogger(MigrateTo4_0_0.class);

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void migrate(KeycloakSession session) {
        session.realms().getRealmsStream().forEach(realm -> migrateRealm(session, realm, false));
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        migrateRealm(session, realm, true);
    }


    protected void migrateRealm(KeycloakSession session, RealmModel realm, boolean json) {
        // Upgrade names of clientScopes to not contain space
        realm.getClientScopesStream()
                .filter(clientScope -> clientScope.getName().contains(" "))
                .forEach(clientScope -> {
                    LOG.debugf("Replacing spaces with underscores in the name of client scope '%s' of realm '%s'",
                            clientScope.getName(), realm.getName());
                    String replacedName = clientScope.getName().replaceAll(" ", "_");
                    clientScope.setName(replacedName);
                });

        if (!json) {
            // Add default client scopes. But don't add them to existing clients. For JSON, they were already added
            LOG.debugf("Adding defaultClientScopes for realm '%s'", realm.getName());
            DefaultClientScopes.createDefaultClientScopes(session, realm, false);
        }

        // Upgrade configuration of "allowed-client-templates" client registration policy
        realm.getComponentsStream(realm.getId(), "org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy")
                .filter(component -> Objects.equals(component.getProviderId(), "allowed-client-templates"))
                .forEach(component -> {
                    List<String> configVal = component.getConfig().remove("allowed-client-templates");
                    if (configVal != null) {
                        component.getConfig().put("allowed-client-scopes", configVal);
                    }
                    component.put("allow-default-scopes", true);

                    realm.updateComponent(component);
                });


        // If client has scope for offline_access role (either directly or through fullScopeAllowed), then add offline_access client
        // scope as optional scope to the client. If it's indirectly (no fullScopeAllowed), then remove role from the scoped roles
        RoleModel offlineAccessRole = realm.getRole(OAuth2Constants.OFFLINE_ACCESS);
        ClientScopeModel offlineAccessScope;
        if (offlineAccessRole == null) {
            LOG.infof("Role 'offline_access' not available in realm '%s'. Skip migration of offline_access client scope.", realm.getName());
        } else {
            offlineAccessScope = KeycloakModelUtils.getClientScopeByName(realm, OAuth2Constants.OFFLINE_ACCESS);
            if (offlineAccessScope == null) {
                LOG.infof("Client scope 'offline_access' not available in realm '%s'. Skip migration of offline_access client scope.", realm.getName());
            } else {
                realm.getClientsStream()
                        .filter(MigrationUtils::isOIDCNonBearerOnlyClient)
                        .filter(c -> c.hasScope(offlineAccessRole))
                        .filter(c -> !c.getClientScopes(false).containsKey(OAuth2Constants.OFFLINE_ACCESS))
                        .peek(c -> {
                            LOG.debugf("Adding client scope 'offline_access' as optional scope to client '%s' in realm '%s'.", c.getClientId(), realm.getName());
                            c.addClientScope(offlineAccessScope, false);
                        })
                        .filter(c -> !c.isFullScopeAllowed())
                        .forEach(c -> {
                            LOG.debugf("Removing role scope mapping for role 'offline_access' from client '%s' in realm '%s'.", c.getClientId(), realm.getName());
                            c.deleteScopeMapping(offlineAccessRole);
                        });
            }
        }


        // Clients with consentRequired, which don't have any client scopes will be added itself to require consent, so that consent screen is shown when users authenticate
        realm.getClientsStream()
                .filter(ClientModel::isConsentRequired)
                .filter(c -> c.getClientScopes(true).isEmpty())
                .forEach(c -> {
                    LOG.debugf("Adding client '%s' of realm '%s' to display itself on consent screen", c.getClientId(), realm.getName());
                    c.setDisplayOnConsentScreen(true);
                    String consentText = c.getName() == null ? c.getClientId() : c.getName();
                    c.setConsentScreenText(consentText);
                });
    }
}
