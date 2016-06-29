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

import java.util.List;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationSyncResult;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface UserFederationMapper extends Provider {

    /**
     * Sync data from federated storage to Keycloak. It's useful just if mapper needs some data preloaded from federated storage (For example
     * load roles from federated provider and sync them to Keycloak database)
     *
     * Applicable just if sync is supported (see UserFederationMapperFactory.getSyncConfig() )
     *
     * @see UserFederationMapperFactory#getSyncConfig()
     * @param mapperModel
     * @param federationProvider
     * @param session
     * @param realm
     */
    UserFederationSyncResult syncDataFromFederationProviderToKeycloak(UserFederationMapperModel mapperModel, UserFederationProvider federationProvider, KeycloakSession session, RealmModel realm);

    /**
     * Sync data from Keycloak back to federated storage
     *
     * @see UserFederationMapperFactory#getSyncConfig()
     * @param mapperModel
     * @param federationProvider
     * @param session
     * @param realm
     */
    UserFederationSyncResult syncDataFromKeycloakToFederationProvider(UserFederationMapperModel mapperModel, UserFederationProvider federationProvider, KeycloakSession session, RealmModel realm);

    /**
     * Return empty list if doesn't support storing of groups
     */
    List<UserModel> getGroupMembers(UserFederationMapperModel mapperModel, UserFederationProvider federationProvider, RealmModel realm, GroupModel group, int firstResult, int maxResults);
}
