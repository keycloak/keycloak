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

package org.keycloak.adapters.springsecurity;

import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

/**
 * {@link FactoryBean} that creates an {@link AdapterDeploymentContext} given a {@link Resource} defining the Keycloak
 * client configuration or a {@link KeycloakConfigResolver} for multi-tenant environments.
 *
 * @author <a href="mailto:thomas.raehalme@aitiofinland.com">Thomas Raehalme</a>
 */
public class AdapterDeploymentContextFactoryBean
        implements FactoryBean<AdapterDeploymentContext>, InitializingBean {
    private static final Logger log =
        LoggerFactory.getLogger(AdapterDeploymentContextFactoryBean.class);
    private final Resource keycloakConfigFileResource;
    private final KeycloakConfigResolver keycloakConfigResolver;
    private AdapterDeploymentContext adapterDeploymentContext;

    public AdapterDeploymentContextFactoryBean(Resource keycloakConfigFileResource) {
        this.keycloakConfigFileResource = Objects.requireNonNull(keycloakConfigFileResource);
        this.keycloakConfigResolver = null;
    }

    public AdapterDeploymentContextFactoryBean(KeycloakConfigResolver keycloakConfigResolver) {
        this.keycloakConfigResolver = Objects.requireNonNull(keycloakConfigResolver);
        this.keycloakConfigFileResource = null;
    }

    @Override
    public Class<?> getObjectType() {
        return AdapterDeploymentContext.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (keycloakConfigResolver != null) {
            adapterDeploymentContext = new AdapterDeploymentContext(keycloakConfigResolver);
        }
        else {
            log.info("Loading Keycloak deployment from configuration file: {}", keycloakConfigFileResource);

            KeycloakDeployment deployment = loadKeycloakDeployment();
            adapterDeploymentContext = new AdapterDeploymentContext(deployment);
        }
    }

    private KeycloakDeployment loadKeycloakDeployment() throws IOException {
        if (!keycloakConfigFileResource.isReadable()) {
            throw new FileNotFoundException(String.format("Unable to locate Keycloak configuration file: %s",
                    keycloakConfigFileResource.getFilename()));
        }

        return KeycloakDeploymentBuilder.build(keycloakConfigFileResource.getInputStream());
    }

    @Override
    public AdapterDeploymentContext getObject() throws Exception {
        return adapterDeploymentContext;
    }
}
