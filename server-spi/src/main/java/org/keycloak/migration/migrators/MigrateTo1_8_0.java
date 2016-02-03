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

import java.util.List;
import java.util.Map;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MigrateTo1_8_0 {

    public static final ModelVersion VERSION = new ModelVersion("1.8.0");

    public void migrate(KeycloakSession session) {
        List<RealmModel> realms = session.realms().getRealms();
        for (RealmModel realm : realms) {

            List<UserFederationProviderModel> federationProviders = realm.getUserFederationProviders();
            for (UserFederationProviderModel fedProvider : federationProviders) {

                if (fedProvider.getProviderName().equals(LDAPConstants.LDAP_PROVIDER)) {
                    Map<String, String> config = fedProvider.getConfig();

                    if (isActiveDirectory(config)) {

                        // Create mapper for MSAD account controls
                        if (realm.getUserFederationMapperByName(fedProvider.getId(), "MSAD account controls") == null) {
                            UserFederationMapperModel mapperModel = KeycloakModelUtils.createUserFederationMapperModel("MSAD account controls", fedProvider.getId(), LDAPConstants.MSAD_USER_ACCOUNT_CONTROL_MAPPER);
                            realm.addUserFederationMapper(mapperModel);
                        }
                    }
                }
            }

        }
    }

    private boolean isActiveDirectory(Map<String, String> ldapConfig) {
        String vendor = ldapConfig.get(LDAPConstants.VENDOR);
        return vendor != null && vendor.equals(LDAPConstants.VENDOR_ACTIVE_DIRECTORY);
    }
}
