/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.model.parameters;

import org.keycloak.cluster.infinispan.InfinispanClusterProviderFactory;
import org.keycloak.connections.infinispan.InfinispanConnectionProviderFactory;
import org.keycloak.connections.infinispan.InfinispanConnectionSpi;
import org.keycloak.testsuite.model.KeycloakModelParameters;
import org.keycloak.models.cache.CacheRealmProviderSpi;
import org.keycloak.models.cache.CacheUserProviderSpi;
import org.keycloak.models.cache.infinispan.InfinispanCacheRealmProviderFactory;
import org.keycloak.models.cache.infinispan.InfinispanUserCacheProviderFactory;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

/**
 *
 * @author hmlnarik
 */
public class Infinispan extends KeycloakModelParameters {

    static final Set<Class<? extends Spi>> ALLOWED_SPIS = ImmutableSet.<Class<? extends Spi>>builder()
      .add(CacheRealmProviderSpi.class)
      .add(CacheUserProviderSpi.class)
      .add(InfinispanConnectionSpi.class)

      .build();

    static final Set<Class<? extends ProviderFactory>> ALLOWED_FACTORIES = ImmutableSet.<Class<? extends ProviderFactory>>builder()
      .add(InfinispanCacheRealmProviderFactory.class)
      .add(InfinispanClusterProviderFactory.class)
      .add(InfinispanConnectionProviderFactory.class)
      .add(InfinispanUserCacheProviderFactory.class)
      .build();

    static {
        System.setProperty("keycloak.connectionsInfinispan.default.embedded", System.getProperty("keycloak.connectionsInfinispan.default.embedded", "true"));
    }

    public Infinispan() {
        super(ALLOWED_SPIS, ALLOWED_FACTORIES);
    }
}
