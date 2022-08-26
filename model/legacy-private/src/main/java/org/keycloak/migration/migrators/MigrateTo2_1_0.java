/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.migration.migrators;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class MigrateTo2_1_0 implements Migration {
    public static final ModelVersion VERSION = new ModelVersion("2.1.0");

    public ModelVersion getVersion() {
        return VERSION;
    }

    public void migrate(KeycloakSession session) {
        session.realms().getRealmsStream().forEach(realm -> {
            migrateDefaultRequiredAction(realm);
            migrateRolePolicies(realm, session);
        });
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        migrateDefaultRequiredAction(realm);
        migrateRolePolicies(realm, session);

    }

    // KEYCLOAK-3244: Required Action "Configure Totp" should be "Configure OTP"
    private void migrateDefaultRequiredAction(RealmModel realm) {
        RequiredActionProviderModel otpAction = realm.getRequiredActionProviderByAlias(UserModel.RequiredAction.CONFIGURE_TOTP.name());

        MigrationUtils.updateOTPRequiredAction(otpAction);

        realm.updateRequiredActionProvider(otpAction);
    }

    // KEYCLOAK-3338: Changes to how role policy config is stored"
    private void migrateRolePolicies(RealmModel realm, KeycloakSession session) {
        AuthorizationProvider authorizationProvider = session.getProvider(AuthorizationProvider.class);
        StoreFactory storeFactory = authorizationProvider.getStoreFactory();
        PolicyStore policyStore = storeFactory.getPolicyStore();
        realm.getClientsStream().forEach(clientModel -> {
            ResourceServer resourceServer = storeFactory.getResourceServerStore().findByClient(clientModel);

            if (resourceServer != null) {
                policyStore.findByType(resourceServer, "role").forEach(policy -> {
                    Map<String, String> config = new HashMap(policy.getConfig());
                    String roles = config.get("roles");
                    List roleConfig;

                    try {
                        roleConfig = JsonSerialization.readValue(roles, List.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Malformed configuration for role policy [" + policy.getName() + "].", e);
                    }

                    if (!roleConfig.isEmpty() && roleConfig.get(0) instanceof String) {
                        try {
                            config.put("roles", JsonSerialization.writeValueAsString(roleConfig.stream().map(new Function<String, Map>() {
                                @Override
                                public Map apply(String roleId) {
                                    Map updated = new HashMap();

                                    updated.put("id", roleId);

                                    return updated;
                                }
                            }).collect(Collectors.toList())));
                            policy.setConfig(config);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to migrate role policy [" + policy.getName() + "].", e);
                        }
                    }
                });
            }
        });
    }
}
