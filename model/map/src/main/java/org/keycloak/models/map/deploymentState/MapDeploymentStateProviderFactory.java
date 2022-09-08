/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.deploymentState;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.Version;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.migration.MigrationModel;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.DeploymentStateProvider;
import org.keycloak.models.DeploymentStateProviderFactory;
import org.keycloak.models.DeploymentStateSpi;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public class MapDeploymentStateProviderFactory implements DeploymentStateProviderFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "map";

    private static final String RESOURCES_VERSION_SEED = "resourcesVersionSeed";

    @Override
    public DeploymentStateProvider create(KeycloakSession session) {
        return INSTANCE;
    }

    @Override
    public void init(Config.Scope config) {
        String seed = config.get(RESOURCES_VERSION_SEED);
        if (seed == null) {
            // hardcoded until https://github.com/keycloak/keycloak/issues/13828 has been implemented
            Logger.getLogger(DeploymentStateProviderFactory.class)
                    .warnf("Version seed for deployment state set with a random number. Caution: This can lead to unstable operations when serving resources from the cluster without a sticky loadbalancer or when restarting nodes. Set the 'storage-deployment-state-version-seed' option with a secret seed to ensure stable operations.", RESOURCES_VERSION_SEED, PROVIDER_ID, DeploymentStateSpi.NAME);
            //generate random string for this installation
            seed = SecretGenerator.getInstance().randomString(10);
        }
        try {
            Version.RESOURCES_VERSION = Base64Url.encode(MessageDigest.getInstance("SHA-256")
                    .digest((seed + Version.RESOURCES_VERSION).getBytes()))
                    .substring(0, 5);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.MAP_STORAGE);
    }

    private static final DeploymentStateProvider INSTANCE =  new DeploymentStateProvider() {

        private final MigrationModel INSTANCE = new MigrationModel() {
            @Override
                public String getStoredVersion() {
                    return null;
                }
                @Override
                public String getResourcesTag() {
                    throw new UnsupportedOperationException("Not supported.");
                }
                @Override
                public void setStoredVersion(String version) {
                    throw new UnsupportedOperationException("Not supported.");
                }
        };

        @Override
        public MigrationModel getMigrationModel() {
            return INSTANCE;
        }

        @Override
        public void close() {
        }

    };
}
