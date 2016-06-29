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

package org.keycloak.mappers;

import java.util.Map;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.UserFederationMapperSyncConfigRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface UserFederationMapperFactory extends ProviderFactory<UserFederationMapper>, ConfiguredProvider {

    /**
     * Refers to providerName (type) of the federated provider, which this mapper can be used for. For example "ldap" or "kerberos"
     *
     * @return providerName
     */
    String getFederationProviderType();

    String getDisplayCategory();
    String getDisplayType();

    /**
     * Specifies if mapper supports sync data from federated storage to keycloak and viceversa.
     * Also specifies messages to be displayed in admin console UI (For example "Sync roles from LDAP" etc)
     *
     * @return syncConfig representation
     */
    UserFederationMapperSyncConfigRepresentation getSyncConfig();

    /**
     * Called when instance of mapperModel is created for this factory through admin endpoint
     *
     * @param realm
     * @param fedProviderModel
     * @param mapperModel
     * @throws FederationConfigValidationException if configuration provided in mapperModel is not valid
     */
    void validateConfig(RealmModel realm, UserFederationProviderModel fedProviderModel, UserFederationMapperModel mapperModel) throws FederationConfigValidationException;

    /**
     * Used to detect what are default values for ProviderConfigProperties specified during mapper creation
     *
     * @return
     */
    Map<String, String> getDefaultConfig(UserFederationProviderModel providerModel);

}
