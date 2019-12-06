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

package org.keycloak.migration.migrators;

import org.keycloak.component.ComponentModel;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.UserStorageProviderModel;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MigrateTo1_8_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("1.8.0");

    public ModelVersion getVersion() {
        return VERSION;
    }


    public void migrate(KeycloakSession session) {
        List<RealmModel> realms = session.realms().getRealms();
        for (RealmModel realm : realms) {

            migrateRealm(realm);

        }
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        migrateRealm(realm);
    }

    protected void migrateRealm(RealmModel realm) {
        List<UserStorageProviderModel> federationProviders = realm.getUserStorageProviders();
        for (UserStorageProviderModel fedProvider : federationProviders) {

            if (fedProvider.getProviderId().equals(LDAPConstants.LDAP_PROVIDER)) {

                if (isActiveDirectory(fedProvider)) {
                    // Create mapper for MSAD account controls
                    if (getMapperByName(realm, fedProvider, "MSAD account controls") == null) {
                        ComponentModel mapperModel = KeycloakModelUtils.createComponentModel("MSAD account controls", fedProvider.getId(), LDAPConstants.MSAD_USER_ACCOUNT_CONTROL_MAPPER, "org.keycloak.storage.ldap.mappers.LDAPStorageMapper");
                        realm.addComponentModel(mapperModel);
                    }
                }
            }
        }
    }

    public static ComponentModel getMapperByName(RealmModel realm, ComponentModel providerModel, String name) {
        List<ComponentModel> components = realm.getComponents(providerModel.getId(), "org.keycloak.storage.ldap.mappers.LDAPStorageMapper");
        for (ComponentModel component : components) {
            if (component.getName().equals(name)) {
                return component;
            }
        }
        return null;
    }


    private boolean isActiveDirectory(UserStorageProviderModel provider) {
        String vendor = provider.getConfig().getFirst(LDAPConstants.VENDOR);
        return vendor != null && vendor.equals(LDAPConstants.VENDOR_ACTIVE_DIRECTORY);
    }
}
