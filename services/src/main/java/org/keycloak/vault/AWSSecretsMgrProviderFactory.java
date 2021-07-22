/**
 * Copyright 2021 OutSystems and/or its affiliates and other
 * contributors as indicated by the @author tags.
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
package org.keycloak.vault;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.lang.invoke.MethodHandles;

public class AWSSecretsMgrProviderFactory extends AbstractVaultProviderFactory {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private static final String PROVIDER_ID = "awssecretsmgr";

    static final String AWS_DEFAULT_REGION = "defaultRegion";
    static final String ENV_AWS_REGION = "KEYCLOAK_AWSSM_REGION";

    private String region;

    @Override
    public VaultProvider create(KeycloakSession session) {
        if (region == null) {
            logger.debug("Can not create an AWS SecretsManager vault provider since it's not initialized correctly");
            return null;
        }

        return new AWSSecretsMgrProvider(getRealmName(session), super.keyResolvers, region);
    }

    @Override
    public void init(Config.Scope config) {
        super.init(config);

        this.region = System.getenv(ENV_AWS_REGION);
        if (this.region == null) {
            this.region = config.get(AWS_DEFAULT_REGION);
        }
        if (this.region == null) {
            logger.debug("AWSSecretsMgrProviderFactory not properly configured - missing region");
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
