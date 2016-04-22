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

package org.keycloak.testsuite.federation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.mappers.FederationConfigValidationException;
import org.keycloak.mappers.UserFederationMapper;
import org.keycloak.mappers.UserFederationMapperFactory;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserFederationSyncResult;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.UserFederationMapperSyncConfigRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DummyUserFederationMapper implements UserFederationMapperFactory, UserFederationMapper {

    public static final String PROVIDER_NAME = "dummy-mapper";

    @Override
    public String getFederationProviderType() {
        return DummyUserFederationProviderFactory.PROVIDER_NAME;
    }

    @Override
    public String getDisplayCategory() {
        return "Dummy";
    }

    @Override
    public String getDisplayType() {
        return "Dummy";
    }

    @Override
    public UserFederationMapperSyncConfigRepresentation getSyncConfig() {
        return new UserFederationMapperSyncConfigRepresentation(true, "dummyFedToKeycloak", true, "dummyKeycloakToFed");
    }

    @Override
    public void validateConfig(RealmModel realm, UserFederationProviderModel fedProviderModel, UserFederationMapperModel mapperModel) throws FederationConfigValidationException {

    }

    @Override
    public Map<String, String> getDefaultConfig(UserFederationProviderModel providerModel) {
        return Collections.emptyMap();
    }

    @Override
    public String getHelpText() {
        return "Dummy";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    public UserFederationMapper create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public UserFederationSyncResult syncDataFromFederationProviderToKeycloak(final UserFederationMapperModel mapperModel, UserFederationProvider federationProvider, KeycloakSession session, RealmModel realm) {
        return new UserFederationSyncResult() {

            @Override
            public String getStatus() {
                return "dummyFedToKeycloakSuccess mapper=" + mapperModel.getName();
            }

        };
    }

    @Override
    public UserFederationSyncResult syncDataFromKeycloakToFederationProvider(final UserFederationMapperModel mapperModel, UserFederationProvider federationProvider, KeycloakSession session, RealmModel realm) {
        return new UserFederationSyncResult() {

            @Override
            public String getStatus() {
                return "dummyKeycloakToFedSuccess mapper=" + mapperModel.getName();
            }

        };
    }

    @Override
    public List<UserModel> getGroupMembers(UserFederationMapperModel mapperModel, UserFederationProvider federationProvider, RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return Collections.emptyList();
    }
}
