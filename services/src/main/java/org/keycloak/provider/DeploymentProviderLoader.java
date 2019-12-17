/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.provider;

import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
final class DeploymentProviderLoader implements ProviderLoader {

    private final KeycloakDeploymentInfo info;

    DeploymentProviderLoader(KeycloakDeploymentInfo info) {
        this.info = info;
    }

    @Override
    public List<Spi> loadSpis() {
        return Collections.emptyList();
    }

    @Override
    public List<ProviderFactory> load(Spi spi) {
        return info.getProviders().getOrDefault(spi.getClass(), Collections.emptyList());
    }
}
